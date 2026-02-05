// Add Holding Form JavaScript

/**
 * Update form labels and help text based on asset type
 */
function updateFormForAssetType() {
    console.debug('updateFormForAssetType called');
    const assetType = document.getElementById('assetType').value;
    console.debug('assetType=', assetType);
    const symbolLabel = document.getElementById('symbolLabel');
    const symbolHelp = document.getElementById('symbolHelp');
    const quantityLabel = document.getElementById('quantityLabel');
    const quantityHelp = document.getElementById('quantityHelp');
    const purchasePriceLabel = document.getElementById('purchasePriceLabel');
    const purchasePriceHelp = document.getElementById('purchasePriceHelp');
    const stockCategories = document.getElementById('stockCategories');
    const mfCategories = document.getElementById('mfCategories');
    const categoryHelp = document.getElementById('categoryHelp');
    const stockLookup = document.getElementById('stock-lookup');
    console.debug('stockLookup element:', stockLookup);
    const symbolInput = document.getElementById('symbol');
    const symbolSearch = document.getElementById('symbolSearch');
    const suggestions = document.getElementById('symbolSuggestions');
    const infoDiv = document.getElementById('stockLookupInfo');

    // clear previous search state and selection
    if (symbolSearch) symbolSearch.value = '';
    if (suggestions) suggestions.innerHTML = '';
    if (infoDiv) { infoDiv.style.display = 'none'; infoDiv.textContent = ''; }
    if (symbolInput) {
        symbolInput.dataset.selected = 'false';
        symbolInput.value = '';
    }

    if (assetType === 'STOCK') {
        symbolLabel.textContent = 'Stock Symbol *';
        symbolHelp.textContent = 'Select ticker symbol (use the search)';
        quantityLabel.textContent = 'Number of Shares *';
        quantityHelp.textContent = 'Number of shares purchased';
        purchasePriceLabel.textContent = 'Purchase Price per Share *';
        purchasePriceHelp.textContent = 'Price per share at purchase';
        stockCategories.style.display = '';
        mfCategories.style.display = 'none';
        categoryHelp.textContent = 'Select sector category for portfolio analysis';
        if (stockLookup) stockLookup.style.display = 'block';
        if (symbolInput) symbolInput.readOnly = true; // enforce selection-only
    } else if (assetType === 'MUTUAL_FUND') {
        symbolLabel.textContent = 'Scheme Code *';
        symbolHelp.textContent = 'Select mutual fund scheme (use the search)';
        quantityLabel.textContent = 'Number of Units *';
        quantityHelp.textContent = 'Number of units purchased';
        purchasePriceLabel.textContent = 'Purchase NAV *';
        purchasePriceHelp.textContent = 'NAV per unit at purchase';
        stockCategories.style.display = 'none';
        mfCategories.style.display = '';
        categoryHelp.textContent = 'Select fund category (Equity, Debt, Hybrid, Index, ELSS)';
        if (stockLookup) stockLookup.style.display = 'block'; // show the same lookup UI for mutual funds
        if (symbolInput) symbolInput.readOnly = true; // enforce selection-only
    } else {
        stockLookup.style.display = 'none';
        if (symbolInput) symbolInput.readOnly = false;
    }
}

// Handle form submission
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('add-holding-form');
    form.addEventListener('submit', handleFormSubmit);

    // Initialize UI based on current asset type (important on page load)
    try { updateFormForAssetType(); } catch (e) { /* ignore if elements missing */ }

    // Set max date to today for purchase date
    const purchaseDateInput = document.getElementById('purchaseDate');
    const today = new Date().toISOString().split('T')[0];
    purchaseDateInput.setAttribute('max', today);

    // Wire up symbolSearch input
    const symbolSearch = document.getElementById('symbolSearch');
    if (symbolSearch) {
        symbolSearch.addEventListener('input', onSymbolSearchInput);
        // prevent form submission when enter pressed in search — force selection
        symbolSearch.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
            }
        });
    }

    // Ensure assetType change also triggers updateFormForAssetType (defensive)
    const assetTypeSelect = document.getElementById('assetType');
    if (assetTypeSelect) {
        assetTypeSelect.addEventListener('change', updateFormForAssetType);
    }

    // Make sure symbol input can't be typed into for STOCK/MUTUAL_FUND until updateFormForAssetType runs
    const symbolInput = document.getElementById('symbol');
    if (symbolInput) {
        // block typing if readonly — keep default non-readonly
        symbolInput.addEventListener('keydown', (e) => {
            if (symbolInput.readOnly) {
                e.preventDefault();
            }
        });
        // prevent paste into readonly symbol field
        symbolInput.addEventListener('paste', (e) => {
            if (symbolInput.readOnly) {
                e.preventDefault();
            }
        });
    }

    // Handle click outside suggestions to hide
    document.addEventListener('click', (e) => {
        const suggestions = document.getElementById('symbolSuggestions');
        if (suggestions && !suggestions.contains(e.target) && e.target.id !== 'symbolSearch') {
            suggestions.style.display = 'none';
        }
    });
});

/**
 * Handle form submission
 */
async function handleFormSubmit(event) {
    event.preventDefault();

    const formError = document.getElementById('form-error');

    const formSuccess = document.getElementById('form-success');
    const submitBtn = document.getElementById('submit-btn');

    // Hide previous messages
    formError.style.display = 'none';
    formSuccess.style.display = 'none';

    // Get form data
    const formData = {
        assetType: document.getElementById('assetType').value.trim(),
        symbol: document.getElementById('symbol').value.trim().toUpperCase(),
        quantity: parseFloat(document.getElementById('quantity').value),
        purchasePrice: parseFloat(document.getElementById('purchasePrice').value),
        purchaseDate: document.getElementById('purchaseDate').value,
        category: document.getElementById('category').value.trim() || null
    };

    // Validate form data
    if (!validateFormData(formData)) {
        showError('form-error', 'Please fill in all required fields correctly.');
        return;
    }

    // Disable submit button
    submitBtn.disabled = true;
    submitBtn.textContent = 'Adding...';

    try {
        // Submit to API
        await holdingsAPI.create(formData);

        // Show success message
        showSuccess('form-success', 'Holding added successfully! Redirecting...');

        // Reset form
        document.getElementById('add-holding-form').reset();

        // Redirect to dashboard after 1.5 seconds
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1500);

    } catch (error) {
        console.error('Error adding holding:', error);
        showError('form-error', error.message || 'Failed to add holding. Please check your input and try again.');

        // Re-enable submit button
        submitBtn.disabled = false;
        submitBtn.textContent = 'Add Holding';
    }
}

/**
 * Validate form data
 */
function validateFormData(data) {
    // Check required fields
    if (!data.assetType || !data.symbol || !data.purchaseDate) {
        return false;
    }

    // If asset type requires selection, ensure symbol was chosen via suggestions
    const symbolInput = document.getElementById('symbol');
    if (data.assetType === 'STOCK' || data.assetType === 'MUTUAL_FUND') {
        if (!symbolInput || symbolInput.dataset.selected !== 'true') {
            // user didn't select from the dropdown
            showError('form-error', 'Please select a valid symbol from the search suggestions.');
            return false;
        }
    }

    // Check numeric values
    if (isNaN(data.quantity) || data.quantity <= 0) {
        return false;
    }

    if (isNaN(data.purchasePrice) || data.purchasePrice <= 0) {
        return false;
    }

    // Validate symbol length
    if (data.symbol.length > 10) {
        return false;
    }

    return true;
}

/**
 * Show error message in form
 */
function showError(elementId, message) {
    const errorElement = document.getElementById(elementId);
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }
}

/**
 * Show success message in form
 */
function showSuccess(elementId, message) {
    const successElement = document.getElementById(elementId);
    if (successElement) {
        successElement.textContent = message;
        successElement.style.display = 'block';
    }
}

async function onSymbolSearchInput(e) {
    const q = e.target.value.trim();
    const suggestionsDiv = document.getElementById('symbolSuggestions');
    const infoDiv = document.getElementById('stockLookupInfo');
    const assetType = document.getElementById('assetType').value;

    // Clear previous selection whenever user types
    const symbolInput = document.getElementById('symbol');
    if (symbolInput) symbolInput.dataset.selected = 'false';

    if (!q || q.length < 2) {
        if (suggestionsDiv) suggestionsDiv.style.display = 'none';
        if (infoDiv) {
            infoDiv.style.display = 'none';
            infoDiv.textContent = '';
        }
        return;
    }

    try {
        suggestionsDiv.innerHTML = '';
        suggestionsDiv.style.display = 'none';
        infoDiv.style.display = 'none';

        let res;
        let matches = [];

        if (assetType === 'STOCK') {
            res = await assetLookupAPI.searchStocks(q);
            matches = res.matches || [];
        } else if (assetType === 'MUTUAL_FUND') {
            res = await assetLookupAPI.searchMutualFunds(q);
            matches = res.results || [];
        } else {
            return;
        }

        if (!matches.length) {
            infoDiv.style.display = '';
            infoDiv.textContent = 'No matches found';
            return;
        }

        // render suggestions
        matches.forEach(m => {
            const item = document.createElement('div');
            item.className = 'suggestion-item';

            if (assetType === 'STOCK') {
                item.textContent = `${m.symbol} — ${m.name} ${m.exch ? '(' + m.exch + ')' : ''}`;
                item.dataset.type = 'STOCK';
                item.dataset.symbol = m.symbol;
            } else {
                item.textContent = `${m.schemeCode} — ${m.schemeName}`;
                item.dataset.type = 'MUTUAL_FUND';
                item.dataset.schemeCode = m.schemeCode;
            }

            item.addEventListener('click', () => onSelectSuggestionFromDom(item));
            suggestionsDiv.appendChild(item);
        });

        suggestionsDiv.style.display = '';

    } catch (err) {
        console.error('Error searching assets:', err);
        infoDiv.style.display = '';
        infoDiv.textContent = 'Error searching assets';
    }
}

async function onSelectSuggestionFromDom(item) {
    const type = item.dataset.type;
    const symbolInput = document.getElementById('symbol');
    const suggestionsDiv = document.getElementById('symbolSuggestions');
    const infoDiv = document.getElementById('stockLookupInfo');

    // hide suggestions
    if (suggestionsDiv) suggestionsDiv.style.display = 'none';

    if (type === 'STOCK') {
        const symbol = item.dataset.symbol;
        if (symbolInput) {
            symbolInput.value = symbol.toUpperCase();
            symbolInput.dataset.selected = 'true';
        }

        // Fetch stock details to prefill price
        try {
            infoDiv.style.display = '';
            infoDiv.textContent = 'Fetching stock details...';
            const details = await assetLookupAPI.getStockDetails(symbol);
            if (details && details.price) {
                const purchasePriceInput = document.getElementById('purchasePrice');
                if (purchasePriceInput) purchasePriceInput.value = parseFloat(details.price);
            }
            infoDiv.style.display = 'none';
        } catch (err) {
            console.error('Error fetching stock details:', err);
            infoDiv.style.display = '';
            infoDiv.textContent = 'Failed to fetch stock details';
        }
    } else if (type === 'MUTUAL_FUND') {
        const schemeCode = item.dataset.schemecode || item.dataset.schemeCode;
        if (symbolInput) {
            symbolInput.value = schemeCode;
            symbolInput.dataset.selected = 'true';
        }

        // Fetch mutual fund details to prefill NAV
        try {
            infoDiv.style.display = '';
            infoDiv.textContent = 'Fetching mutual fund details...';
            const details = await assetLookupAPI.getMutualFundDetails(schemeCode);
            if (details && details.latestNAV) {
                const purchasePriceInput = document.getElementById('purchasePrice');
                if (purchasePriceInput) purchasePriceInput.value = parseFloat(details.latestNAV);
            }
            infoDiv.style.display = 'none';
        } catch (err) {
            console.error('Error fetching mutual fund details:', err);
            infoDiv.style.display = '';
            infoDiv.textContent = 'Failed to fetch mutual fund details';
        }
    }
}