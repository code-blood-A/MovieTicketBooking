package com.moviebooking.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * PaymentServiceApplication — entry point for Payment Service.
 *
 * @EnableFeignClients: scans this package for @FeignClient interfaces
 * and registers their Spring-generated proxy implementations as beans.
 * Without this, BookingServiceClient cannot be injected anywhere.
 */
@SpringBootApplication
@EnableFeignClients
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
