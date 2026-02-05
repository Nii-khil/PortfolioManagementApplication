package org.jdbc.portfoliomanagement.service;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.jdbc.portfoliomanagement.repository.HoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private YahooFinanceService yahooFinanceService;

    @InjectMocks
    private HoldingService holdingService;

    private Holding testHolding;

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
    }

    @Test
    void testGetAllHoldings() {
        // Given
        List<Holding> holdings = new ArrayList<>();
        holdings.add(testHolding);
        when(holdingRepository.findAll()).thenReturn(holdings);
        when(yahooFinanceService.getCurrentPrice(anyString())).thenReturn(BigDecimal.valueOf(160.00));

        // When
        List<Holding> result = holdingService.getAllHoldings();

        // Then
        assertThat(result).hasSize(1);
        verify(holdingRepository, times(1)).findAll();
    }

    @Test
    void testGetHoldingById() {
        // Given
        when(holdingRepository.findById(1L)).thenReturn(Optional.of(testHolding));
        when(yahooFinanceService.getCurrentPrice(anyString())).thenReturn(BigDecimal.valueOf(160.00));

        // When
        Optional<Holding> result = holdingService.getHoldingById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        verify(holdingRepository, times(1)).findById(1L);
    }

    @Test
    void testGetHoldingByIdNotFound() {
        // Given
        when(holdingRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Holding> result = holdingService.getHoldingById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(holdingRepository, times(1)).findById(999L);
    }

    @Test
    void testCreateHolding() {
        // Given
        when(holdingRepository.save(any(Holding.class))).thenReturn(testHolding);
        when(yahooFinanceService.getCurrentPrice(anyString())).thenReturn(BigDecimal.valueOf(160.00));

        // When
        Holding result = holdingService.createHolding(testHolding);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getAssetType()).isEqualTo("STOCK"); // Should be normalized to uppercase
        verify(holdingRepository, times(1)).save(any(Holding.class));
    }

    @Test
    void testCreateHoldingNormalizesAssetType() {
        // Given
        Holding lowercaseHolding = new Holding(
                "stock",
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                LocalDate.of(2025, 1, 15)
        );
        when(holdingRepository.save(any(Holding.class))).thenReturn(testHolding);
        when(yahooFinanceService.getCurrentPrice(anyString())).thenReturn(BigDecimal.valueOf(160.00));

        // When
        Holding result = holdingService.createHolding(lowercaseHolding);

        // Then
        assertThat(result.getAssetType()).isEqualTo("STOCK");
        verify(holdingRepository, times(1)).save(any(Holding.class));
    }

    @Test
    void testUpdateHolding() {
        // Given
        Holding updatedDetails = new Holding(
                "STOCK",
                "AAPL",
                new BigDecimal("20"),
                new BigDecimal("160.00"),
                LocalDate.of(2025, 1, 15)
        );

        when(holdingRepository.findById(1L)).thenReturn(Optional.of(testHolding));
        when(holdingRepository.save(any(Holding.class))).thenReturn(testHolding);
        when(yahooFinanceService.getCurrentPrice(anyString())).thenReturn(BigDecimal.valueOf(160.00));

        // When
        Optional<Holding> result = holdingService.updateHolding(1L, updatedDetails);

        // Then
        assertThat(result).isPresent();
        verify(holdingRepository, times(1)).findById(1L);
        verify(holdingRepository, times(1)).save(any(Holding.class));
    }

    @Test
    void testUpdateHoldingNotFound() {
        // Given
        Holding updatedDetails = new Holding(
                "STOCK",
                "AAPL",
                new BigDecimal("20"),
                new BigDecimal("160.00"),
                LocalDate.of(2025, 1, 15)
        );
        when(holdingRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Holding> result = holdingService.updateHolding(999L, updatedDetails);

        // Then
        assertThat(result).isEmpty();
        verify(holdingRepository, times(1)).findById(999L);
        verify(holdingRepository, never()).save(any(Holding.class));
    }

    @Test
    void testDeleteHolding() {
        // Given
        when(holdingRepository.existsById(1L)).thenReturn(true);
        doNothing().when(holdingRepository).deleteById(1L);

        // When
        boolean result = holdingService.deleteHolding(1L);

        // Then
        assertThat(result).isTrue();
        verify(holdingRepository, times(1)).existsById(1L);
        verify(holdingRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteHoldingNotFound() {
        // Given
        when(holdingRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = holdingService.deleteHolding(999L);

        // Then
        assertThat(result).isFalse();
        verify(holdingRepository, times(1)).existsById(999L);
        verify(holdingRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetHoldingsByAssetType() {
        // Given
        List<Holding> holdings = new ArrayList<>();
        holdings.add(testHolding);
        when(holdingRepository.findByAssetType("STOCK")).thenReturn(holdings);
        when(yahooFinanceService.getCurrentPrice(anyString())).thenReturn(BigDecimal.valueOf(160.00));

        // When
        List<Holding> result = holdingService.getHoldingsByAssetType("STOCK");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssetType()).isEqualTo("STOCK");
        verify(holdingRepository, times(1)).findByAssetType("STOCK");
    }

    @Test
    void testGetHoldingsByAssetTypeEmpty() {
        // Given
        when(holdingRepository.findByAssetType("CRYPTO")).thenReturn(new ArrayList<>());

        // When
        List<Holding> result = holdingService.getHoldingsByAssetType("CRYPTO");

        // Then
        assertThat(result).isEmpty();
        verify(holdingRepository, times(1)).findByAssetType("CRYPTO");
    }
}
