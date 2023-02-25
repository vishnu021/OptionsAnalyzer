package com.vish.fno.manage.dao;

import com.vish.fno.technical.model.Candlestick;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandlestickRepository extends MongoRepository<Candlestick, String> {
    List<Candlestick> findByRecordDateAndRecordSymbol(String date, String symbol);

}
