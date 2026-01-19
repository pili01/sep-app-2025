package com.example;

import com.example.service.PSPCoreClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PSPBankApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(PSPBankApplication.class, args);

        PSPCoreClient client = context.getBean(PSPCoreClient.class);

        client.connectToCorePSP();
    }
}
