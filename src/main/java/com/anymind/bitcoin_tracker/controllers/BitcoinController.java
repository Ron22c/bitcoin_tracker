package com.anymind.bitcoin_tracker.controllers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.anymind.bitcoin_tracker.models.BitcoinModel;
import com.anymind.bitcoin_tracker.models.TransactionInfoModel;
import com.anymind.bitcoin_tracker.services.BitcoinService;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class BitcoinController {
    private BitcoinService bitcoinService;


    public BitcoinController(BitcoinService bitcoinService) {
        this.bitcoinService = bitcoinService;
    }

    public Router getRouter(Vertx vertx) {
        Router router = Router.router(vertx);
        BodyHandler bodyHandler = BodyHandler.create();
        router.route().handler(bodyHandler);

        router.get("/transaction").handler(this::handleGetTransactions);
        router.get("/transaction/last").handler(this::handleGetLastRecentTransactions);
        router.post("/transaction").handler(this::handleAddTransactions);
        router.post("/transaction/history").handler(this::handleTransactionsHistory);

        return router;
    }

    private void handleGetTransactions(RoutingContext context) {
        bitcoinService.getEntries().onComplete(result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else {
                List<BitcoinModel> entries = result.result();
                JsonArray jsonArray = new JsonArray(entries.stream().map(BitcoinModel::toJson).collect(Collectors.toList()));
                context.response().putHeader("content-type", "application/json").end(jsonArray.encode());
            }
        });
    }

    private void handleGetLastRecentTransactions(RoutingContext context) {
        bitcoinService.getLastRecentEntry().onComplete(result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else {
                BitcoinModel entries = result.result();
                String res = entries != null ? entries.toJson().encode() : "{}";
                context.response().putHeader("content-type", "application/json").end(res);
            }
        });
    }


    private void handleTransactionsHistory(RoutingContext context) {
        RequestBody body = context.body();
        JsonObject requestBody = body.asJsonObject();        
        String start = requestBody.getString("start");
        String end = requestBody.getString("end");

        LocalDateTime startTime;
        LocalDateTime endTime;
        if(start != null && start.length() > 0 && end != null && end.length() > 0) {

            Pattern pattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}$");
            boolean isStartDateTime = pattern.matcher(start).matches();
            boolean isEndDateTime = pattern.matcher(end).matches();
            if(!isStartDateTime || !isEndDateTime) {
                HttpServerResponse response = context.response();
                response.setChunked(true);
                response.write("start or end format is wrong it should be like '2019-10-05T14:45:05+07:00'");
                response.end();
                return;
            }
            OffsetDateTime sD = OffsetDateTime.parse(start);
            startTime = sD.toLocalDateTime();
            OffsetDateTime nD = OffsetDateTime.parse(end);
            endTime = nD.toLocalDateTime();
        } else {
            HttpServerResponse response = context.response();
            response.setChunked(true);
            response.write("start and end should be present in request '2019-10-05T14:45:05+07:00'");
            response.end();
            return;
        }

        bitcoinService.getHistory(start, end, startTime, endTime).onComplete(result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else {
                List<TransactionInfoModel> entries = result.result();
                JsonArray jsonArray = new JsonArray(entries.stream().map(TransactionInfoModel::toJson).collect(Collectors.toList()));
                context.response().putHeader("content-type", "application/json").end(jsonArray.encode());
            }
        });

    }

    private void handleAddTransactions(RoutingContext context) {
            RequestBody body = context.body();
            JsonObject requestBody = body.asJsonObject();        
            String amount_val = requestBody.getString("amount");
            String created_at = requestBody.getString("created_at");

            double amount = 0;
            try {
                amount = Double.parseDouble(amount_val);
            } catch (Exception e) {
                HttpServerResponse response = context.response();
                response.setChunked(true);
                response.write("Amount should be double value Ex. 1 | 1.2");
                response.end();
                return;
            }


            LocalDateTime createdAt;
            if(created_at != null && created_at.length() > 0) {

                Pattern pattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}$");
                boolean isDateTime = pattern.matcher(created_at).matches();
                if(!isDateTime) {
                    HttpServerResponse response = context.response();
                    response.setChunked(true);
                    response.write("created_at format is wrong it should be like '2019-10-05T14:45:05+07:00'");
                    response.end();
                    return;
                }
                OffsetDateTime dateTime_off = OffsetDateTime.parse(created_at);
                createdAt = dateTime_off.toLocalDateTime();
                
            } else {
                createdAt = LocalDateTime.now();
            }


            bitcoinService.addEntries(amount, createdAt).onComplete(result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else {
                int entries = result.result();
                context.response().putHeader("content-type", "application/json").end(""+entries);
            }
        });
    }
}
