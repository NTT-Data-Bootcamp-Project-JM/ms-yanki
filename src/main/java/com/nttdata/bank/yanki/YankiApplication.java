package com.nttdata.bank.yanki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Yanki wallet. It never calls another microservice over HTTP.
 */
@SpringBootApplication
public class YankiApplication {

    /**
     * Starts the wallet service.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(YankiApplication.class, args);
    }
}
