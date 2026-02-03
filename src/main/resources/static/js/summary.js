// Portfolio Summary JavaScript

let currentHistoricalChart = null;

// Load summary when page loads
document.addEventListener('DOMContentLoaded', () => {
    loadSummary();
    loadDiversificationSuggestions();
    populateStockSelector();
});

async function loadSummary() {
    const loadingElement = document.getElementById('loading');
    const errorElement = document.getElementById('error');
    const summaryContainer = document.getElementById('summary-container');

    try {
        // Show loading
        loadingElement.style.display = 'block';
        errorElement.style.display = 'none';
        summaryContainer.style.display = 'none';

        // Fetch summary data
        const summary = await portfolioAPI.getSummary();

        // Hide loading
        loadingElement.style.display = 'none';

        // Display summary
        displaySummaryMetrics(summary);
        displayComposition(summary.compositionByAssetType);
        await loadPerformers();

        summaryContainer.style.display = 'block';

    } catch (error) {
        console.error('Error loading summary:', error);
        loadingElement.style.display = 'none';
        errorElement.textContent = 'Failed to load portfolio summary. Please make sure the backend server is running.';
        errorElement.style.display = 'block';
    }
}

function displaySummaryMetrics(summary) {
    // Total Value (in INR)
    document.getElementById('total-value').textContent = formatCurrencyInr(summary.totalValue);

    // Total Investment (in INR)
    document.getElementById('total-investment').textContent = formatCurrencyInr(summary.totalInvestment);

    // Total P/L (in INR)
    const totalPLElement = document.getElementById('total-pl');
    totalPLElement.textContent = formatCurrencyInr(summary.totalProfitLoss);
    totalPLElement.className = 'metric-value ' + (summary.totalProfitLoss >= 0 ? 'profit' : 'loss');

    // Total P/L Percentage
    const totalPLPctElement = document.getElementById('total-pl-pct');
    totalPLPctElement.textContent = formatPercentage(summary.totalProfitLossPercentage);
    totalPLPctElement.className = 'metric-value ' + (summary.totalProfitLossPercentage >= 0 ? 'profit' : 'loss');

    // Display exchange rate info if available
    if (summary.exchangeRate) {
        console.log(`💱 Exchange Rate: 1 USD = ₹${summary.exchangeRate}`);
    }
}

function displayComposition(composition) {
    if (!composition || Object.keys(composition).length === 0) {
        document.getElementById('composition-container').innerHTML = '<p>No composition data available</p>';
        return;
    }

    // Create canvas for pie chart
    const canvas = document.getElementById('composition-chart');
    const ctx = canvas.getContext('2d');

    // Set canvas size
    canvas.width = 300;
    canvas.height = 300;

    // Calculate total
    const total = Object.values(composition).reduce((sum, val) => sum + val, 0);

    // Colors for different asset types
    const colors = {
        'STOCK': '#667eea',
        'MUTUAL_FUND': '#48bb78',
        'CASH': '#ed8936',
        'default': '#a0aec0'
    };

    // Draw pie chart
    let currentAngle = -Math.PI / 2; // Start from top

    const entries = Object.entries(composition);
    entries.forEach(([assetType, value]) => {
        const sliceAngle = (value / total) * 2 * Math.PI;

        // Draw slice
        ctx.beginPath();
        ctx.arc(150, 150, 120, currentAngle, currentAngle + sliceAngle);
        ctx.lineTo(150, 150);
        ctx.fillStyle = colors[assetType] || colors.default;
        ctx.fill();
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 2;
        ctx.stroke();

        currentAngle += sliceAngle;
    });

    // Create legend
    const legendContainer = document.getElementById('composition-legend');
    legendContainer.innerHTML = '';

    entries.forEach(([assetType, value]) => {
        const percentage = ((value / total) * 100).toFixed(1);
        const legendItem = document.createElement('div');
        legendItem.className = 'legend-item';
        legendItem.innerHTML = `
            <div class="legend-color" style="background-color: ${colors[assetType] || colors.default}"></div>
            <span><strong>${assetType}:</strong> ${formatCurrencyInr(value)} (${percentage}%)</span>
        `;
        legendContainer.appendChild(legendItem);
    });
}

async function loadPerformers() {
    try {
        // Load best performer
        const bestPerformer = await portfolioAPI.getBestPerformer();
        displayPerformer('best-performer', bestPerformer);

        // Load worst performer
        const worstPerformer = await portfolioAPI.getWorstPerformer();
        displayPerformer('worst-performer', worstPerformer);

    } catch (error) {
        console.error('Error loading performers:', error);
        document.getElementById('best-performer').innerHTML = '<p>No data available</p>';
        document.getElementById('worst-performer').innerHTML = '<p>No data available</p>';
    }
}

function displayPerformer(elementId, holding) {
    const element = document.getElementById(elementId);

    if (!holding) {
        element.innerHTML = '<p>No holdings available</p>';
        return;
    }

    const plClass = holding.profitLoss >= 0 ? 'profit' : 'loss';

    element.innerHTML = `
        <div style="margin-top: 10px;">
            <p><strong>Symbol:</strong> ${holding.symbol}</p>
            <p><strong>Asset Type:</strong> ${holding.assetType}</p>
            <p><strong>Current Value:</strong> ${formatCurrency(holding.currentValue, holding.assetType)}</p>
            <p class="${plClass}"><strong>P/L:</strong> ${formatCurrency(holding.profitLoss, holding.assetType)} (${formatPercentage(holding.profitLossPercentage)})</p>
        </div>
    `;
}

async function loadDiversificationSuggestions() {
    const container = document.getElementById('diversification-content');
    const loading = document.getElementById('diversification-loading');

    try {
        loading.style.display = 'block';

        const response = await fetch('http://localhost:8081/api/portfolio/diversification');
        const data = await response.json();

        loading.style.display = 'none';

        // Display risk level badge
        let riskBadgeClass = 'risk-moderate';
        if (data.riskLevel === 'Low') riskBadgeClass = 'risk-low';
        else if (data.riskLevel === 'High') riskBadgeClass = 'risk-high';
        else if (data.riskLevel === 'Very High') riskBadgeClass = 'risk-very-high';

        let html = `
            <div class="risk-badge ${riskBadgeClass}">
                Risk Level: ${data.riskLevel}
            </div>
            <ul class="recommendations-list">
        `;

        if (data.recommendations && data.recommendations.length > 0) {
            data.recommendations.forEach(rec => {
                html += `<li>${rec}</li>`;
            });
        } else {
            html += '<li>Your portfolio appears well-diversified!</li>';
        }

        html += '</ul>';
        container.innerHTML = html;

    } catch (error) {
        console.error('Error loading diversification suggestions:', error);
        loading.style.display = 'none';
        container.innerHTML = '<p>Unable to load diversification analysis. Please try again.</p>';
    }
}

async function populateStockSelector() {
    try {
        const holdings = await holdingsAPI.getAll();
        const assetSelector = document.getElementById('asset-selector');

        // Filter stocks and mutual funds
        const assets = holdings.filter(h => h.assetType === 'STOCK' || h.assetType === 'MUTUAL_FUND');

        if (assets.length === 0) {
            document.getElementById('no-historical-data').textContent = 'No stocks or mutual funds in portfolio yet.';
            document.getElementById('no-historical-data').style.display = 'block';
            return;
        }

        // Clear existing options except first
        assetSelector.innerHTML = '<option value="">-- Select an asset --</option>';

        // Add asset options with asset type for identification
        assets.forEach(asset => {
            const option = document.createElement('option');
            option.value = asset.symbol;
            option.setAttribute('data-asset-type', asset.assetType);
            const displayType = asset.assetType === 'STOCK' ? 'Stock' : 'MF';
            option.textContent = `${asset.symbol} (${displayType})`;
            assetSelector.appendChild(option);
        });

    } catch (error) {
        console.error('Error populating asset selector:', error);
    }
}

async function loadHistoricalChart() {
    const assetSelector = document.getElementById('asset-selector');
    const symbol = assetSelector.value;
    const chartContainer = document.getElementById('historical-chart-container');
    const noDataMsg = document.getElementById('no-historical-data');

    if (!symbol) {
        noDataMsg.textContent = 'Select a stock or mutual fund to view its price history';
        noDataMsg.style.display = 'block';
        chartContainer.style.display = 'none';
        return;
    }

    // Get asset type from the selected option
    const selectedOption = assetSelector.options[assetSelector.selectedIndex];
    const assetType = selectedOption.getAttribute('data-asset-type');

    try {
        // Show loading message
        noDataMsg.textContent = 'Loading historical data...';
        noDataMsg.style.display = 'block';
        chartContainer.style.display = 'none';

        // Fetch historical data
        const response = await fetch(`http://localhost:8081/api/historical/${symbol}`);

        if (!response.ok) {
            throw new Error('Failed to fetch historical data');
        }

        const historicalData = await response.json();

        if (!historicalData || historicalData.length === 0) {
            // No data in database, trigger fetch from API
            console.log(`No historical data found for ${symbol}, fetching from API...`);

            // Trigger API fetch
            const fetchResponse = await fetch(
                `http://localhost:8081/api/historical/fetch?symbol=${symbol}&assetType=${assetType}`,
                { method: 'POST' }
            );

            if (!fetchResponse.ok) {
                throw new Error('Failed to fetch data from external API');
            }

            // Wait a bit and retry getting the data
            await new Promise(resolve => setTimeout(resolve, 2000));
            const retryResponse = await fetch(`http://localhost:8081/api/historical/${symbol}`);
            const retryData = await retryResponse.json();

            if (!retryData || retryData.length === 0) {
                noDataMsg.innerHTML = `
                    <strong>⚠️ No historical data available for ${symbol}</strong><br>
                    The data was requested but nothing was returned.<br>
                    This could mean:<br>
                    • API rate limit exceeded (25 requests/day)<br>
                    • Invalid symbol<br>
                    • API service issues<br><br>
                    <em>Try testing with a mutual fund instead - they have unlimited API access!</em>
                `;
                noDataMsg.style.display = 'block';
                chartContainer.style.display = 'none';
                return;
            }

            displayHistoricalChart(retryData, symbol, assetType);
        } else {
            displayHistoricalChart(historicalData, symbol, assetType);
        }

    } catch (error) {
        console.error('Error loading historical data:', error);
        noDataMsg.innerHTML = `
            <strong>⚠️ Error loading historical data</strong><br>
            ${error.message}<br><br>
            <em>Common causes:</em><br>
            • Alpha Vantage API rate limit (25 requests/day)<br>
            • Network connectivity issues<br>
            • Backend server not running<br><br>
            <strong>Solution:</strong> Try selecting a mutual fund instead - they have unlimited API access!
        `;
        noDataMsg.style.display = 'block';
        chartContainer.style.display = 'none';
    }
}

/**
 * Display the historical chart with data
 */
function displayHistoricalChart(historicalData, symbol, assetType) {
    const chartContainer = document.getElementById('historical-chart-container');
    const noDataMsg = document.getElementById('no-historical-data');

    noDataMsg.style.display = 'none';
    chartContainer.style.display = 'block';

    // Prepare data for chart
    const dates = historicalData.map(item => {
        const date = new Date(item.priceDate);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    });
    const prices = historicalData.map(item => item.price);

    // Determine label based on asset type
    const priceLabel = assetType === 'MUTUAL_FUND' ? 'NAV' : 'Price';

    // Draw line chart
    drawHistoricalChart(dates, prices, symbol, priceLabel);
}

/**
 * Draw historical price line chart
 */
function drawHistoricalChart(dates, prices, symbol, priceLabel = 'Price') {
    const canvas = document.getElementById('historical-chart');
    const ctx = canvas.getContext('2d');

    // Clear previous chart
    if (currentHistoricalChart) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }

    // Set canvas size
    canvas.width = canvas.offsetWidth;
    canvas.height = 300;

    const width = canvas.width;
    const height = canvas.height;
    const padding = 50;
    const chartWidth = width - 2 * padding;
    const chartHeight = height - 2 * padding;

    // Find min and max prices
    const minPrice = Math.min(...prices);
    const maxPrice = Math.max(...prices);
    const priceRange = maxPrice - minPrice;

    // Draw axes
    ctx.strokeStyle = '#e2e8f0';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(padding, padding);
    ctx.lineTo(padding, height - padding);
    ctx.lineTo(width - padding, height - padding);
    ctx.stroke();

    // Draw grid lines
    ctx.strokeStyle = '#f7fafc';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 5; i++) {
        const y = padding + (chartHeight / 5) * i;
        ctx.beginPath();
        ctx.moveTo(padding, y);
        ctx.lineTo(width - padding, y);
        ctx.stroke();

        // Price labels
        const price = maxPrice - (priceRange / 5) * i;
        ctx.fillStyle = '#718096';
        ctx.font = '12px Arial';
        ctx.textAlign = 'right';
        // Use INR symbol consistently for all assets
        ctx.fillText('₹' + price.toFixed(2), padding - 10, y + 4);
    }

    // Draw line
    ctx.strokeStyle = '#667eea';
    ctx.lineWidth = 3;
    ctx.beginPath();

    prices.forEach((price, index) => {
        const x = padding + (chartWidth / (prices.length - 1)) * index;
        const y = height - padding - ((price - minPrice) / priceRange) * chartHeight;

        if (index === 0) {
            ctx.moveTo(x, y);
        } else {
            ctx.lineTo(x, y);
        }
    });

    ctx.stroke();

    // Draw points
    prices.forEach((price, index) => {
        const x = padding + (chartWidth / (prices.length - 1)) * index;
        const y = height - padding - ((price - minPrice) / priceRange) * chartHeight;

        ctx.fillStyle = '#667eea';
        ctx.beginPath();
        ctx.arc(x, y, 4, 0, Math.PI * 2);
        ctx.fill();
    });

    // Title
    ctx.fillStyle = '#2d3748';
    ctx.font = 'bold 16px Arial';
    ctx.textAlign = 'center';
    const historyLabel = priceLabel === 'NAV' ? 'NAV History' : 'Price History';
    ctx.fillText(`${symbol} - 30 Day ${historyLabel}`, width / 2, 30);

    // Show some date labels (every 5th point)
    ctx.fillStyle = '#718096';
    ctx.font = '11px Arial';
    ctx.textAlign = 'center';
    for (let i = 0; i < dates.length; i += 5) {
        const x = padding + (chartWidth / (prices.length - 1)) * i;
        ctx.fillText(dates[i], x, height - padding + 20);
    }

    currentHistoricalChart = true;
}