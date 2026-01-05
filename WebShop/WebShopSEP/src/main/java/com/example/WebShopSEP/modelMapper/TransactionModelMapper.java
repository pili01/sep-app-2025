package com.example.WebShopSEP.modelMapper;

import com.example.WebShopSEP.dto.transaction.TransactionDto;
import com.example.WebShopSEP.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {UserModelMapper.class}, nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface TransactionModelMapper {
    TransactionDto toDto(Transaction entity);
    Set<TransactionDto> toDtos(Set<Transaction> entity);
}
