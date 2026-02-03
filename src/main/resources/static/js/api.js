
// API Configuration
const API_BASE_URL = 'http://localhost:8081/api';

const api = {
    get: async (endpoint) => {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API GET Error:', error);
            throw error;
        }
    },

    post: async (endpoint, data) => {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API POST Error:', error);
            throw error;
        }
    },

    put: async (endpoint, data) => {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API PUT Error:', error);
            throw error;
        }
    },

    delete: async (endpoint) => {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return true;
        } catch (error) {
            console.error('API DELETE Error:', error);
            throw error;
        }
    }
};

const holdingsAPI = {
    getAll: async () => {
        return await api.get('/holdings');
    },

    getById: async (id) => {
        return await api.get(`/holdings/${id}`);
    },

    create: async (holdingData) => {
        return await api.post('/holdings', holdingData);
    },

    update: async (id, holdingData) => {
        return await api.put(`/holdings/${id}`, holdingData);
    },

    delete: async (id) => {
        return await api.delete(`/holdings/${id}`);
    },

    getByType: async (assetType) => {
        return await api.get(`/holdings/type/${assetType}`);
    }
};

const portfolioAPI = {
    getSummary: async () => {
        return await api.get('/portfolio/summary');
    },

    getBestPerformer: async () => {
        return await api.get('/portfolio/best-performer');
    },

    getWorstPerformer: async () => {
        return await api.get('/portfolio/worst-performer');
    }
};

function formatCurrency(amount, assetTypeOrCurrency) {
    if (amount === null || amount === undefined) return '₹0.00';

    // Determine currency
    let currency = 'INR'; // default
    if (assetTypeOrCurrency) {
        if (assetTypeOrCurrency === 'STOCK') {
            currency = 'USD';
        } else if (assetTypeOrCurrency === 'USD' || assetTypeOrCurrency === 'INR' ||
                   assetTypeOrCurrency === 'EUR' || assetTypeOrCurrency === 'GBP') {
            currency = assetTypeOrCurrency;
        } else {
            currency = 'INR'; // MUTUAL_FUND, CASH, or unknown
        }
    }

    const currencySymbols = {
        'INR': '₹',
        'USD': '$',
        'EUR': '€',
        'GBP': '£'
    };

    // Format number based on currency
    const locale = currency === 'INR' ? 'en-IN' : 'en-US';
    const formatted = new Intl.NumberFormat(locale, {
        style: 'decimal',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount);

    const symbol = currencySymbols[currency] || '₹';
    return `${symbol}${formatted}`;
}

function formatCurrencyInr(amount) {
    if (amount === null || amount === undefined) return '₹0.00';

    const formatted = new Intl.NumberFormat('en-IN', {
        style: 'decimal',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount);

    return `₹${formatted}`;
}

function formatPercentage(percentage) {
    if (percentage === null || percentage === undefined) return '0.00%';
    const sign = percentage >= 0 ? '+' : '';
    return `${sign}${percentage.toFixed(2)}%`;
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function showError(elementId, message) {
    const errorElement = document.getElementById(elementId);
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.style.display = 'block';
        setTimeout(() => {
            errorElement.style.display = 'none';
        }, 5000);
    }
}

function showSuccess(elementId, message) {
    const successElement = document.getElementById(elementId);
    if (successElement) {
        successElement.textContent = message;
        successElement.style.display = 'block';
        setTimeout(() => {
            successElement.style.display = 'none';
        }, 3000);
    }
}