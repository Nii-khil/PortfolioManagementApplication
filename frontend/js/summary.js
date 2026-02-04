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

    const selectedOption = assetSelector.options[assetSelector.selectedIndex];
    const assetType = selectedOption ? selectedOption.getAttribute('data-asset-type') : null;

    try {
        noDataMsg.textContent = 'Loading historical data...';
        noDataMsg.style.display = 'block';
        chartContainer.style.display = 'none';

        const response = await fetch(`http://localhost:8081/api/historical/${encodeURIComponent(symbol)}`);
        if (!response.ok) {
            throw new Error('Failed to fetch historical data from backend');
        }

        let historicalData = await response.json();

        // if no data present in DB, trigger fetch-from-external-API endpoint
        if (!historicalData || historicalData.length === 0) {
            console.log(`No historical data found for ${symbol} in DB, requesting from external API`);

            const fetchResponse = await fetch(
                `http://localhost:8081/api/historical/fetch?symbol=${encodeURIComponent(symbol)}&assetType=${encodeURIComponent(assetType)}`,
                { method: 'POST' }
            );

            if (!fetchResponse.ok) {
                throw new Error('Failed to request historical data from external API');
            }

            await new Promise(resolve => setTimeout(resolve, 1200));
            const retryResponse = await fetch(`http://localhost:8081/api/historical/${encodeURIComponent(symbol)}`);
            if (!retryResponse.ok) {
                throw new Error('Failed to re-fetch historical data after requesting external API');
            }
            historicalData = await retryResponse.json();
        }

        if (!historicalData || historicalData.length === 0) {
            noDataMsg.innerHTML = `\n                <strong>⚠️ No historical data available</strong>            `;
            noDataMsg.style.display = 'block';
            chartContainer.style.display = 'none';
            return;
        }

        displayHistoricalChart(historicalData, symbol, assetType);

    } catch (error) {
        console.error('Error loading historical data:', error);
        noDataMsg.innerHTML = `\n            <strong>⚠️ Error loading historical data</strong><br>\n            ${error.message}\n        `;
        noDataMsg.style.display = 'block';
        chartContainer.style.display = 'none';
    }
}

function displayHistoricalChart(historicalData, symbol, assetType) {
    const chartContainer = document.getElementById('historical-chart-container');
    const noDataMsg = document.getElementById('no-historical-data');

    noDataMsg.style.display = 'none';
    chartContainer.style.display = 'block';

    const dates = historicalData.map(item => {
        const d = new Date(item.priceDate);
        if (isNaN(d)) {
            return item.priceDate;
        }
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    });

    const prices = historicalData.map(item => {
        const n = Number(item.price);
        return isNaN(n) ? 0 : n;
    });

    const priceLabel = assetType === 'MUTUAL_FUND' ? 'NAV' : 'Price';

    drawHistoricalChart(dates, prices, symbol, assetType, priceLabel);
}

function drawHistoricalChart(dates, prices, symbol, assetType, priceLabel = 'Price') {
    const canvas = document.getElementById('historical-chart');
    const ctx = canvas.getContext('2d');

    // Dynamically adjust the canvas size based on the parent container's width
    const chartContainer = canvas.parentElement;  // The container of the canvas
    canvas.width = chartContainer.offsetWidth;  // Set canvas width to the container's width
    canvas.height = 300;  // Set a fixed height, or adjust as needed

    // Clear previous chart content
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const width = canvas.width;
    const height = canvas.height;
    const padding = 50;
    const chartWidth = width - 2 * padding;
    const chartHeight = height - 2 * padding;

    const cleanPrices = prices.map(p => {
        const n = Number(p);
        return isNaN(n) ? 0 : n;
    });

    // Find min and max prices
    const minPrice = Math.min(...cleanPrices);
    const maxPrice = Math.max(...cleanPrices);
    let priceRange = maxPrice - minPrice;
    if (priceRange === 0) {
        // Prevent division by zero when all prices are the same; show a small range so chart renders
        priceRange = maxPrice === 0 ? 1 : Math.abs(maxPrice) * 0.01;
    }

    // handling case where there's only a single data point
    const steps = Math.max(1, cleanPrices.length - 1);

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

    cleanPrices.forEach((price, index) => {
        const x = padding + (chartWidth / steps) * index;
        const y = height - padding - ((price - minPrice) / priceRange) * chartHeight;

        if (index === 0) {
            ctx.moveTo(x, y);
        } else {
            ctx.lineTo(x, y);
        }
    });

    ctx.stroke();

    // Draw points
    cleanPrices.forEach((price, index) => {
        const x = padding + (chartWidth / steps) * index;
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
        const x = padding + (chartWidth / steps) * i;
        ctx.fillText(dates[i], x, height - padding + 20);
    }

    currentHistoricalChart = true;
}


// Optional: Re-draw the chart on window resize for responsiveness
window.addEventListener('resize', () => {
    if (currentHistoricalChart) {
        const assetSelector = document.getElementById('asset-selector');
        const symbol = assetSelector.value;
        const assetType = assetSelector.options[assetSelector.selectedIndex].getAttribute('data-asset-type');
        const priceLabel = assetType === 'MUTUAL_FUND' ? 'NAV' : 'Price';

        // Reload and re-render the chart when the window is resized
        loadHistoricalChart();  // This will adjust the chart for the new window size
    }
});
