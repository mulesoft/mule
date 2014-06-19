/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

public interface RefreshTokenManager
{

    public static final int DEFAULT_MIN_REFRESH_INTERVAL = 60 * 1000;

    /**
     * Refreshes the token of the given id for the given adapter.
     *
     * @param adapter
     * @param accessTokenId
     * @throws Exception
     */
    public void refreshToken(OAuth2Adapter adapter, String accessTokenId) throws Exception;

    /**
     * Sets the minimum interval of time in which we allow a given access token id to be refresh.
     * If an access token is attempted to be refresh in an interval lower than the provided, then the token
     * is not refresh
     *
     * @param minRefreshIntervalInMillis a number of milliseconds
     */
    public void setMinRefreshIntervalInMillis(int minRefreshIntervalInMillis);

}
