// Add Holding Form JavaScript

/**
 * Update form labels and help text based on asset type
 */
function updateFormForAssetType() {
    const assetType = document.getElementById('assetType').value;
    const symbolLabel = document.getElementById('symbolLabel');
    const symbolHelp = document.getElementById('symbolHelp');
    const quantityLabel = document.getElementById('quantityLabel');
    const quantityHelp = document.getElementById('quantityHelp');
    const purchasePriceLabel = document.getElementById('purchasePriceLabel');
    const purchasePriceHelp = document.getElementById('purchasePriceHelp');
    const stockCategories = document.getElementById('stockCategories');
    const mfCategories = document.getElementById('mfCategories');
    const categoryHelp = document.getElementById('categoryHelp');

    if (assetType === 'STOCK') {
        symbolLabel.textContent = 'Stock Symbol *';
        symbolHelp.textContent = 'Enter ticker symbol (e.g., AAPL, GOOGL, MSFT)';
        quantityLabel.textContent = 'Number of Shares *';
        quantityHelp.textContent = 'Number of shares purchased';
        purchasePriceLabel.textContent = 'Purchase Price per Share *';
        purchasePriceHelp.textContent = 'Price per share at purchase';
        stockCategories.style.display = '';
        mfCategories.style.display = 'none';
        categoryHelp.textContent = 'Select sector category for portfolio analysis';
    } else if (assetType === 'MUTUAL_FUND') {
        symbolLabel.textContent = 'Scheme Code *';
        symbolHelp.textContent = 'Enter mutual fund scheme code (e.g., 119551, 125497)';
        quantityLabel.textContent = 'Number of Units *';
        quantityHelp.textContent = 'Number of units purchased';
        purchasePriceLabel.textContent = 'Purchase NAV *';
        purchasePriceHelp.textContent = 'NAV per unit at purchase';
        stockCategories.style.display = 'none';
        mfCategories.style.display = '';
        categoryHelp.textContent = 'Select fund category (Equity, Debt, Hybrid, Index, ELSS)';
    } else if (assetType === 'CASH') {
        symbolLabel.textContent = 'Currency *';
        symbolHelp.textContent = 'Enter currency code (e.g., USD, EUR, GBP)';
        quantityLabel.textContent = 'Amount *';
        quantityHelp.textContent = 'Cash amount';
        purchasePriceLabel.textContent = 'Exchange Rate *';
        purchasePriceHelp.textContent = 'Exchange rate (use 1.00 for base currency)';
        stockCategories.style.display = 'none';
        mfCategories.style.display = 'none';
        categoryHelp.textContent = 'Category not applicable for cash';
    }
}

// Handle form submission
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('add-holding-form');
    form.addEventListener('submit', handleFormSubmit);

    // Set max date to today for purchase date
    const purchaseDateInput = document.getElementById('purchaseDate');
    const today = new Date().toISOString().split('T')[0];
    purchaseDateInput.setAttribute('max', today);
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