package org.jdbc.portfoliomanagement.repository;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByAssetType(String assetType);
    List<Holding> findBySymbol(String symbol);

    @Query("SELECT h.assetType, COUNT(h), SUM(h.quantity * h.purchasePrice) " +
            "FROM Holding h GROUP BY h.assetType")
    List<Object[]> findPortfolioComposition();
}
