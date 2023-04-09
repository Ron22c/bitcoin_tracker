package com.anymind.bitcoin_tracker.services;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.anymind.bitcoin_tracker.models.BitcoinModel;
import com.anymind.bitcoin_tracker.models.TransactionInfoModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;

public class BitcoinService {
    private JDBCClient jdbcClient;

    public BitcoinService(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Future<Integer> addEntries(double amount, LocalDateTime create_at) {
            Promise<Integer> addEntryPromise = Promise.promise();
            Promise<Double> lastEntryPromise = Promise.promise();

            getLastRecentEntry().onComplete(result -> {
                if (result.failed()) {
                    lastEntryPromise.fail(result.cause());;
                } else {
                    BitcoinModel entries = result.result();
                    double total = entries != null &&entries.getId() != null ? Double.parseDouble(entries.getTotal()) : 0;
                    total = total + amount;
                    lastEntryPromise.complete(total);
                }
            });


            lastEntryPromise.future().onComplete(result -> {
                if(result.failed()) {
                    addEntryPromise.fail(result.cause());
                } else {
                    Double total = result.result();
                    addEntriesToDb(amount, total, create_at).onComplete(res -> {
                        if(result.failed()) {
                            addEntryPromise.fail(result.cause());
                        } else {
                            addEntryPromise.complete(res.result());
                        }
                    });
                }
            });

            return addEntryPromise.future();

    }
    public Future<Integer> addEntriesToDb(double amount, double total, LocalDateTime created_at) {
        Promise<Integer> promise = Promise.promise();
 
        // Execute an INSERT query
        jdbcClient
            .updateWithParams("INSERT INTO bitcoid.bitcoin_transactions (amount, total, created_at) VALUES (?, ?, ?)",
                new JsonArray().add(amount).add(total).add(created_at), result -> {
                if (result.succeeded()) {
                // Handle the query result
                int numRows = result.result().getUpdated();

                promise.complete(numRows);

                } else {
                    promise.fail(result.cause());
                }
            });

        return promise.future();
    }

    public Future<BitcoinModel> getLastRecentEntry () {
        Promise<BitcoinModel> promise = Promise.promise();

        getTransactionData("SELECT * FROM bitcoid.bitcoin_transactions ORDER BY created_at desc LIMIT 1;")
            .onComplete(result -> {
                if (result.failed()) {
                    promise.fail(result.cause());
                } else {
                    List<BitcoinModel> entries = result.result();
                    BitcoinModel entry = entries.size()>0 ? entries.get(0) : null; 
                    promise.complete(entry);
                }
            });

        return promise.future();
    }

    public Future<List<BitcoinModel>> getTransactionData(String query) {
        Promise<List<BitcoinModel>> promise = Promise.promise();

        jdbcClient.query(query, result -> {
            if (result.succeeded()) {
              ObjectMapper mapper = new ObjectMapper();
              mapper.registerModule(new JavaTimeModule());
    
              List<BitcoinModel> dataList = new ArrayList<>();

              try {
                for(int i=0; i<result.result().getRows().size(); i++) {
                    String id = null;
                    String amount = null;
                    String total = null;
                    String created_at = null;

                    created_at = result.result().getRows().get(i).getString("created_at");
                    amount = result.result().getRows().get(i).getString("amount");
                    total = result.result().getRows().get(i).getString("total");
                    id = result.result().getRows().get(i).getString("id");
                    
                    OffsetDateTime dateTime_off = OffsetDateTime.parse(created_at);
                    LocalDateTime dateTime = dateTime_off.toLocalDateTime();
                    
                    dateTime = dateTime.plusHours(5).plusMinutes(30);
                    BitcoinModel bitcoinModel = new BitcoinModel(id, amount, total, dateTime);
    
                    dataList.add(bitcoinModel);
                }
    
              } catch (Exception e) {
                promise.fail(result.cause());
                return;
              }
              promise.complete(dataList);
            } else {
              // Handle the query failure
              promise.fail(result.cause());
            }
        });

        return promise.future();
    }

    public Future<List<BitcoinModel>> getEntries() {
        Promise<List<BitcoinModel>> promise = Promise.promise();

        getTransactionData("SELECT * FROM bitcoid.bitcoin_transactions;")
        .onComplete(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
            } else {
                List<BitcoinModel> entries = result.result();
                promise.complete(entries);
            }
        });

        return promise.future();
    }

    public Future<List<TransactionInfoModel>> getHistory(String startTime, String endTime ,LocalDateTime start, LocalDateTime end) {
        
        Promise<List<TransactionInfoModel>> promise = Promise.promise();

        String query = String
            .format("SELECT * FROM bitcoid.bitcoin_transactions where created_at >= '%s' and created_at <= '%s' ORDER BY created_at ASC;", startTime, endTime);
        
        getTransactionData(query)
        .onComplete(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
            } else {
                List<BitcoinModel> entries = result.result();
                List<TransactionInfoModel> tm = findValues(entries, start, end);
                promise.complete(tm);
            }
        });
        return promise.future();
    }

    public static List<TransactionInfoModel> findValues(List<BitcoinModel> master, LocalDateTime start, LocalDateTime end) {

        List<TransactionInfoModel> dateTimeList = new ArrayList<>();
        LocalDateTime current = start;

        int left = 0;
        int right = 1;

        while (current.isBefore(end) && right<master.size()) {
            String amount = "";
            if(current.isBefore(master.get(right).getCreated_at())) {
                amount=master.get(left).getTotal();
                dateTimeList.add(new TransactionInfoModel(amount, current));
                current = current.plus(1, ChronoUnit.HOURS);
            } else if(current.isEqual(master.get(right).getCreated_at())){
                amount=master.get(right).getTotal();
                dateTimeList.add(new TransactionInfoModel(amount, current));
                current = current.plus(1, ChronoUnit.HOURS);
            } else {
                left++;
                right++;
            }
        }

        while(current.isBefore(end)) {
            String amount=master.get(left).getTotal();
            dateTimeList.add(new TransactionInfoModel(amount, current));
            current = current.plus(1, ChronoUnit.HOURS);
        }

        return dateTimeList;
        
    }
}
