package com.anymind.bitcoin_tracker.models;

import java.time.LocalDateTime;

import io.vertx.core.json.JsonObject;

public class TransactionInfoModel {
    private String amount;
    private LocalDateTime dateTime;


    public String getAmount() {
        return amount;
    }




    public void setAmount(String amount) {
        this.amount = amount;
    }




    public LocalDateTime getDateTime() {
        return dateTime;
    }




    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }




    public TransactionInfoModel(String amount, LocalDateTime dateTime) {
        this.amount = amount;
        this.dateTime = dateTime;
    }

    


    public JsonObject toJson() {
        return new JsonObject()
            .put("amount", this.amount)
            .put("dateTime", this.dateTime.toString());
    }
}
