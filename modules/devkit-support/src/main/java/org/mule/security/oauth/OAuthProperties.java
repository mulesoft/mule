/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

/**
 * <code>OAuthProperties</code> is a set of constants pertaining to OAuth properties.
 */
public interface OAuthProperties
{

    public static final String VERIFIER = "_oauthVerifier";
    public static final String ACCESS_TOKEN_ID = "OAuthAccessTokenId";
    public static final String EVENT_STATE_TEMPLATE = "<<MULE_EVENT_ID=%s>>";
    public static final String HTTP_STATUS = "http.status";
    public static final String CALLBACK_LOCATION = "Location";
    public static final String AUTHORIZATION_EVENT_KEY_TEMPLATE = "%s-authorization-event";
}


