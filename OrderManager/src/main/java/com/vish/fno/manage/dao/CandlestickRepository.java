package com.vish.fno.manage.dao;

import com.vish.fno.model.SymbolData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandlestickRepository extends MongoRepository<SymbolData, String> {
    Optional<SymbolData> findByRecordDateAndRecordSymbol(String date, String symbol);

}
