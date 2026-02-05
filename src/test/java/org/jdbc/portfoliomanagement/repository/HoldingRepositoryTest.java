package org.jdbc.portfoliomanagement.repository;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class HoldingRepositoryTest {

    @Autowired
    private HoldingRepository holdingRepository;

    private Holding testHolding1;
    private Holding testHolding2;
    private Holding testHolding3;

    @BeforeEach
    void setUp() {
        holdingRepository.deleteAll();

        testHolding1 = new Holding(
                "STOCK",
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                LocalDate.of(2025, 1, 15)
        );

        testHolding2 = new Holding(
                "STOCK",
                "GOOGL",
                new BigDecimal("5"),
                new BigDecimal("2800.00"),
                LocalDate.of(2025, 2, 1)
        );

        testHolding3 = new Holding(
                "MF",
                "123456",
                new BigDecimal("100"),
                new BigDecimal("50.00"),
                LocalDate.of(2025, 1, 20)
        );
    }

    @Test
    void testSaveHolding() {
        // When
        Holding savedHolding = holdingRepository.save(testHolding1);

        // Then
        assertThat(savedHolding.getId()).isNotNull();
        assertThat(savedHolding.getSymbol()).isEqualTo("AAPL");
        assertThat(savedHolding.getAssetType()).isEqualTo("STOCK");
        assertThat(savedHolding.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void testFindById() {
        // Given
        Holding savedHolding = holdingRepository.save(testHolding1);

        // When
        Optional<Holding> foundHolding = holdingRepository.findById(savedHolding.getId());

        // Then
        assertThat(foundHolding).isPresent();
        assertThat(foundHolding.get().getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void testFindByIdNotFound() {
        // When
        Optional<Holding> foundHolding = holdingRepository.findById(999L);

        // Then
        assertThat(foundHolding).isEmpty();
    }

    @Test
    void testFindAll() {
        // Given
        holdingRepository.save(testHolding1);
        holdingRepository.save(testHolding2);
        holdingRepository.save(testHolding3);

        // When
        List<Holding> holdings = holdingRepository.findAll();

        // Then
        assertThat(holdings).hasSize(3);
    }

    @Test
    void testFindByAssetType() {
        // Given
        holdingRepository.save(testHolding1);
        holdingRepository.save(testHolding2);
        holdingRepository.save(testHolding3);

        // When
        List<Holding> stockHoldings = holdingRepository.findByAssetType("STOCK");
        List<Holding> mfHoldings = holdingRepository.findByAssetType("MF");

        // Then
        assertThat(stockHoldings).hasSize(2);
        assertThat(mfHoldings).hasSize(1);
        assertThat(stockHoldings.get(0).getAssetType()).isEqualTo("STOCK");
        assertThat(mfHoldings.get(0).getAssetType()).isEqualTo("MF");
    }

    @Test
    void testFindBySymbol() {
        // Given
        holdingRepository.save(testHolding1);
        holdingRepository.save(testHolding2);

        // When
        List<Holding> appleHoldings = holdingRepository.findBySymbol("AAPL");

        // Then
        assertThat(appleHoldings).hasSize(1);
        assertThat(appleHoldings.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void testUpdateHolding() {
        // Given
        Holding savedHolding = holdingRepository.save(testHolding1);

        // When
        savedHolding.setQuantity(new BigDecimal("20"));
        savedHolding.setPurchasePrice(new BigDecimal("160.00"));
        Holding updatedHolding = holdingRepository.save(savedHolding);

        // Then
        Optional<Holding> foundHolding = holdingRepository.findById(updatedHolding.getId());
        assertThat(foundHolding).isPresent();
        assertThat(foundHolding.get().getQuantity()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(foundHolding.get().getPurchasePrice()).isEqualByComparingTo(new BigDecimal("160.00"));
    }

    @Test
    void testDeleteHolding() {
        // Given
        Holding savedHolding = holdingRepository.save(testHolding1);
        Long holdingId = savedHolding.getId();

        // When
        holdingRepository.deleteById(holdingId);

        // Then
        Optional<Holding> deletedHolding = holdingRepository.findById(holdingId);
        assertThat(deletedHolding).isEmpty();
    }

    @Test
    void testFindPortfolioComposition() {
        // Given
        holdingRepository.save(testHolding1);
        holdingRepository.save(testHolding2);
        holdingRepository.save(testHolding3);

        // When
        List<Object[]> composition = holdingRepository.findPortfolioComposition();

        // Then
        assertThat(composition).isNotEmpty();
        assertThat(composition.size()).isGreaterThanOrEqualTo(2); // At least STOCK and MF
    }

    @Test
    void testExistsById() {
        // Given
        Holding savedHolding = holdingRepository.save(testHolding1);

        // When
        boolean exists = holdingRepository.existsById(savedHolding.getId());
        boolean notExists = holdingRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testCount() {
        // Given
        holdingRepository.save(testHolding1);
        holdingRepository.save(testHolding2);

        // When
        long count = holdingRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}
