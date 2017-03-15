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

    String VERIFIER = "_oauthVerifier";
    String ACCESS_TOKEN_ID = "OAuthAccessTokenId";
    String BASE_EVENT_STATE_TEMPLATE = "MULE_EVENT_ID=%s";
    String DEFAULT_EVENT_STATE_TEMPLATE_SUFFIX = ">>";
    String DEFAULT_EVENT_STATE_TEMPLATE = BASE_EVENT_STATE_TEMPLATE + DEFAULT_EVENT_STATE_TEMPLATE_SUFFIX;
    String EVENT_ID_REGEX = "MULE_EVENT_ID=([\\w-]*)";
    String ORIGINAL_STATE_REGEX = "MULE_EVENT_ID=[\\w-]*";
    String EVENT_STATE_REGEX_SUFFIX = "(.*)";
    String HTTP_STATUS = "http.status";
    String CALLBACK_LOCATION = "Location";
    String AUTHORIZATION_EVENT_KEY_TEMPLATE = "%s-authorization-event";
}


