package org.jdbc.portfoliomanagement.service;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.jdbc.portfoliomanagement.repository.HoldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class HoldingService {
    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private PriceService priceService;

    @Autowired
    private CurrencyConversionService currencyConversionService;

    public List<Holding> getAllHoldings() {
        List<Holding> holdings = holdingRepository.findAll();
        holdings.forEach(this::addCalculatedFields);
        return holdings;
    }

    public Optional<Holding> getHoldingById(Long id) {
        Optional<Holding> holdings = holdingRepository.findById(id);
        holdings.ifPresent(this::addCalculatedFields);
        return holdings;
    }

    public Holding createHolding(Holding holding) {
        Holding savedHolding = holdingRepository.save(holding);
        addCalculatedFields(savedHolding);
        return savedHolding;
    }

    public Optional<Holding> updateHolding(Long id, Holding holdingDetails) {
        return holdingRepository.findById(id)
                .map(holding -> {
                    holding.setAssetType(holdingDetails.getAssetType());
                    holding.setSymbol(holdingDetails.getSymbol());
                    holding.setQuantity(holdingDetails.getQuantity());
                    holding.setPurchasePrice(holdingDetails.getPurchasePrice());
                    holding.setPurchaseDate(holdingDetails.getPurchaseDate());
                    Holding updatedHolding = holdingRepository.save(holding);
                    addCalculatedFields(updatedHolding);
                    return updatedHolding;
        });
    }

    public boolean deleteHolding(Long id) {
        if(holdingRepository.existsById(id)) {
            holdingRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Holding> getHoldingsByAssetType(String assetType) {
        List<Holding> holdings = holdingRepository.findByAssetType(assetType);
        holdings.forEach(this::addCalculatedFields);
        return holdings;
    }

    private void addCalculatedFields(Holding holding) {
        BigDecimal currentPrice = priceService.getCurrentPrice(holding.getSymbol(), holding.getAssetType());
        holding.setCurrentPrice(currentPrice);

        BigDecimal currentValue = currentPrice.multiply(holding.getQuantity());
        holding.setCurrentValue(currentValue.setScale(2, RoundingMode.HALF_UP));

        BigDecimal purchaseValue = holding.getPurchasePrice().multiply(holding.getQuantity());
        BigDecimal profitLoss = currentValue.subtract(purchaseValue);
        holding.setProfitLoss(profitLoss.setScale(2, RoundingMode.HALF_UP));

        if(purchaseValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = profitLoss.divide(purchaseValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            holding.setProfitLossPercentage(percentage);
        } else {
            holding.setProfitLossPercentage(BigDecimal.ZERO);
        }

        String currency = currencyConversionService.getCurrencyCode(holding.getAssetType());
        String currencySymbol = currencyConversionService.getCurrencySymbol(holding.getAssetType());
        holding.setCurrency(currency);
        holding.setCurrencySymbol(currencySymbol);

        BigDecimal currentValueInr = currencyConversionService.convertToInr(currentValue, holding.getAssetType());
        BigDecimal purchaseValueInr = currencyConversionService.convertToInr(purchaseValue, holding.getAssetType());
        BigDecimal profitLossInr = currentValueInr.subtract(purchaseValueInr);

        holding.setCurrentValueInr(currentValueInr.setScale(2, RoundingMode.HALF_UP));
        holding.setProfitLossInr(profitLossInr.setScale(2, RoundingMode.HALF_UP));
    }
}
