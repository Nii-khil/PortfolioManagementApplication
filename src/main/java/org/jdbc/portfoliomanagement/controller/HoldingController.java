package org.jdbc.portfoliomanagement.controller;

import org.jdbc.portfoliomanagement.entity.HistoricalPrice;
import org.jdbc.portfoliomanagement.entity.Holding;
import org.jdbc.portfoliomanagement.service.HistoricalPriceService;
import org.jdbc.portfoliomanagement.service.HoldingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://127.0.0.1:5500/")
@RestController
@RequestMapping("/api")
public class HoldingController {

    @Autowired
    private HoldingService holdingService;

    @Autowired
    private HistoricalPriceService historicalPriceService;

    @GetMapping("/holdings")
    public ResponseEntity<List<Holding>> getAllHoldings() {
        List<Holding> holdings = holdingService.getAllHoldings();
        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/holdings/{id}")
    public ResponseEntity<Holding> getHoldingById(@PathVariable("id") Long id) {
        return holdingService.getHoldingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/holdings")
    public ResponseEntity<Holding> createHolding(@RequestBody Holding holding) {
        Holding created = holdingService.createHolding(holding);

        try {
            String Symbol = created.getSymbol();
            String assetType = created.getAssetType();

            List<HistoricalPrice> existingData = historicalPriceService.getHistoricalPrices(Symbol);
            if (existingData.isEmpty()) {
                historicalPriceService.fetchAndStoreHistoricalData(Symbol, assetType);
            } else {
                System.out.println("Historical data for symbol " + Symbol + " already exists. Skipping fetch.");
            }
        } catch(Exception e) {
            System.err.println("Error fetching historical data: " + e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/holdings/{id}")
    public ResponseEntity<Holding> updateHolding(@PathVariable("id") Long id, @RequestBody Holding holding) {
        return holdingService.updateHolding(id, holding)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/holdings/{id}")
    public ResponseEntity<Void> deleteHolding(@PathVariable("id") Long id) {
        if (holdingService.deleteHolding(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/holdings/assetType/{assetType}")
    public ResponseEntity<List<Holding>> getHoldingsByAssetType(@PathVariable("assetType") String assetType) {
        List<Holding> holdings = holdingService.getHoldingsByAssetType(assetType);
        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Holding Service is up and running!");
    }

    @GetMapping("/historical/{symbol}")
    public ResponseEntity<List<HistoricalPrice>> getHistoricalPrices(@PathVariable("symbol") String symbol) {
        List<HistoricalPrice> prices = historicalPriceService.getHistoricalPrices(symbol);
        return ResponseEntity.ok(prices);
    }

    @PostMapping("/historical/fetch")
    public ResponseEntity<List<HistoricalPrice>> fetchHistoricalData(
            @RequestParam("symbol") String symbol,
            @RequestParam("assetType") String assetType) {
        List<HistoricalPrice> prices = historicalPriceService.fetchAndStoreHistoricalData(symbol, assetType);
        return ResponseEntity.ok(prices);
    }

    @GetMapping("/portfolio/summary")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary() {
        Map<String, Object> summary = holdingService.getPortfolioSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/portfolio/best-performer")
    public ResponseEntity<Holding> getBestPerformer() {
        Holding best = holdingService.getBestPerformer();
        if(best != null) {
            return ResponseEntity.ok(best);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/portfolio/worst-performer")
    public ResponseEntity<Holding> getWorstPerformer() {
        Holding worst = holdingService.getWorstPerformer();
        if (worst != null) {
            return ResponseEntity.ok(worst);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/search/stocks")
    public ResponseEntity<Map<String, Object>> searchStocks(@RequestParam("query") String query) {
        Map<String, Object> res = holdingService.searchStocks(query);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/search/stocks/{symbol}")
    public ResponseEntity<Map<String, Object>> getStockDetails(@PathVariable("symbol") String symbol) {
        Map<String, Object> res = holdingService.getStockDetails(symbol);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/search/mutualfunds")
    public ResponseEntity<Map<String, Object>> searchMutualFunds(@RequestParam("query") String query) {
        Map<String, Object> res = holdingService.searchMutualFunds(query);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/search/mutualfunds/{schemeCode}")
    public ResponseEntity<Map<String, Object>> getMutualFundDetails(@PathVariable("schemeCode") String schemeCode) {
        Map<String, Object> res = holdingService.getMutualFundDetails(schemeCode);
        return ResponseEntity.ok(res);
    }

    @GetMapping("portfolio/diversification")
    public ResponseEntity<Map<String, Object>> getDiversificationSuggestions() {
        Map<String, Object> suggestions = holdingService.getDiversificationSuggestions();
        return ResponseEntity.ok(suggestions);
    }
}
