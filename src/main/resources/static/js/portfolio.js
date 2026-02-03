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
    const holdingsBody = document.getElementById('holdings-body');

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

        // Display holdings
        holdingsBody.innerHTML = '';
        holdings.forEach(holding => {
            const row = createHoldingRow(holding);
            holdingsBody.appendChild(row);
        });

        holdingsContainer.style.display = 'block';

    } catch (error) {
        console.error('Error loading holdings:', error);
        loadingElement.style.display = 'none';
        errorElement.textContent = 'Failed to load holdings. Please make sure the backend server is running.';
        errorElement.style.display = 'block';
    }
}

function createHoldingRow(holding) {
    const row = document.createElement('tr');

    // Determine profit/loss class
    const plClass = holding.profitLoss >= 0 ? 'profit' : 'loss';

    // Get currency symbol based on asset type
    const currencySymbol = holding.currencySymbol || (holding.assetType === 'STOCK' ? '$' : '₹');

    row.innerHTML = `
        <td><strong>${holding.symbol}</strong></td>
        <td>${holding.assetType}</td>
        <td>${holding.category || '-'}</td>
        <td>${holding.quantity}</td>
        <td>${formatCurrency(holding.purchasePrice, holding.assetType)}</td>
        <td>${formatCurrency(holding.currentPrice, holding.assetType)}</td>
        <td>${formatCurrency(holding.currentValue, holding.assetType)}</td>
        <td class="${plClass}">${formatCurrency(holding.profitLoss, holding.assetType)}</td>
        <td class="${plClass}">${formatPercentage(holding.profitLossPercentage)}</td>
        <td>${formatDate(holding.purchaseDate)}</td>
        <td>
            <button class="btn-secondary" onclick="editHolding(${holding.id})"
                    style="margin-right: 5px; padding: 6px 12px;">
                Edit
            </button>
            <button class="btn-danger" onclick="deleteHolding(${holding.id}, '${holding.symbol}')">
                Delete
            </button>
        </td>
    `;

    return row;
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