package org.jdbc.portfoliomanagement.service;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PortfolioService {

    @Autowired
    private HoldingService holdingService;

    @Autowired
    private CurrencyConversionService currencyConversionService;

    public Map<String, Object> getPortfolioSummary() {
        List<Holding> holdings = holdingService.getAllHoldings();

        Map<String, Object> summary = new HashMap<>();

        if(holdings.isEmpty()) {
            summary.put("totalValue", BigDecimal.ZERO);
            summary.put("totalInvestment", BigDecimal.ZERO);
            summary.put("totalProfitLoss", BigDecimal.ZERO);
            summary.put("totalProfitLossPercentage", BigDecimal.ZERO);
            summary.put("totalHoldings", 0);
            summary.put("compositionByAssetType", new HashMap<>());
            summary.put("compositionByCategory", new HashMap<>());
            summary.put("currency", "INR");
            summary.put("currencySymbol", "₹");
            summary.put("exchangeRate", currencyConversionService.getUsdToInrRate());
            return summary;
        }

        BigDecimal totalCurrentValueInr = BigDecimal.ZERO;
        BigDecimal totalInvestmentInr = BigDecimal.ZERO;
        Map<String, BigDecimal> assetCompositionMap = new HashMap<>();
        Map<String, BigDecimal> categoryCompositionMap = new HashMap<>();

        for(Holding holding : holdings) {
            totalCurrentValueInr = totalCurrentValueInr.add(holding.getCurrentValueInr());

            BigDecimal purchaseValue = holding.getPurchasePrice().multiply(holding.getQuantity());
            BigDecimal purchaseValueInr = currencyConversionService.convertToInr(purchaseValue, holding.getAssetType());
            totalInvestmentInr = totalCurrentValueInr.add(purchaseValueInr);

            String assetType = holding.getAssetType();
            BigDecimal currentAssetValue = assetCompositionMap.getOrDefault(assetType, BigDecimal.ZERO);
            assetCompositionMap.put(assetType, currentAssetValue.add(holding.getCurrentValueInr()));

            if(holding.getCategory() != null && !holding.getCategory().isEmpty()) {
                String category = holding.getCategory();
                BigDecimal currentCategoryValue = categoryCompositionMap.getOrDefault(category, BigDecimal.ZERO);
                categoryCompositionMap.put(category, currentCategoryValue.add(holding.getCurrentValueInr()));
            }
        }

        BigDecimal totalProfitLoss = totalCurrentValueInr.subtract(totalInvestmentInr);

        BigDecimal profitLossPercentage = BigDecimal.ZERO;
        if(totalInvestmentInr.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercentage = totalProfitLoss.divide(totalInvestmentInr, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        summary.put("totalValue", totalCurrentValueInr.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalInvestment", totalInvestmentInr.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalProfitLoss", totalProfitLoss.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalProfitLossPercentage", profitLossPercentage.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalHoldings", holdings.size());
        summary.put("currency", "INR");
        summary.put("currencySymbol", "₹");
        summary.put("exchangeRate", currencyConversionService.getUsdToInrRate());

        Map<String, BigDecimal> roundedAssetComposition = new HashMap<>();
        assetCompositionMap.forEach((key, value) ->
            roundedAssetComposition.put(key, value.setScale(2, RoundingMode.HALF_UP))
        );
        summary.put("compositionByAssetType", roundedAssetComposition);

        Map<String, BigDecimal> roundedCategoryComposition = new HashMap<>();
        categoryCompositionMap.forEach((key, value) ->
            roundedCategoryComposition.put(key, value.setScale(2, RoundingMode.HALF_UP))
        );
        summary.put("compositionByCategory", roundedCategoryComposition);

        return summary;
    }

    public Holding getBestPerformer() {
        List<Holding> holdings = holdingService.getAllHoldings();
        return holdings.stream()
                .max((h1, h2) -> h1.getProfitLossPercentage().compareTo(h2.getProfitLossPercentage())).orElse(null);
    }

    public Holding getWorstPerformer() {
        List<Holding> holdings = holdingService.getAllHoldings();
        return holdings.stream()
                .min((h1, h2) -> h1.getProfitLossPercentage().compareTo(h2.getProfitLossPercentage())).orElse(null);
    }

    public Map<String, Object> getDiversificationSuggestions() {
        List<Holding> holdings = holdingService.getAllHoldings();
        Map<String, Object> suggestions = new HashMap<>();
        List<String> recommendationsList = new ArrayList<>();

        if(holdings.isEmpty()) {
            suggestions.put("needsDiversification", false);
            suggestions.put("recommendations", recommendationsList);
            suggestions.put("riskLevel", "N/A");
            return suggestions;
        }

        BigDecimal tempTotalStockValue = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryValues = new HashMap<>();

        for(Holding holding : holdings) {
            if("STOCK".equals(holding.getAssetType()) && holding.getCategory() != null) {
                tempTotalStockValue = tempTotalStockValue.add(holding.getCurrentValue());
                String category = holding.getCategory();
                BigDecimal currentValue = categoryValues.getOrDefault(category, BigDecimal.ZERO);
                categoryValues.put(category, currentValue.add(holding.getCurrentValue()));
            }
        }

        final BigDecimal totalStockValue = tempTotalStockValue;

        if(totalStockValue.compareTo(BigDecimal.ZERO) == 0) {
            suggestions.put("needsDiversification", false);
            suggestions.put("recommendations", recommendationsList);
            suggestions.put("riskLevel", "Low");
            return suggestions;
        }

        Map<String, BigDecimal> categoryPercentages = new HashMap<>();
        categoryValues.forEach((category, value) -> {
            BigDecimal percentage = value.divide(totalStockValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            categoryPercentages.put(category, percentage);
        });

        boolean needsDiversification = false;
        final String[] riskLevel = {"Moderate"};

        for(Map.Entry<String, BigDecimal> entry : categoryPercentages.entrySet()) {
            String category = entry.getKey();
            BigDecimal percentage = entry.getValue();

            if(percentage.compareTo(new BigDecimal(40)) > 0) {
                needsDiversification = true;
                riskLevel[0] = "High";
                recommendationsList.add(String.format("High concentration in %s (%.2f%%). Consider diversifying into other sectors.", category, percentage.doubleValue()));
            } else if (percentage.compareTo(new BigDecimal(30)) > 0) {
                recommendationsList.add(String.format("Moderate concentration in %s (%.2f%%). Monitor and consider diversification if it increases.", category, percentage.doubleValue()));
            }
        }

        Set<String> importantCategories = new HashSet<>(Arrays.asList("Technology", "Healthcare", "Finance", "Consumer Goods", "Energy"));
        Set<String> missingCategories = new HashSet<>(importantCategories);
        missingCategories.removeAll(categoryValues.keySet());

        if(!missingCategories.isEmpty() && categoryValues.size() < 3) {
            needsDiversification = true;
            for(String missingCategory : missingCategories) {
                recommendationsList.add(String.format("Consider adding holdings in the %s sector for better diversification.", missingCategory));
            }
        }

        if(categoryValues.size() == 1) {
            riskLevel[0] = "Very High";
            needsDiversification = true;
            recommendationsList.add("Portfolio is concentrated in a single category. Consider diversifying into multiple sectors.");
        } else if (categoryValues.size() == 2) {
            riskLevel[0] = "High";
            recommendationsList.add("Portfolio has limited category diversification. Consider adding more sectors.");
        } else if (categoryValues.size() >= 5) {
            riskLevel[0] = "Low";
            recommendationsList.add("Good diversification across multiple sectors!");
        }

        Map<String, Object> summary = getPortfolioSummary();
        Map<String, BigDecimal> assetComposition = (Map<String, BigDecimal>) summary.get("compositionByAssetType");
        BigDecimal totalValue = (BigDecimal) summary.get("totalValue");

        if(assetComposition.containsKey("STOCK")) {
            BigDecimal stockPercentage = assetComposition.get("STOCK")
                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if(stockPercentage.compareTo(new BigDecimal("80")) > 0) {
                recommendationsList.add(String.format("High allocation to stocks (%.2f%%). Consider balancing with other asset types.", stockPercentage.doubleValue()));
            } else if (stockPercentage.compareTo(new BigDecimal("90")) > 0) {
                riskLevel[0] = "Very High";
                needsDiversification = true;
                recommendationsList.add(String.format("Very high allocation to stocks (%.2f%%). This increases risk significantly.", stockPercentage.doubleValue()));
            }
        }
        if(recommendationsList.isEmpty()) {
            recommendationsList.add("Your portfolio is well diversified!");
        }

        suggestions.put("needsDiversification", needsDiversification);
        suggestions.put("recommendations", recommendationsList);
        suggestions.put("riskLevel", riskLevel[0]);
        suggestions.put("categoryBreakdown", categoryPercentages);

        return suggestions;
    }
}
