package org.jdbc.portfoliomanagement.repository;

import org.jdbc.portfoliomanagement.entity.HistoricalPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class HistoricalPriceRepositoryTest {

    @Autowired
    private HistoricalPriceRepository historicalPriceRepository;

    private HistoricalPrice price1;
    private HistoricalPrice price2;
    private HistoricalPrice price3;

    @BeforeEach
    void setUp() {
        historicalPriceRepository.deleteAll();

        price1 = new HistoricalPrice(
                "AAPL",
                new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 1)
        );

        price2 = new HistoricalPrice(
                "AAPL",
                new BigDecimal("155.00"),
                LocalDate.of(2026, 1, 15)
        );

        price3 = new HistoricalPrice(
                "GOOGL",
                new BigDecimal("2800.00"),
                LocalDate.of(2026, 1, 10)
        );
    }

    @Test
    void testSaveHistoricalPrice() {
        // When
        HistoricalPrice saved = historicalPriceRepository.save(price1);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void testFindBySymbolOrderByPriceDateAsc() {
        // Given
        historicalPriceRepository.save(price2); // Save in reverse order
        historicalPriceRepository.save(price1);
        historicalPriceRepository.save(price3);

        // When
        List<HistoricalPrice> applePrices = historicalPriceRepository.findBySymbolOrderByPriceDateAsc("AAPL");

        // Then
        assertThat(applePrices).hasSize(2);
        assertThat(applePrices.get(0).getPriceDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(applePrices.get(1).getPriceDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void testFindBySymbolAndPriceDateBetweenOrderByPriceDateAsc() {
        // Given
        historicalPriceRepository.save(price1);
        historicalPriceRepository.save(price2);
        historicalPriceRepository.save(price3);

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 10);

        // When
        List<HistoricalPrice> prices = historicalPriceRepository
                .findBySymbolAndPriceDateBetweenOrderByPriceDateAsc("AAPL", startDate, endDate);

        // Then
        assertThat(prices).hasSize(1);
        assertThat(prices.get(0).getPriceDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void testFindBySymbolNotFound() {
        // Given
        historicalPriceRepository.save(price1);

        // When
        List<HistoricalPrice> prices = historicalPriceRepository.findBySymbolOrderByPriceDateAsc("MSFT");

        // Then
        assertThat(prices).isEmpty();
    }

    @Test
    void testSaveAll() {
        // When
        List<HistoricalPrice> saved = historicalPriceRepository.saveAll(List.of(price1, price2, price3));

        // Then
        assertThat(saved).hasSize(3);
        assertThat(historicalPriceRepository.count()).isEqualTo(3);
    }

    @Test
    void testDeleteHistoricalPrice() {
        // Given
        HistoricalPrice saved = historicalPriceRepository.save(price1);

        // When
        historicalPriceRepository.deleteById(saved.getId());

        // Then
        assertThat(historicalPriceRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void testFindAll() {
        // Given
        historicalPriceRepository.save(price1);
        historicalPriceRepository.save(price2);
        historicalPriceRepository.save(price3);

        // When
        List<HistoricalPrice> allPrices = historicalPriceRepository.findAll();

        // Then
        assertThat(allPrices).hasSize(3);
    }

    @Test
    void testDateRangeFiltering() {
        // Given
        HistoricalPrice oldPrice = new HistoricalPrice("AAPL", new BigDecimal("100.00"), LocalDate.of(2025, 1, 1));
        HistoricalPrice recentPrice = new HistoricalPrice("AAPL", new BigDecimal("200.00"), LocalDate.of(2026, 2, 1));

        historicalPriceRepository.save(oldPrice);
        historicalPriceRepository.save(price1);
        historicalPriceRepository.save(recentPrice);

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        // When
        List<HistoricalPrice> filteredPrices = historicalPriceRepository
                .findBySymbolAndPriceDateBetweenOrderByPriceDateAsc("AAPL", startDate, endDate);

        // Then
        assertThat(filteredPrices).hasSize(1);
        assertThat(filteredPrices.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    }
}
