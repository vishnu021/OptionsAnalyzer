package com.vish.fno.manage.dao;

import com.vish.fno.model.OptionSymbolData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OptionsRepository extends MongoRepository<OptionSymbolData, String> {
    Optional<OptionSymbolData> findByRecordDateAndRecordSymbol(String date, String symbol);

}
