/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester;

import org.mule.module.http.api.HttpAuthentication;

/**
 * Configuration object for an {@code HttpRequester}.
 */
public interface HttpRequesterConfig
{

    public String getBasePath();

    public String getHost();

    public String getPort();

    public String getFollowRedirects();

    public String getRequestStreamingMode();

    public String getSendBodyMode();

    public String getParseResponse();

    public String getResponseTimeout();

    public HttpAuthentication getAuthentication();


}
