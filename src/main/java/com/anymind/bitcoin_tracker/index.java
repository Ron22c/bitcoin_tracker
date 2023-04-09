package com.anymind.bitcoin_tracker;

import com.anymind.bitcoin_tracker.controllers.BitcoinController;
import com.anymind.bitcoin_tracker.services.BitcoinService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;

public class index extends AbstractVerticle{
    private JDBCClient jdbcClient;

    public index(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void start() throws Exception {
        BitcoinService bitcoinService = new BitcoinService(jdbcClient);
        BitcoinController bitcoinController = new BitcoinController(bitcoinService);

        Router mainRouter = Router.router(vertx);
        
        mainRouter.route("/api/*").subRouter(bitcoinController.getRouter(vertx));
        
        vertx.createHttpServer()
            .requestHandler(mainRouter)
            .listen(8080, result -> {
                if (result.succeeded()) {
                    System.out.println("Server started on port " + result.result().actualPort());
                } else {
                    System.err.println("Failed to start server: " + result.cause());
                }
            });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        JsonObject dbConfig = new JsonObject()
                .put("url", "jdbc:postgresql://localhost:5432/postgres")
                .put("driver_class", "org.postgresql.Driver")
                .put("user", "postgres")
                .put("password", "#########");

        JDBCClient jdbcClient = JDBCClient.createShared(vertx, dbConfig);

        index index = new index(jdbcClient);

        vertx.deployVerticle(index, result -> {
            if (result.failed()) {
                System.err.println("Failed to deploy verticle: " + result.cause());
            }
        });
    }    
}
