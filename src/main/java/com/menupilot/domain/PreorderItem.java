package com.menupilot.domain;

import jakarta.persistence.*;

@Entity
public class PreorderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) private Preorder preorder;
    @ManyToOne(optional=false) private MenuItem menuItem;
    @Column(nullable=false) private Integer qty = 1;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Preorder getPreorder() { return preorder; }
    public void setPreorder(Preorder preorder) { this.preorder = preorder; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
}
