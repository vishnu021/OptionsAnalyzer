package com.vish.fno.manage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.config.TestConfig;
import com.vish.fno.config.CandleUtils;
import com.vish.fno.manage.orderflow.OrderHandler;
import com.vish.fno.manage.orderflow.StrategyExecutor;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.Candle;
import com.vish.fno.model.CandleMetaData;
import com.vish.fno.model.SymbolData;
import com.vish.fno.model.Ticker;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = TestConfig.class)
public class IntegrationTest {

    public static final String TEST_DATE = "2024-08-02";

    private static final String TICKER_FILE_PATH = "src/test/resources/2024-08-02/NIFTY_50.txt";
    private static final String CANDLESTICK_FILE_PATH = "src/test/resources/2024-08-02/NIFTY 50_2024-08-02.json";
    public static final String NIFTY_50 = "NIFTY 50";
    @MockBean
    private KiteService kiteService;
    @MockBean
    private CandlestickService candlestickService;

    @Autowired
    private OrderHandler orderHandler;

    @Autowired
    private StrategyExecutor strategyExecutor;

    @Autowired
    private TimeProvider timeProvider;

    @Autowired
    private DataCache dataCache;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(kiteService.getITMStock(anyString(), anyDouble(), eq(true))).thenReturn("NIFTY50100CE");
        when(kiteService.getITMStock(anyString(), anyDouble(), eq(false))).thenReturn("NIFTY50100PE");
        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean())).thenAnswer(invocation -> {
            com.zerodhatech.models.Order order = mock(com.zerodhatech.models.Order.class);
            KiteOpenOrder kiteOpenOrder = KiteOpenOrder.builder().order(order).isOrderPlaced(true).build();
            return Optional.of(kiteOpenOrder);
        });

        when(kiteService.buyOrder(anyString(), anyInt(), anyString(),anyBoolean())).thenAnswer(invocation -> {
            com.zerodhatech.models.Order order = mock(com.zerodhatech.models.Order.class);
            KiteOpenOrder kiteOpenOrder = KiteOpenOrder.builder().order(order).isOrderPlaced(true).build();
            return Optional.of(kiteOpenOrder);
        });
        mockGetCandlesticksFromBroker();
    }

    private void mockGetCandlesticksFromBroker() {
        when(candlestickService.getCandlesticksFromBroker(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    List<Candle> candles = CandleUtils.parseCandleFile(CANDLESTICK_FILE_PATH);
                    Date dateTime = Date.from(timeProvider.now().atZone(ZoneId.systemDefault()).toInstant());
                    List<Candle> data = candles.subList(0, TimeUtils.getIndexOfTimeStamp(dateTime));

                    SymbolData symbolData = new SymbolData(new CandleMetaData(NIFTY_50, TEST_DATE), data);
                    return Optional.of(symbolData);
                });
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    List<Candle> candles = CandleUtils.parseCandleFile(CANDLESTICK_FILE_PATH);
                    Date dateTime = Date.from(timeProvider.now().atZone(ZoneId.systemDefault()).toInstant());
                    List<Candle> data = candles.subList(0, TimeUtils.getIndexOfTimeStamp(dateTime));

                    SymbolData symbolData = new SymbolData(new CandleMetaData(NIFTY_50, TEST_DATE), data);
                    return Optional.of(symbolData);
                });
    }

    @Test
    void testFullIntegration() throws IOException {
        // Mock the required behaviors
        when(kiteService.isInitialised()).thenReturn(true);
        List<Ticker> tickers = CandleUtils.parseTickerFile(TICKER_FILE_PATH);
        when(kiteService.getSymbol(256265)).thenReturn(NIFTY_50);
        // Execute the update method to simulate 10 minutes of trading
        for (int i = 0; i < 375; i++) {
            // Update the mock time provider to return the next minute
            TestConfig.mockTimeProvider(timeProvider, i);
            strategyExecutor.update();
                List<Ticker> nextMinuteTickers = filterTickers(tickers, timeProvider.now());
                for(Ticker ticker: nextMinuteTickers) {
                    orderHandler.handleTicks(List.of(ticker));
                }
        }

        verify(kiteService, times(10)).buyOrder(anyString(), eq(75), anyString(), eq(false));
        verify(kiteService, times(10)).sellOrder(anyString(), anyDouble(), eq(75), anyString(), eq(false));
    }

    public static List<Ticker> filterTickers(List<Ticker> tickers, LocalDateTime localDate) {
        Date startDate = Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(localDate.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant());

        return tickers.stream()
                .filter(ticker -> ticker.getTickTimestamp().after(startDate) && ticker.getTickTimestamp().before(endDate))
                .collect(Collectors.toList());
    }

    private List<Ticker> readTickersFromFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filePath);
        return Arrays.asList(objectMapper.readValue(file, Ticker[].class));
    }
}
