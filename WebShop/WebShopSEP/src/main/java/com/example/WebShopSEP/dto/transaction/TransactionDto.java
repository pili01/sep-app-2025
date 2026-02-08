package com.example.WebShopSEP.dto.transaction;

import com.example.WebShopSEP.dto.user.UserDto;
import com.example.WebShopSEP.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter @Setter
@AllArgsConstructor
public class TransactionDto {
    private Long id;

    private UserDto user;

    private String transactionId;

    private Long rentalId;

    private TransactionStatus status;

    private Timestamp timestamp;

    private String paymentMethod;
}
