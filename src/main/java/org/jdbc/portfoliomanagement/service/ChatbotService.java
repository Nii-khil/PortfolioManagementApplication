package org.jdbc.portfoliomanagement.service;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class ChatbotService {

    @Autowired
    private HoldingService holdingService;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final WebClient webClient;

    public ChatbotService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public Map<String, Object> handleChatbotQuery(String query) {
        Map<String, Object> response = new HashMap<>();

        try {
            String dataContext = prepareDataContext(query);

            String aiResponse = callGeminiAPI(query, dataContext);

            response.put("success", true);
            response.put("response", aiResponse);
            response.put("query", query);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("response", "I'm having trouble processing your request. Please try again.");
        }

        return response;
    }

    private String prepareDataContext(String query) {
        StringBuilder context = new StringBuilder();

        try {
            Map<String, Object> summary = holdingService.getPortfolioSummary();
            List<Holding> holdings = holdingService.getAllHoldings();

            context.append("Portfolio Data:\n");
            context.append("Total Holdings: ").append(holdings.size()).append("\n");

            if (summary.containsKey("totalInvestment")) {
                context.append("Total Investment: ₹").append(summary.get("totalInvestment")).append("\n");
            }
            if (summary.containsKey("currentValue")) {
                context.append("Current Value: ₹").append(summary.get("currentValue")).append("\n");
            }
            if (summary.containsKey("totalProfitLoss")) {
                context.append("Total Profit/Loss: ₹").append(summary.get("totalProfitLoss")).append("\n");
            }
            if (summary.containsKey("totalProfitLossPercentage")) {
                context.append("Total P/L %: ").append(summary.get("totalProfitLossPercentage")).append("%\n");
            }

            if (!holdings.isEmpty()) {
                context.append("\nHoldings:\n");
                for (Holding holding : holdings) {
                    context.append("- ").append(holding.getSymbol())
                            .append(" (").append(holding.getAssetType()).append(")")
                            .append(": Qty ").append(holding.getQuantity())
                            .append(", Purchase: ₹").append(holding.getPurchasePrice());

                    if (holding.getCurrentPrice() != null) {
                        context.append(", Current: ₹").append(holding.getCurrentPrice());
                    }
                    if (holding.getProfitLoss() != null) {
                        context.append(", P/L: ₹").append(holding.getProfitLoss());
                    }
                    context.append("\n");
                }
            }

            Holding best = holdingService.getBestPerformer();
            Holding worst = holdingService.getWorstPerformer();

            if (best != null) {
                context.append("\nBest Performer: ").append(best.getSymbol())
                        .append(" (").append(best.getProfitLossPercentage()).append("%)\n");
            }
            if (worst != null) {
                context.append("Worst Performer: ").append(worst.getSymbol())
                        .append(" (").append(worst.getProfitLossPercentage()).append("%)\n");
            }

        } catch (Exception e) {
            context.append("Error gathering portfolio data: ").append(e.getMessage());
        }

        return context.toString();
    }

    private String callGeminiAPI(String query, String dataContext) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            return generateFallbackResponse(query, dataContext);
        }

        try {
            String prompt = String.format(
                "You are a helpful financial portfolio assistant. Answer the user's question based on their portfolio data.\n\n" +
                "User Question: %s\n\n" +
                "%s\n\n" +
                "Provide a clear, concise, and friendly response. Use emojis where appropriate. " +
                "Format currency values in Indian Rupees (₹). Keep the response under 200 words.",
                query, dataContext
            );

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            parts.put("text", prompt);
            contents.put("parts", Collections.singletonList(parts));
            requestBody.put("contents", Collections.singletonList(contents));

            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/gemini-pro:generateContent")
                            .queryParam("key", geminiApiKey)
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> responseParts = (List<Map<String, Object>>) content.get("parts");
                    if (!responseParts.isEmpty()) {
                        return (String) responseParts.get(0).get("text");
                    }
                }
            }

            return generateFallbackResponse(query, dataContext);

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return generateFallbackResponse(query, dataContext);
        }
    }

    private String generateFallbackResponse(String query, String dataContext) {
        Map<String, String> metrics = parseDataContext(dataContext);

        String queryLower = query.toLowerCase();

        if (queryLower.contains("performing today") || queryLower.contains("performance")) {
            String totalPL = metrics.getOrDefault("totalProfitLoss", "N/A");
            String plPercent = metrics.getOrDefault("totalProfitLossPercentage", "N/A");
            String best = metrics.getOrDefault("bestPerformer", "N/A");
            String worst = metrics.getOrDefault("worstPerformer", "N/A");

            return String.format(
                "📊 **Portfolio Performance Update**\n\n" +
                "Your portfolio is currently showing a %s return of ₹%s (%s%%).\n\n" +
                "🌟 Best Performer: %s\n" +
                "📉 Needs Attention: %s\n\n" +
                "%s",
                plPercent.startsWith("-") ? "loss" : "gain",
                totalPL,
                plPercent,
                best,
                worst,
                plPercent.startsWith("-") ?
                    "Don't worry! Market fluctuations are normal. Consider reviewing your diversification strategy." :
                    "Great job! Your portfolio is showing positive returns. Keep monitoring your investments regularly."
            );
        } else if (queryLower.contains("total") && queryLower.contains("value")) {
            String currentValue = metrics.getOrDefault("currentValue", "N/A");
            String totalInvestment = metrics.getOrDefault("totalInvestment", "N/A");

            return String.format(
                "💰 **Portfolio Value Summary**\n\n" +
                "Current Portfolio Value: ₹%s\n" +
                "Total Investment: ₹%s\n\n" +
                "Your portfolio value represents the current market worth of all your holdings combined. " +
                "This includes stocks, mutual funds, and other assets you've invested in.",
                currentValue,
                totalInvestment
            );
        } else if (queryLower.contains("profit") || queryLower.contains("loss")) {
            String totalPL = metrics.getOrDefault("totalProfitLoss", "N/A");
            String plPercent = metrics.getOrDefault("totalProfitLossPercentage", "N/A");
            String currentValue = metrics.getOrDefault("currentValue", "N/A");
            String totalInvestment = metrics.getOrDefault("totalInvestment", "N/A");

            boolean isProfit = !plPercent.startsWith("-");

            return String.format(
                "📈 **Profit/Loss Analysis**\n\n" +
                "Total %s: ₹%s (%s%%)\n\n" +
                "Investment: ₹%s\n" +
                "Current Value: ₹%s\n\n" +
                "%s",
                isProfit ? "Profit" : "Loss",
                totalPL,
                plPercent,
                totalInvestment,
                currentValue,
                isProfit ?
                    "🎉 Excellent! Your investments are generating positive returns. Consider reinvesting your profits for compound growth." :
                    "📊 Your portfolio is currently in the red. This is temporary - stay focused on your long-term investment goals."
            );
        } else if (queryLower.contains("summary")) {
            String totalHoldings = metrics.getOrDefault("totalHoldings", "N/A");
            String currentValue = metrics.getOrDefault("currentValue", "N/A");
            String totalPL = metrics.getOrDefault("totalProfitLoss", "N/A");
            String plPercent = metrics.getOrDefault("totalProfitLossPercentage", "N/A");

            return String.format(
                "🧾 **Portfolio Summary**\n\n" +
                "📊 Total Holdings: %s\n" +
                "💰 Current Value: ₹%s\n" +
                "📈 Profit/Loss: ₹%s (%s%%)\n\n" +
                "Your portfolio consists of various assets across different categories. " +
                "Regular monitoring and rebalancing can help optimize your returns.",
                totalHoldings,
                currentValue,
                totalPL,
                plPercent
            );
        } else if (queryLower.contains("assets") || queryLower.contains("hold")) {
            String totalHoldings = metrics.getOrDefault("totalHoldings", "N/A");
            String holdingsList = metrics.getOrDefault("holdings", "No holdings available");

            return String.format(
                "📋 **Your Current Holdings**\n\n" +
                "You currently hold %s assets:\n\n%s\n\n" +
                "Each holding contributes to your overall portfolio diversification. " +
                "Consider reviewing your asset allocation periodically to maintain a balanced portfolio.",
                totalHoldings,
                holdingsList
            );
        }

        return "I'm here to help you with your portfolio! You can ask me about:\n\n" +
               "📊 How your portfolio is performing\n" +
               "💰 Your total portfolio value\n" +
               "📈 Your profit or loss\n" +
               "🧾 Portfolio summary\n" +
               "📋 Your current holdings\n\n" +
               "What would you like to know?";
    }

    private Map<String, String> parseDataContext(String dataContext) {
        Map<String, String> metrics = new HashMap<>();

        String[] lines = dataContext.split("\n");
        for (String line : lines) {
            if (line.contains("Total Holdings:")) {
                metrics.put("totalHoldings", line.split(":")[1].trim());
            } else if (line.contains("Total Investment:")) {
                metrics.put("totalInvestment", line.split("₹")[1].trim());
            } else if (line.contains("Current Value:")) {
                metrics.put("currentValue", line.split("₹")[1].trim());
            } else if (line.contains("Total Profit/Loss:")) {
                metrics.put("totalProfitLoss", line.split("₹")[1].trim());
            } else if (line.contains("Total P/L %:")) {
                String plPercent = line.split(":")[1].replace("%", "").trim();
                metrics.put("totalProfitLossPercentage", plPercent);
            } else if (line.contains("Best Performer:")) {
                metrics.put("bestPerformer", line.split(":")[1].trim());
            } else if (line.contains("Worst Performer:")) {
                metrics.put("worstPerformer", line.split(":")[1].trim());
            }
        }

        if (dataContext.contains("Holdings:")) {
            String holdingsSection = dataContext.substring(dataContext.indexOf("Holdings:") + 9);
            String[] holdingLines = holdingsSection.split("\n");
            StringBuilder holdings = new StringBuilder();
            int count = 0;
            for (String line : holdingLines) {
                if (line.trim().startsWith("-") && count < 5) { // Limit to 5 holdings
                    holdings.append(line.trim()).append("\n");
                    count++;
                }
            }
            if (holdings.length() > 0) {
                metrics.put("holdings", holdings.toString().trim());
            }
        }

        return metrics;
    }

    public List<String> getPredefinedQuestions() {
        return Arrays.asList(
            "How is my portfolio performing today?",
            "How much profit or loss am I currently in?",
            "Show my portfolio summary",
            "What assets do I currently hold?"
        );
    }
}