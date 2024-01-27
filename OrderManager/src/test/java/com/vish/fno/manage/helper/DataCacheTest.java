package com.vish.fno.manage.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.util.FileUtils;
import com.vish.fno.reader.service.KiteService;
import com.zerodhatech.models.Instrument;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.vish.fno.manage.util.Constants.NIFTY_50;
import static com.vish.fno.manage.util.Constants.NIFTY_BANK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
class DataCacheTest {

    @Mock
    private FileUtils fileUtils;

    @Mock
    private KiteService kiteService;

    @Mock
    private OrderConfiguration orderConfiguration;

    @InjectMocks
    private DataCache dataCache;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(orderConfiguration.getSymbolsPath()).thenReturn("ind_nifty100list.csv");

        String[] additionalSymbols = new String[] {
                NIFTY_50, NIFTY_BANK, "FINNIFTY", "NIFTY FIN SERVICE", "BANKNIFTY", "NIFTY"
        };
        when(orderConfiguration.getAdditionalSymbols()).thenReturn(additionalSymbols);

        List<Instrument> instrumentsFromJson = objectMapper.readValue(
                new File("src/test/resources/instrument_cache/instruments_2023-12-26.json"),
                new TypeReference<List<Instrument>>() {
                }
        );
        when(kiteService.getAllInstruments()).thenReturn(instrumentsFromJson);

    }

    @Test
    void testGetInstruments() {
        // Arrange
        // Act
        List<Instrument> result = dataCache.getInstruments();

        // Assert
        assertNotNull(result);
        assertEquals(result.size(), 33439);
    }

    @Test
    void testGetExpiryDates() {
        // Arrange
        Set<String> expectedExpiryDates = Set.of("2025-06-26", "2025-12-24", "2027-12-30", "2024-09-26", "2024-02-27", "2024-02-29", "2024-01-17", "2024-01-16", "2024-01-18", "2024-12-26", "2026-06-25", "2028-06-29", "2024-06-27", "2024-01-31", "2024-01-30", "2024-01-11", "2024-02-01", "2024-01-10", "2024-01-02", "2024-03-28", "2024-01-23", "2024-01-04", "2024-01-25", "2026-12-31", "2024-01-03", "2027-06-24", "2024-01-09", "2023-12-26", "2023-12-28");
        // Act
        Set<String> actualExpiryDates = dataCache.getExpiryDates();

        // Assert
        assertNotNull(actualExpiryDates);
        assertEquals(expectedExpiryDates, actualExpiryDates);
    }

    @Test
    void getITMStockForACallOption() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock(NIFTY_BANK, 45126d, true);

        // Assert
        assertEquals("BANKNIFTY23DEC45100CE", optionSymbol);
    }

    @Test
    void getITMStockForAPutOption() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock(NIFTY_BANK, 45126d, false);

        // Assert
        assertEquals("BANKNIFTY23DEC45200PE", optionSymbol);
    }

    @Test
    void getITMStockForAStockCallOption() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("HINDUNILVR", 2600d, true);

        // Assert
        assertEquals("HINDUNILVR23DEC2600CE", optionSymbol);
    }

    @Test
    void getITMStockForAStockPutOption() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("HINDUNILVR", 2600d, false);

        // Assert
        assertEquals("HINDUNILVR23DEC2620PE", optionSymbol);
    }



    @Test
    void getITMStockForAStockCallOption2() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("HDFCBANK", 1748d, true);

        // Assert
        assertEquals("HDFCBANK23DEC1740CE", optionSymbol);
    }

    @Test
    void getITMStockForAStockPutOption2() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("HDFCBANK", 1748d, false);

        // Assert
        assertEquals("HDFCBANK23DEC1750PE", optionSymbol);
    }


    @Test
    void getITMStockForAStockCallOption3() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("HDFCBANK", 1748d, true);

        // Assert
        assertEquals("HDFCBANK23DEC1740CE", optionSymbol);
    }

    @Test
    void getITMStockForAStockPutOption3() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("HDFCBANK", 1748d, false);

        // Assert
        assertEquals("HDFCBANK23DEC1750PE", optionSymbol);
    }

    @Test
    void getITMStockForAStockCallOption4() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("RELIANCE", 2245d, true);

        // Assert
        assertEquals("RELIANCE23DEC2240CE", optionSymbol);
    }

    @Test
    void getITMStockForAStockPutOption4() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("RELIANCE", 2245d, false);

        // Assert
        assertEquals("RELIANCE23DEC2260PE", optionSymbol);
    }

    @Test
    void getITMStockForAStockCallOption5() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("BAJFINANCE", 7226d, true);

        // Assert
        assertEquals("BAJFINANCE23DEC7200CE", optionSymbol);
    }

    @Test
    void getITMStockForAStockPutOption5() {
        // Arrange
        // Act
        String optionSymbol = dataCache.getITMStock("BAJFINANCE", 7225d, false);

        // Assert
        assertEquals("BAJFINANCE23DEC7250PE", optionSymbol);
    }

}