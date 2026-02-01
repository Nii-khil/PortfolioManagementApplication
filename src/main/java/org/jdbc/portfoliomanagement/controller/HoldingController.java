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

@RestController
@RequestMapping("/api")
public class HoldingController {

    @Autowired
    private HoldingService holdingService;

    @GetMapping("/holdings")
    public ResponseEntity<List<Holding>> getAllHoldings() {
        List<Holding> holdings = holdingService.getAllHoldings();
        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/holdings/{id}")
    public ResponseEntity<Holding> getHoldingById(@PathVariable Long id) {
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
    public ResponseEntity<Holding> updateHolding(@PathVariable Long id, @RequestBody Holding holding) {
        return holdingService.updateHolding(id, holding)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/holdings/{id}")
    public ResponseEntity<Void> deleteHolding(@PathVariable Long id) {
        if (holdingService.deleteHolding(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/holdings/assetType/{assetType}")
    public ResponseEntity<List<Holding>> getHoldingsByAssetType(@PathVariable String assetType) {
        List<Holding> holdings = holdingService.getHoldingsByAssetType(assetType);
        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Holding Service is up and running!");
    }


    @Autowired
    private HistoricalPriceService historicalPriceService;

    @GetMapping("/historical/{symbol}")
    public ResponseEntity<List<HistoricalPrice>> getHistoricalPrices(@PathVariable String symbol) {
        List<HistoricalPrice> prices = historicalPriceService.getHistoricalPrices(symbol);
        return ResponseEntity.ok(prices);
    }

    @PostMapping("/historical/fetch")
    public ResponseEntity<List<HistoricalPrice>> fetchHistoricalData(
            @RequestParam String symbol,
            @RequestParam String assetType) {
        List<HistoricalPrice> prices = historicalPriceService.fetchAndStoreHistoricalData(symbol, assetType);
        return ResponseEntity.ok(prices);
    }
}
