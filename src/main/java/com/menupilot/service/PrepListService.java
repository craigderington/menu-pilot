package com.menupilot.service;

import com.menupilot.domain.*;
import com.menupilot.repo.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrepListService {
    private final PreorderRepo preorderRepo;
    private final PreorderItemRepo preorderItemRepo;
    private final MenuItemRepo menuItemRepo;

    public PrepListService(PreorderRepo preorderRepo, PreorderItemRepo preorderItemRepo, MenuItemRepo menuItemRepo) {
        this.preorderRepo = preorderRepo;
        this.preorderItemRepo = preorderItemRepo;
        this.menuItemRepo = menuItemRepo;
    }

    public Map<String, Integer> aggregateByStation(Event event) {
        Map<String, Integer> byStation = new LinkedHashMap<>();
        List<Preorder> pos = preorderRepo.findByEvent(event);
        for (Preorder p : pos) {
            for (PreorderItem it : preorderItemRepo.findByPreorder(p)) {
                MenuItem mi = it.getMenuItem();
                String key = (mi.getStation() == null ? "General" : mi.getStation()) + " :: " + mi.getName();
                byStation.merge(key, it.getQty(), Integer::sum);
            }
        }
        return byStation;
    }
}
