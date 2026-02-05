package org.jdbc.portfoliomanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbc.portfoliomanagement.entity.Holding;
import org.jdbc.portfoliomanagement.entity.HistoricalPrice;
import org.jdbc.portfoliomanagement.service.HistoricalPriceService;
import org.jdbc.portfoliomanagement.service.HoldingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HoldingController.class)
@ActiveProfiles("test")
class HoldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HoldingService holdingService;

    @MockBean
    private HistoricalPriceService historicalPriceService;

    private Holding testHolding;
    private List<Holding> holdingList;

    @BeforeEach
    void setUp() {
        testHolding = new Holding(
                "STOCK",
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                LocalDate.of(2025, 1, 15)
        );
        testHolding.setId(1L);
        testHolding.setCurrentPrice(new BigDecimal("160.00"));

        holdingList = new ArrayList<>();
        holdingList.add(testHolding);
    }

    @Test
    void testGetAllHoldings() throws Exception {
        // Given
        when(holdingService.getAllHoldings()).thenReturn(holdingList);

        // When & Then
        mockMvc.perform(get("/api/holdings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].assetType").value("STOCK"));

        verify(holdingService, times(1)).getAllHoldings();
    }

    @Test
    void testGetHoldingById() throws Exception {
        // Given
        when(holdingService.getHoldingById(1L)).thenReturn(Optional.of(testHolding));

        // When & Then
        mockMvc.perform(get("/api/holdings/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.id").value(1));

        verify(holdingService, times(1)).getHoldingById(1L);
    }

    @Test
    void testGetHoldingByIdNotFound() throws Exception {
        // Given
        when(holdingService.getHoldingById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/holdings/999"))
                .andExpect(status().isNotFound());

        verify(holdingService, times(1)).getHoldingById(999L);
    }

    @Test
    void testCreateHolding() throws Exception {
        // Given
        when(holdingService.createHolding(any(Holding.class))).thenReturn(testHolding);
        when(historicalPriceService.getHistoricalPrices(anyString())).thenReturn(Collections.emptyList());
        when(historicalPriceService.fetchAndStoreHistoricalData(anyString(), anyString())).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testHolding)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"));

        verify(holdingService, times(1)).createHolding(any(Holding.class));
    }

    @Test
    void testUpdateHolding() throws Exception {
        // Given
        Holding updatedHolding = new Holding(
                "STOCK",
                "AAPL",
                new BigDecimal("20"),
                new BigDecimal("160.00"),
                LocalDate.of(2025, 1, 15)
        );
        updatedHolding.setId(1L);

        when(holdingService.updateHolding(anyLong(), any(Holding.class))).thenReturn(Optional.of(updatedHolding));

        // When & Then
        mockMvc.perform(put("/api/holdings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedHolding)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(holdingService, times(1)).updateHolding(anyLong(), any(Holding.class));
    }

    @Test
    void testUpdateHoldingNotFound() throws Exception {
        // Given
        when(holdingService.updateHolding(anyLong(), any(Holding.class))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/holdings/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testHolding)))
                .andExpect(status().isNotFound());

        verify(holdingService, times(1)).updateHolding(anyLong(), any(Holding.class));
    }

    @Test
    void testDeleteHolding() throws Exception {
        // Given
        when(holdingService.deleteHolding(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/holdings/1"))
                .andExpect(status().isNoContent());

        verify(holdingService, times(1)).deleteHolding(1L);
    }

    @Test
    void testDeleteHoldingNotFound() throws Exception {
        // Given
        when(holdingService.deleteHolding(999L)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/holdings/999"))
                .andExpect(status().isNotFound());

        verify(holdingService, times(1)).deleteHolding(999L);
    }

    @Test
    void testGetHoldingsByAssetType() throws Exception {
        // Given
        when(holdingService.getHoldingsByAssetType("STOCK")).thenReturn(holdingList);

        // When & Then
        mockMvc.perform(get("/api/holdings/assetType/STOCK"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assetType").value("STOCK"));

        verify(holdingService, times(1)).getHoldingsByAssetType("STOCK");
    }

    @Test
    void testHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Holding Service is up and running!"));
    }

    @Test
    void testGetHistoricalPrices() throws Exception {
        // Given
        List<HistoricalPrice> prices = Arrays.asList(
                new HistoricalPrice("AAPL", new BigDecimal("150.00"), LocalDate.of(2026, 1, 1)),
                new HistoricalPrice("AAPL", new BigDecimal("155.00"), LocalDate.of(2026, 1, 15))
        );
        when(historicalPriceService.getHistoricalPrices("AAPL")).thenReturn(prices);

        // When & Then
        mockMvc.perform(get("/api/historical/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(historicalPriceService, times(1)).getHistoricalPrices("AAPL");
    }

    @Test
    void testGetPortfolioSummary() throws Exception {
        // Given
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalValue", new BigDecimal("10000.00"));
        summary.put("totalGain", new BigDecimal("1000.00"));
        when(holdingService.getPortfolioSummary()).thenReturn(summary);

        // When & Then
        mockMvc.perform(get("/api/portfolio/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(holdingService, times(1)).getPortfolioSummary();
    }

    @Test
    void testGetBestPerformer() throws Exception {
        // Given
        when(holdingService.getBestPerformer()).thenReturn(testHolding);

        // When & Then
        mockMvc.perform(get("/api/portfolio/best-performer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));

        verify(holdingService, times(1)).getBestPerformer();
    }

    @Test
    void testGetBestPerformerNoContent() throws Exception {
        // Given
        when(holdingService.getBestPerformer()).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/portfolio/best-performer"))
                .andExpect(status().isNoContent());

        verify(holdingService, times(1)).getBestPerformer();
    }

    @Test
    void testGetWorstPerformer() throws Exception {
        // Given
        when(holdingService.getWorstPerformer()).thenReturn(testHolding);

        // When & Then
        mockMvc.perform(get("/api/portfolio/worst-performer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));

        verify(holdingService, times(1)).getWorstPerformer();
    }

    @Test
    void testSearchStocks() throws Exception {
        // Given
        Map<String, Object> searchResults = new HashMap<>();
        searchResults.put("results", Arrays.asList("AAPL", "GOOGL"));
        when(holdingService.searchStocks("AAPL")).thenReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/api/search/stocks").param("query", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(holdingService, times(1)).searchStocks("AAPL");
    }
}
