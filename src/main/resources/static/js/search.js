import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Fetches search results from the backend API.
 *
 * @param {string} query The search query.
 * @param {number} [offset=0] The starting index for the results (default is 0).
 * @param {number} [limit=10] The maximum number of results to return (default is 10).
 * @returns {Promise<object|null>} A promise that resolves to the search results as a JSON object or null if an error occurs.
 */
const search = async (query, offset = 0, limit = 10) => {
    if (!query) {
        console.error('Search query cannot be empty');
        return null;
    }

    if (offset < 0) {
        console.error('Offset cannot be negative');
        return null;
    }
    if (limit <= 0) {
        console.error('Limit must be greater than zero');
        return null;
    }

    try {
        const response = await axios.get(`${API_BASE_URL}/search`, {
            params: { query, offset, limit },
        });

        if (response.status >= 200 && response.status < 300) {
            return response.data;
        } else {
            console.error('Search API returned an error status:', response.status);
            return null;
        }
    } catch (error) {
        if (error.response) {
            console.error('Error fetching search results:', error.response.status, error.response.data);
            if (error.response.status === 404) {
                console.error('The requested resource was not found. Please check the API endpoint.');
            }
        } else if (error.request) {
            console.error('No response received from the API:', error.request);
        } else {
            console.error('Error setting up the request:', error.message);
        }
        return null;
    }
};

export default search;
