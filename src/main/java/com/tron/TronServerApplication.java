package com.tron;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tron")
public class TronServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(TronServerApplication.class, args);
  }
}
