package org.jdbc.portfoliomanagement.service;

import org.jdbc.portfoliomanagement.entity.HistoricalPrice;
import org.jdbc.portfoliomanagement.repository.HistoricalPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalPriceServiceTest {

    @Mock
    private HistoricalPriceRepository historicalPriceRepository;

    @Mock
    private YahooFinanceService yahooFinanceService;

    @InjectMocks
    private HistoricalPriceService historicalPriceService;

    private List<HistoricalPrice> testHistoricalPrices;
    private List<YahooFinanceService.HistoricalData> mockYahooData;

    @BeforeEach
    void setUp() {
        testHistoricalPrices = new ArrayList<>();
        testHistoricalPrices.add(new HistoricalPrice("AAPL", new BigDecimal("150.00"), LocalDate.of(2026, 1, 1)));
        testHistoricalPrices.add(new HistoricalPrice("AAPL", new BigDecimal("155.00"), LocalDate.of(2026, 1, 15)));

        mockYahooData = new ArrayList<>();
        mockYahooData.add(new YahooFinanceService.HistoricalData("AAPL", new BigDecimal("150.00"), LocalDate.of(2026, 1, 1)));
        mockYahooData.add(new YahooFinanceService.HistoricalData("AAPL", new BigDecimal("155.00"), LocalDate.of(2026, 1, 15)));
    }

    @Test
    void testGetHistoricalPrices() {
        // Given
        when(historicalPriceRepository.findBySymbolOrderByPriceDateAsc("AAPL")).thenReturn(testHistoricalPrices);

        // When
        List<HistoricalPrice> result = historicalPriceService.getHistoricalPrices("AAPL");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
        verify(historicalPriceRepository, times(1)).findBySymbolOrderByPriceDateAsc("AAPL");
    }

    @Test
    void testGetHistoricalPricesWithEmptySymbol() {
        // When
        List<HistoricalPrice> result = historicalPriceService.getHistoricalPrices("");

        // Then
        assertThat(result).isEmpty();
        verify(historicalPriceRepository, never()).findBySymbolOrderByPriceDateAsc(anyString());
    }

    @Test
    void testGetHistoricalPricesWithNullSymbol() {
        // When
        List<HistoricalPrice> result = historicalPriceService.getHistoricalPrices(null);

        // Then
        assertThat(result).isEmpty();
        verify(historicalPriceRepository, never()).findBySymbolOrderByPriceDateAsc(anyString());
    }

    @Test
    void testGetHistoricalPricesTrimsSymbol() {
        // Given
        when(historicalPriceRepository.findBySymbolOrderByPriceDateAsc("AAPL")).thenReturn(testHistoricalPrices);

        // When
        List<HistoricalPrice> result = historicalPriceService.getHistoricalPrices("  AAPL  ");

        // Then
        assertThat(result).hasSize(2);
        verify(historicalPriceRepository, times(1)).findBySymbolOrderByPriceDateAsc("AAPL");
    }

    @Test
    void testFetchAndStoreHistoricalDataForStock() {
        // Given
        when(yahooFinanceService.getHistoricalData("AAPL")).thenReturn(mockYahooData);
        when(historicalPriceRepository.saveAll(anyList())).thenReturn(testHistoricalPrices);

        // When
        List<HistoricalPrice> result = historicalPriceService.fetchAndStoreHistoricalData("AAPL", "STOCK");

        // Then
        assertThat(result).hasSize(2);
        verify(yahooFinanceService, times(1)).getHistoricalData("AAPL");
        verify(historicalPriceRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testFetchAndStoreHistoricalDataForMutualFund() {
        // Given
        // For mutual fund, the service uses a different API (not yahooFinanceService)
        // No stubbing needed for yahooFinanceService since it won't be called

        // When
        historicalPriceService.fetchAndStoreHistoricalData("123456", "MF");

        // Then
        // Mutual fund API call will likely return empty or fail, so no save should occur
        verify(historicalPriceRepository, never()).saveAll(anyList());
        // Verify that yahooFinanceService was NOT called for mutual funds
        verify(yahooFinanceService, never()).getHistoricalData(anyString());
    }

    @Test
    void testFetchAndStoreHistoricalDataWithEmptyResponse() {
        // Given
        when(yahooFinanceService.getHistoricalData("INVALID")).thenReturn(Collections.emptyList());

        // When
        List<HistoricalPrice> result = historicalPriceService.fetchAndStoreHistoricalData("INVALID", "STOCK");

        // Then
        assertThat(result).isEmpty();
        verify(yahooFinanceService, times(1)).getHistoricalData("INVALID");
        verify(historicalPriceRepository, never()).saveAll(anyList());
    }

    @Test
    void testFetchAndStoreHistoricalDataHandlesException() {
        // Given
        when(yahooFinanceService.getHistoricalData("AAPL")).thenThrow(new RuntimeException("API Error"));

        // When
        List<HistoricalPrice> result = historicalPriceService.fetchAndStoreHistoricalData("AAPL", "STOCK");

        // Then
        assertThat(result).isEmpty();
        verify(yahooFinanceService, times(1)).getHistoricalData("AAPL");
        verify(historicalPriceRepository, never()).saveAll(anyList());
    }

    @Test
    void testFetchAndStoreHistoricalDataWithNullAssetType() {
        // Given
        when(yahooFinanceService.getHistoricalData("AAPL")).thenReturn(mockYahooData);
        when(historicalPriceRepository.saveAll(anyList())).thenReturn(testHistoricalPrices);

        // When
        List<HistoricalPrice> result = historicalPriceService.fetchAndStoreHistoricalData("AAPL", null);

        // Then
        // Should fallback to trying stock first
        assertThat(result).hasSize(2);
        verify(yahooFinanceService, times(1)).getHistoricalData("AAPL");
    }

    @Test
    void testFetchAndStoreHistoricalDataNormalizesAssetType() {
        // Given
        when(yahooFinanceService.getHistoricalData("AAPL")).thenReturn(mockYahooData);
        when(historicalPriceRepository.saveAll(anyList())).thenReturn(testHistoricalPrices);

        // When - lowercase "stock" should be normalized
        List<HistoricalPrice> result = historicalPriceService.fetchAndStoreHistoricalData("AAPL", "stock");

        // Then
        assertThat(result).hasSize(2);
        verify(yahooFinanceService, times(1)).getHistoricalData("AAPL");
        verify(historicalPriceRepository, times(1)).saveAll(anyList());
    }
}
