package com.nttdata.bank.yanki.mapper;

import com.nttdata.bank.yanki.api.dto.WalletResponse;
import com.nttdata.bank.yanki.domain.model.WalletDocument;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Maps wallet domain models to API DTOs.
 */
@Mapper(componentModel = "spring")
public interface WalletMapper {

    /**
     * @param document wallet aggregate
     * @return API response
     */
    WalletResponse toResponse(WalletDocument document);

    /**
     * @param documents wallet aggregates
     * @return API responses
     */
    List<WalletResponse> toResponseList(List<WalletDocument> documents);
}