package com.vish.fno.manage;

import com.vish.fno.config.TestConfig;
import com.vish.fno.config.CandleUtils;
import com.vish.fno.manage.orderflow.OrderHandler;
import com.vish.fno.manage.orderflow.StrategyExecutor;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.Candle;
import com.vish.fno.model.CandleMetaData;
import com.vish.fno.model.SymbolData;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.TimeProvider;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = TestConfig.class)
public class IntegrationTest {

    public static final String TEST_DATE = "2024-08-30";
    private static final String INDEX = "NIFTY 50";
    private static final long INDEX_TOKEN = 256265;
    private static final String CALL_OPTION = "NIFTY2490525250CE";
    private static final String PUT_OPTION = "NIFTY2490525250PE";
    private static final String BASE_PATH = String.format("src/test/resources/%s/", TEST_DATE);
    private static final String TICKER_FILE_PATH = String.format("%s%s.txt", BASE_PATH, INDEX.replaceAll(" ", "_"));
    private static final String CE_TICKER_FILE_PATH = String.format("%s%s.txt", BASE_PATH, CALL_OPTION);
    private static final String PE_TICKER_FILE_PATH = String.format("%s%s.txt", BASE_PATH, PUT_OPTION);
    private static final String CANDLESTICK_FILE_PATH_NIFTY = String.format("%s%s_%s.json", BASE_PATH, INDEX, TEST_DATE);
    private static final String CANDLESTICK_FILE_PATH_NIFTY_CE = String.format("%s%s%s.json", BASE_PATH, CALL_OPTION, TEST_DATE);
    private static final String CANDLESTICK_FILE_PATH_NIFTY_PE = String.format("%s%s%s.json", BASE_PATH, PUT_OPTION, TEST_DATE);

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
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doNothing().when(kiteService).appendWebSocketSymbolsList(anyList(), anyBoolean());
        when(kiteService.getITMStock(anyString(), anyDouble(), eq(true))).thenReturn(CALL_OPTION);
        when(kiteService.getITMStock(anyString(), anyDouble(), eq(false))).thenReturn(PUT_OPTION);
        when(kiteService.sellOrder(anyString(), anyInt(), anyString(), anyBoolean())).thenAnswer(invocation -> {
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
                    String symbol = invocation.getArgument(0);
                    log.info("symbol passed to getCandlesticksFromBroker: {}", symbol);
                    List<Candle> candles = CandleUtils.parseCandleFile(CANDLESTICK_FILE_PATH_NIFTY);
                    Date dateTime = Date.from(timeProvider.now().atZone(ZoneId.systemDefault()).toInstant());
                    List<Candle> data = candles.subList(0, TimeUtils.getIndexOfTimeStamp(dateTime));

                    SymbolData symbolData = new SymbolData(new CandleMetaData(INDEX, TEST_DATE), data);
                    return Optional.of(symbolData);
                });
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String symbol = invocation.getArgument(1);
                    String candleStickFilePath = switch (symbol) {
                        case INDEX -> CANDLESTICK_FILE_PATH_NIFTY;
                        case CALL_OPTION -> CANDLESTICK_FILE_PATH_NIFTY_CE;
                        case PUT_OPTION -> CANDLESTICK_FILE_PATH_NIFTY_PE;
                        default -> null;
                    };
                    List<Candle> candles = CandleUtils.parseCandleFile(candleStickFilePath);
                    Date dateTime = Date.from(timeProvider.now().atZone(ZoneId.systemDefault()).toInstant());
                    int timeIndex = TimeUtils.getIndexOfTimeStamp(dateTime);
                    List<Candle> data = candles.subList(0, timeIndex);

                    SymbolData symbolData = new SymbolData(new CandleMetaData(symbol, TEST_DATE), data);
                    return Optional.of(symbolData);
                });
    }

    @Test
    void testFullIntegration() throws IOException {
        // Mock the required behaviors
        List<Tick> allTickers = getAllTicks();
        when(kiteService.isInitialised()).thenReturn(true);
        when(kiteService.getSymbol(INDEX_TOKEN)).thenReturn(INDEX);
        when(kiteService.getSymbol(9909506)).thenReturn(CALL_OPTION);
        when(kiteService.getSymbol(9909762)).thenReturn(PUT_OPTION);

        // Execute the update method to simulate 10 minutes of trading
        for (int i = 0; i < 375; i++) {
            TestConfig.mockTimeProvider(timeProvider, i);
            strategyExecutor.update();

            List<Tick> nextMinuteTickers = getNextMinuteTicks(allTickers, timeProvider.now());
            List<List<Tick>> nextTicks = groupTicksByMillisecond(nextMinuteTickers);
            for (List<Tick> ticks : nextTicks) {
                orderHandler.handleTicks(ticks);
            }
        }

        verify(kiteService, times(11)).buyOrder(anyString(), eq(75), anyString(), eq(false));
        verify(kiteService, times(11)).sellOrder(anyString(), eq(75), anyString(), eq(true));
    }

    @NotNull
    private static List<Tick> getAllTicks() throws IOException {
        List<Tick> indexTickers = CandleUtils.parseTickerFile(TICKER_FILE_PATH);
        List<Tick> ceTickers = CandleUtils.parseTickerFile(CE_TICKER_FILE_PATH);
        List<Tick> peTickers = CandleUtils.parseTickerFile(PE_TICKER_FILE_PATH);

        List<Tick> allTickers = new ArrayList<>();
        allTickers.addAll(indexTickers);
        allTickers.addAll(ceTickers);
        allTickers.addAll(peTickers);

        allTickers.sort(Comparator.comparing(Tick::getTickTimestamp));
        return allTickers;
    }

    public static List<Tick> getNextMinuteTicks(List<Tick> tickers, LocalDateTime localDate) {
        Date startDate = Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(localDate.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant());
        return tickers.stream()
                .filter(ticker -> ticker.getTickTimestamp().after(startDate) && ticker.getTickTimestamp().before(endDate))
                .collect(Collectors.toList());
    }

    public static List<List<Tick>> groupTicksByMillisecond(List<Tick> tickers) {
        Map<Long, List<Tick>> groupedByMillisecond = new LinkedHashMap<>();

        for (Tick ticker : tickers) {
            long timestampInMillis = ticker.getTickTimestamp().getTime();
            groupedByMillisecond
                    .computeIfAbsent(timestampInMillis, k -> new ArrayList<>())
                    .add(ticker);
        }

        return new ArrayList<>(groupedByMillisecond.values());
    }
}
