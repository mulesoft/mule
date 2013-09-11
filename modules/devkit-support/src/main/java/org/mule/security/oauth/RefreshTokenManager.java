/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

public interface RefreshTokenManager
{

    /**
     * Refreshes the token of the given id for the given adapter.
     * @param adapter
     * @param accessTokenId
     * @throws Exception
     */
    public void refreshToken(OAuth2Adapter adapter, String accessTokenId) throws Exception;

}
