package org.jdbc.portfoliomanagement.repository;


import org.jdbc.portfoliomanagement.entity.HistoricalPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HistoricalPriceRepository extends JpaRepository<HistoricalPrice, Long> {

    // Find all historical prices for a symbol
    List<HistoricalPrice> findBySymbolOrderByPriceDateAsc(String symbol);

    // Find historical prices for a symbol within date range
    List<HistoricalPrice> findBySymbolAndPriceDateBetweenOrderByPriceDateAsc(
            String symbol, LocalDate startDate, LocalDate endDate);
}
