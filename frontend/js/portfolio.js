// Portfolio Dashboard JavaScript

// load holdings when page loads
document.addEventListener('DOMContentLoaded', () => {
    loadHoldings();
});

async function loadHoldings() {
    const loadingElement = document.getElementById('loading');
    const errorElement = document.getElementById('error');
    const emptyStateElement = document.getElementById('empty-state');
    const holdingsContainer = document.getElementById('holdings-container');
    const holdingsGrid = document.getElementById('holdings-grid');

    try {
        // Show loading
        loadingElement.style.display = 'block';
        errorElement.style.display = 'none';
        emptyStateElement.style.display = 'none';
        holdingsContainer.style.display = 'none';

        // Fetch holdings
        const holdings = await holdingsAPI.getAll();

        // Hide loading
        loadingElement.style.display = 'none';

        // Check if empty
        if (!holdings || holdings.length === 0) {
            emptyStateElement.style.display = 'block';
            return;
        }

        // Display holdings as cards
        holdingsGrid.innerHTML = '';
        holdings.forEach(holding => {
            const card = createHoldingCard(holding);
            holdingsGrid.appendChild(card);
        });

        holdingsContainer.style.display = 'block';

    } catch (error) {
        console.error('Error loading holdings:', error);
        loadingElement.style.display = 'none';
        errorElement.textContent = 'Failed to load holdings. Please make sure the backend server is running.';
        errorElement.style.display = 'block';
    }
}

function createHoldingCard(holding) {
    const card = document.createElement('div');
    card.className = 'holding-card';

    const plClass = holding.profitLoss >= 0 ? 'profit' : 'loss';
    const assetBadgeClass = holding.assetType === 'STOCK' ? 'badge-stock' : 'badge-mf';
    const assetLabel = holding.assetType === 'STOCK' ? 'Stock' : 'Mutual Fund';

    card.innerHTML = `
        <div class="holding-card-header">
            <span class="holding-symbol">${holding.symbol}</span>
            <div class="holding-badges">
                <span class="badge ${assetBadgeClass}">${assetLabel}</span>
                ${holding.category ? `<span class="badge badge-category">${holding.category}</span>` : ''}
            </div>
        </div>
        <div class="holding-stats">
            <div class="holding-stat">
                <div class="holding-stat-label">Quantity</div>
                <div class="holding-stat-value">${holding.quantity}</div>
            </div>
            <div class="holding-stat">
                <div class="holding-stat-label">Current Value</div>
                <div class="holding-stat-value">${formatCurrency(holding.currentValue, holding.assetType)}</div>
            </div>
            <div class="holding-stat">
                <div class="holding-stat-label">Purchase Price</div>
                <div class="holding-stat-value">${formatCurrency(holding.purchasePrice, holding.assetType)}</div>
            </div>
            <div class="holding-stat">
                <div class="holding-stat-label">Purchase Date</div>
                <div class="holding-stat-value">${formatDate(holding.purchaseDate)}</div>
            </div>
        </div>
        <div class="holding-pl ${plClass}">
            <span class="holding-pl-label">Profit / Loss</span>
            <span class="holding-pl-value">${formatCurrency(holding.profitLoss, holding.assetType)} (${formatPercentage(holding.profitLossPercentage)})</span>
        </div>
        <div class="holding-actions">
            <button class="btn-secondary" onclick="editHolding(${holding.id})">Edit</button>
            <button class="btn-danger" onclick="deleteHolding(${holding.id}, ${JSON.stringify(holding.symbol)})">Delete</button>
        </div>
    `;

    return card;
}


function editHolding(id) {
    window.location.href = `edit-holding.html?id=${id}`;
}

async function deleteHolding(id, symbol) {
    if (!confirm(`Are you sure you want to delete ${symbol}?`)) {
        return;
    }

    try {
        await holdingsAPI.delete(id);
        alert(`${symbol} has been deleted successfully.`);
        loadHoldings(); // Reload the list
    } catch (error) {
        console.error('Error deleting holding:', error);
        alert('Failed to delete holding. Please try again.');
    }
}

function refreshHoldings() {
    loadHoldings();
}