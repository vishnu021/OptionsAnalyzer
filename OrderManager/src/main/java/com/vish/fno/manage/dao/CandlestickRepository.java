package com.vish.fno.manage.dao;

import com.vish.fno.model.CandleStick;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandlestickRepository extends MongoRepository<CandleStick, String> {
    List<CandleStick> findByRecordDateAndRecordSymbol(String date, String symbol);

}
