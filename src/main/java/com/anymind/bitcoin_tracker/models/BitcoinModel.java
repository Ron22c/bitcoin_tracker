package com.anymind.bitcoin_tracker.models;

import java.time.LocalDateTime;

import io.vertx.core.json.JsonObject;

public class BitcoinModel {
    private String id;
    private String amount;
    private String total;
    private LocalDateTime created_at;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public BitcoinModel(String id, String amount, String total, LocalDateTime created_at) {
        this.id = id;
        this.amount = amount;
        this.total = total;
        this.created_at = created_at;
    }

    public JsonObject toJson() {
        return new JsonObject()
            .put("id", this.id)
            .put("amount", this.amount)
            .put("total", this.total)
            .put("created_at", this.created_at.toString());
    }
}
