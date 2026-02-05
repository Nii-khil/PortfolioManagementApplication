// Portfolio Dashboard JavaScript

// simple cache for names to avoid repeated lookups
const nameCache = {};

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
            // asynchronously fetch and update friendly name
            updateHoldingDisplayName(holding).catch(err => console.debug('Name lookup failed', err));
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

    // Use a display span for symbol so we can update it later without affecting the underlying value
    const symbolDisplayId = `symbol-display-${holding.id}`;

    row.innerHTML = `
        <td><strong id="${symbolDisplayId}">${holding.symbol}</strong></td>
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

async function updateHoldingDisplayName(holding) {
    const key = `${holding.assetType}|${holding.symbol}`;
    const displayEl = document.getElementById(`symbol-display-${holding.id}`);
    if (!displayEl) return;

    if (nameCache[key]) {
        displayEl.textContent = nameCache[key];
        return;
    }

    try {
        if (holding.assetType === 'MUTUAL_FUND') {
            // mutual funds: fetch scheme name
            const details = await assetLookupAPI.getMutualFundDetails(holding.symbol);
            const schemeName = details && details.schemeName ? details.schemeName : null;
            const text = schemeName ? `${schemeName} — ${holding.symbol}` : holding.symbol;
            displayEl.textContent = text;
            nameCache[key] = text;
        } else if (holding.assetType === 'STOCK') {
            // stocks: search for stock to get friendly name
            const res = await assetLookupAPI.searchStocks(holding.symbol);
            const matches = res && (res.matches || res.results) ? (res.matches || res.results) : [];
            let foundName = null;
            for (const m of matches) {
                if (m.symbol && m.symbol.toUpperCase() === holding.symbol.toUpperCase()) {
                    foundName = m.name || m.shortname || m.longname || null;
                    break;
                }
            }
            const text = foundName ? `${holding.symbol} — ${foundName}` : holding.symbol;
            displayEl.textContent = text;
            nameCache[key] = text;
        } else {
            // default: just show symbol
            displayEl.textContent = holding.symbol;
            nameCache[key] = holding.symbol;
        }
    } catch (err) {
        console.debug('Failed to fetch display name for', key, err);
        // fallback: show symbol
        displayEl.textContent = holding.symbol;
        nameCache[key] = holding.symbol;
    }
}

function editHolding(id) {
    window.location.href = `edit-holding.html?id=${id}`;
}

async function deleteHolding(id, symbol) {
    // show friendly name if we have it cached
    const assetKeyStarts = Object.keys(nameCache).find(k => k.endsWith('|'+symbol));
    const displayName = assetKeyStarts ? nameCache[assetKeyStarts] : symbol;

    if (!confirm(`Are you sure you want to delete ${displayName}?`)) {
        return;
    }

    try {
        await holdingsAPI.delete(id);
        alert(`${displayName} has been deleted successfully.`);
        loadHoldings(); // Reload the list
    } catch (error) {
        console.error('Error deleting holding:', error);
        alert('Failed to delete holding. Please try again.');
    }
}

function refreshHoldings() {
    loadHoldings();
}