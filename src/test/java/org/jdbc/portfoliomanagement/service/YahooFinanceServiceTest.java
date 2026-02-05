package org.jdbc.portfoliomanagement.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
@ExtendWith(MockitoExtension.class)
class YahooFinanceServiceTest {
    private YahooFinanceService yahooFinanceService;
    @BeforeEach
    void setUp() {
        yahooFinanceService = new YahooFinanceService();
    }
    @Test
    void testServiceInitialization() {
        YahooFinanceService service = new YahooFinanceService();
        assertThat(service).isNotNull();
    }
    @Test
    void testGetCurrentPriceWithInvalidSymbol() {
        String invalidSymbol = "INVALIDSYMBOL123456789";
        BigDecimal price = yahooFinanceService.getCurrentPrice(invalidSymbol);
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }
    @Test
    void testGetHistoricalDataWithInvalidSymbol() {
        String invalidSymbol = "INVALIDSYMBOL123456789";
        List<YahooFinanceService.HistoricalData> data = yahooFinanceService.getHistoricalData(invalidSymbol);
        assertThat(data).isEmpty();
    }
    @Test
    void testHistoricalDataClass() {
        BigDecimal price = new BigDecimal("150.00");
        java.time.LocalDate date = java.time.LocalDate.of(2026, 1, 1);
        String symbol = "AAPL";
        YahooFinanceService.HistoricalData data = new YahooFinanceService.HistoricalData(symbol, price, date);
        assertThat(data.getSymbol()).isEqualTo(symbol);
        assertThat(data.getPrice()).isEqualByComparingTo(price);
        assertThat(data.getDate()).isEqualTo(date);
    }
}
