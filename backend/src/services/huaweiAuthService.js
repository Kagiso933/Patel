const axios = require('axios');
const huaweiConfig = require('../config/huaweiConfig');

// Simple in-memory cache: Huawei tokens are valid ~1hr, no need to fetch one
// per request.
let cachedToken = null;
let cachedTokenExpiresAt = 0;

/**
 * Requests (or reuses) an OAuth2 client-credentials access token from
 * Huawei. Required before calling any AppGallery Connect / Wallet Kit
 * server API.
 */
async function getHuaweiAccessToken() {
  if (cachedToken && Date.now() < cachedTokenExpiresAt) {
    return cachedToken;
  }

  const params = new URLSearchParams({
    grant_type: 'client_credentials',
    client_id: huaweiConfig.clientId,
    client_secret: huaweiConfig.clientSecret,
  });

  const { data } = await axios.post(huaweiConfig.tokenUrl, params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });

  cachedToken = data.access_token;
  // Refresh a little early to avoid using a token that expires mid-request.
  cachedTokenExpiresAt = Date.now() + (data.expires_in - 60) * 1000;

  return cachedToken;
}

module.exports = { getHuaweiAccessToken };
