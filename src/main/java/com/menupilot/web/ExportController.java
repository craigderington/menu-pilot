package com.menupilot.web;

import com.menupilot.domain.*;
import com.menupilot.repo.*;
import com.menupilot.service.PrepListService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/export")
public class ExportController {

    private com.itextpdf.text.BaseColor parseHex(String hex) {
        if (hex == null) return new com.itextpdf.text.BaseColor(240,240,240);
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        try {
            int r = Integer.valueOf(h.substring(0,2),16);
            int g = Integer.valueOf(h.substring(2,4),16);
            int b = Integer.valueOf(h.substring(4,6),16);
            return new com.itextpdf.text.BaseColor(r,g,b);
        } catch(Exception e) {
            return new com.itextpdf.text.BaseColor(240,240,240);
        }
    }
\n\n    static record PreviewRow(String name,String station,Integer capQty,Integer priceCents,String description,String error) {}

    private final EventRepo eventRepo;
    private final MenuItemRepo menuItemRepo;
    private final PreorderRepo preorderRepo;
    private final PreorderItemRepo preorderItemRepo;
    private final PrepListService prepListService;

    public ExportController(EventRepo eventRepo, MenuItemRepo menuItemRepo, PreorderRepo preorderRepo, PreorderItemRepo preorderItemRepo, PrepListService prepListService) {
        this.eventRepo = eventRepo;
        this.menuItemRepo = menuItemRepo;
        this.preorderRepo = preorderRepo;
        this.preorderItemRepo = preorderItemRepo;
        this.prepListService = prepListService;
    }

    // ---- CSV: Menu items export
    @GetMapping("/events/{id}/menu.csv")
    public ResponseEntity<byte[]> exportMenuCsv(@PathVariable Long id) throws IOException {
        Event e = eventRepo.findById(id).orElseThrow();
        List<MenuItem> items = menuItemRepo.findByEvent(e);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("name","station","capQty","priceCents","description"));
        for (MenuItem i : items) {
            csv.printRecord(i.getName(), i.getStation(), i.getCapQty(), i.getPriceCents(), i.getDescription());
        }
        csv.flush();
        byte[] bytes = baos.toByteArray();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=menu-" + id + ".csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(bytes);
    }

    // ---- CSV: Menu items import (append items)
    @PostMapping("/events/{id}/menu/import")
    public String importMenuCsv(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Event e = eventRepo.findById(id).orElseThrow();
        Reader in = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(in);
        for (CSVRecord r : records) {
            MenuItem mi = new MenuItem();
            mi.setEvent(e);
            mi.setName(r.get("name"));
            mi.setStation(opt(r,"station"));
            mi.setCapQty(parseInt(opt(r,"capQty")));
            mi.setPriceCents(parseInt(opt(r,"priceCents")));
            mi.setDescription(opt(r,"description"));
            menuItemRepo.save(mi);
        }
        return "redirect:/admin/events/" + id;
    }

    private String opt(CSVRecord r, String key) { return r.isMapped(key) ? r.get(key) : null; }
    private Integer parseInt(String s) { try { return s==null||s.isBlank()?null:Integer.valueOf(s.trim()); } catch(Exception ex){ return null; } }

    // ---- CSV: Preorders (flat) export
    @GetMapping("/events/{id}/preorders.csv")
    public ResponseEntity<byte[]> exportPreordersCsv(@PathVariable Long id) throws IOException {
        Event e = eventRepo.findById(id).orElseThrow();
        List<Preorder> pos = preorderRepo.findByEvent(e);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("userEmail","item","qty","station"));
        for (Preorder p : pos) {
            for (PreorderItem it : preorderItemRepo.findByPreorder(p)) {
                csv.printRecord(p.getUser().getEmail(), it.getMenuItem().getName(), it.getQty(), it.getMenuItem().getStation());
            }
        }
        csv.flush();
        byte[] bytes = baos.toByteArray();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=preorders-" + id + ".csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(bytes);
    }

    // ---- CSV: Prep list (aggregated) export
    @GetMapping("/events/{id}/prep-list.csv")
    public ResponseEntity<byte[]> exportPrepListCsv(@PathVariable Long id) throws IOException {
        Event e = eventRepo.findById(id).orElseThrow();
        Map<String,Integer> rows = prepListService.aggregateByStation(e);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("station_item","total"));
        for (Map.Entry<String,Integer> en : rows.entrySet()) {
            csv.printRecord(en.getKey(), en.getValue());
        }
        csv.flush();
        byte[] bytes = baos.toByteArray();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prep-list-" + id + ".csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(bytes);
    }

    // ---- PDF: Printable Menu
    @GetMapping(value="/events/{id}/menu.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> menuPdf(@PathVariable Long id) throws Exception {
        Event e = eventRepo.findById(id).orElseThrow();
        List<MenuItem> items = menuItemRepo.findByEvent(e);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font h1 = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font h2 = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font body = new Font(Font.FontFamily.HELVETICA, 11);

        if (e.getOrg() != null && e.getOrg().getLogoPath() != null) { try { com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(e.getOrg().getLogoPath()); img.scaleToFit(120, 120); img.setAlignment(Element.ALIGN_CENTER); doc.add(img); } catch(Exception ex) {} }\n        if (e.getCoverImagePath() != null) { try { com.itextpdf.text.Image cover = com.itextpdf.text.Image.getInstance(e.getCoverImagePath()); cover.scaleToFit(500, 220); cover.setAlignment(Element.ALIGN_CENTER); doc.add(cover); } catch(Exception ex) {} }
        Paragraph title = new Paragraph("Menu – " + e.getName(), h1);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        if (e.getStartsAt() != null) {
            Paragraph when = new Paragraph(e.getStartsAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), body);
            when.setAlignment(Element.ALIGN_CENTER);
            doc.add(when);
        }
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{3f, 5f, 1.2f});
        table.setWidthPercentage(100);
        addHeader(table, "Item", h2);
        addHeader(table, "Description", h2);
        addHeader(table, "Price", h2);
        for (MenuItem i : items) {
            table.addCell(new Phrase(i.getName()==null?"":i.getName(), body));
            table.addCell(new Phrase(i.getDescription()==null?"":i.getDescription(), body));
            String price = i.getPriceCents()==null?"":String.format("$%.2f", i.getPriceCents()/100.0);
            table.addCell(new Phrase(price, body));
        }
        doc.add(table);
        doc.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=menu-" + id + ".pdf")
                .body(baos.toByteArray());
    }

    // ---- PDF: Kitchen Tickets (one line per order item, grouped by station)
    @GetMapping(value="/events/{id}/tickets.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> ticketsPdf(@PathVariable Long id) throws Exception {
        Event e = eventRepo.findById(id).orElseThrow();
        List<Preorder> pos = preorderRepo.findByEvent(e);

        class Line { String station; String item; int qty; String email; }
        List<Line> lines = new ArrayList<>();
        for (Preorder p : pos) {
            for (PreorderItem it : preorderItemRepo.findByPreorder(p)) {
                Line ln = new Line();
                ln.station = it.getMenuItem().getStation()==null?"General":it.getMenuItem().getStation();
                ln.item = it.getMenuItem().getName();
                ln.qty = it.getQty();
                ln.email = p.getUser().getEmail();
                lines.add(ln);
            }
        }
        // Sort by station then item
        lines.sort(Comparator.comparing((Line l) -> l.station).thenComparing(l -> l.item));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font h1 = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font h2 = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font body = new Font(Font.FontFamily.HELVETICA, 11);

        if (e.getOrg() != null && e.getOrg().getLogoPath() != null) { try { com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(e.getOrg().getLogoPath()); img.scaleToFit(120, 120); img.setAlignment(Element.ALIGN_CENTER); doc.add(img); } catch(Exception ex) {} }\n        if (e.getCoverImagePath() != null) { try { com.itextpdf.text.Image cover = com.itextpdf.text.Image.getInstance(e.getCoverImagePath()); cover.scaleToFit(500, 220); cover.setAlignment(Element.ALIGN_CENTER); doc.add(cover); } catch(Exception ex) {} }
        Paragraph title = new Paragraph("Kitchen Tickets – " + e.getName(), h1);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{2f, 5f, 1f, 4f});
        table.setWidthPercentage(100);
        addHeader(table, "Station", h2);
        addHeader(table, "Item", h2);
        addHeader(table, "Qty", h2);
        addHeader(table, "Member", h2);
        for (Line ln : lines) {
            for (int ci=0; ci<Math.max(1, copies); ci++) {
            table.addCell(new Phrase(ln.station, body));
            table.addCell(new Phrase(ln.item, body));
            table.addCell(new Phrase(String.valueOf(ln.qty), body));
            table.addCell(new Phrase(ln.email, body));
        }
        doc.add(table);
        doc.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets-" + id + ".pdf")
                .body(baos.toByteArray());
    }

    private void addHeader(PdfPTable t, String label, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(label, f));
        c.setBackgroundColor(parseHex(e.getThemeAccentHex()));
        t.addCell(c);
    }
}

    @PostMapping("/events/{id}/menu/import/preview")
    public String importMenuPreview(@PathVariable Long id, @RequestParam("file") MultipartFile file, org.springframework.ui.Model model) throws IOException {
        Event e = eventRepo.findById(id).orElseThrow();
        java.io.Reader in = new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
        java.util.List<PreviewRow> rows = new java.util.ArrayList<>();
        boolean hasErrors = false;
        for (CSVRecord r : records) {
            String name = opt(r,"name");
            String station = opt(r,"station");
            Integer cap = parseInt(opt(r,"capQty"));
            Integer price = parseInt(opt(r,"priceCents"));
            String desc = opt(r,"description");
            String error = null;
            if (name == null || name.isBlank()) error = "name is required";
            if (cap != null && cap < 0) error = (error==null?"":"; ") + "capQty must be >= 0";
            if (price != null && price < 0) error = (error==null?"":"; ") + "priceCents must be >= 0";
            if (error != null) hasErrors = true;
            rows.add(new PreviewRow(name, station, cap, price, desc, error));
        }
        model.addAttribute("event", e);
        model.addAttribute("rows", rows);
        model.addAttribute("hasErrors", hasErrors);
        // Serialize back into hidden field for commit
        String json = new com.google.gson.Gson().toJson(rows);
        model.addAttribute("payload", json);
        return "admin/menu-import-preview";
    }

    @PostMapping("/events/{id}/menu/import/commit")
    public String importMenuCommit(@PathVariable Long id, @RequestParam("payload") String payload) {
        Event e = eventRepo.findById(id).orElseThrow();
        PreviewRow[] rows = new com.google.gson.Gson().fromJson(payload, PreviewRow[].class);
        for (PreviewRow r : rows) {
            if (r.error() != null) continue; // skip invalid rows
            MenuItem mi = new MenuItem();
            mi.setEvent(e);
            mi.setName(r.name());
            mi.setStation(r.station());
            mi.setCapQty(r.capQty());
            mi.setPriceCents(r.priceCents());
            mi.setDescription(r.description());
            menuItemRepo.save(mi);
        }
        return "redirect:/admin/events/" + id;
    }

    // ---- PDF: Receipt-style tickets (one ticket per line item, narrow width)
    @GetMapping(value="/events/{id}/tickets-receipt.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> ticketsReceipt(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam(name="copies", required=false, defaultValue="1") int copies) throws Exception {
        Event e = eventRepo.findById(id).orElseThrow();
        java.util.List<Preorder> pos = preorderRepo.findByEvent(e);

        class Line { String station; String item; int qty; String email; }
        java.util.List<Line> lines = new java.util.ArrayList<>();
        for (Preorder p : pos) {
            for (PreorderItem it : preorderItemRepo.findByPreorder(p)) {
                Line ln = new Line();
                ln.station = it.getMenuItem().getStation()==null?"General":it.getMenuItem().getStation();
                ln.item = it.getMenuItem().getName();
                ln.qty = it.getQty();
                ln.email = p.getUser().getEmail();
                lines.add(ln);
            }
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        // 80mm receipt ~ 226.8pt width. Height per ticket ~ 140pt; create one page per ticket.
        for (Line ln : lines) {
            for (int ci=0; ci<Math.max(1, copies); ci++) {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle(226.8f, 200f), 10,10,10,10);
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, baos);
            doc.open();
            com.itextpdf.text.Font header = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 12, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font body = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 10);

            if (e.getOrg() != null && e.getOrg().getLogoPath() != null) {
                try { com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(e.getOrg().getLogoPath()); img.scaleToFit(100, 40); doc.add(img); } catch(Exception ex) {}
            }
            if (e.getCoverImagePath() != null) {
                try { com.itextpdf.text.Image cover = com.itextpdf.text.Image.getInstance(e.getCoverImagePath()); cover.scaleToFit(200, 60); doc.add(cover); } catch(Exception ex) {}
            }

            com.itextpdf.text.Paragraph p1 = new com.itextpdf.text.Paragraph("STATION: " + ln.station, header);
            doc.add(p1);
            com.itextpdf.text.Paragraph p2 = new com.itextpdf.text.Paragraph(ln.item, header);
            doc.add(p2);
            com.itextpdf.text.Paragraph p3 = new com.itextpdf.text.Paragraph("QTY: " + ln.qty, header);
            doc.add(p3);
            com.itextpdf.text.Paragraph p4 = new com.itextpdf.text.Paragraph("MEMBER: " + ln.email, body);
            doc.add(p4);
            com.itextpdf.text.Paragraph p5 = new com.itextpdf.text.Paragraph("EVENT: " + e.getName(), body);
            doc.add(p5);

            doc.close();
            }
        }

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets-receipt-" + id + ".pdf")
                .body(baos.toByteArray());
    }
