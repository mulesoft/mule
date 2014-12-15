/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.HttpConstants;
import org.mule.transport.http.HttpConnector;
import org.mule.util.StringUtils;

public class HttpRequestPropertyManager
{

    public static String getRequestPath(MuleMessage message)
    {
        String requestPath = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY, StringUtils.EMPTY);
        if (requestPath.equals(StringUtils.EMPTY))
        {
            requestPath = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_URI, StringUtils.EMPTY);
        }
        return requestPath;
    }

    public static String getScheme(MuleEvent event)
    {
        String scheme = event.getMessageSourceURI().getScheme();
        if (scheme == null)
        {
            scheme = event.getMessage().getInboundProperty(HttpConstants.RequestProperties.HTTP_SCHEME);
        }
        return scheme;
    }

    public static String getBasePath(MuleMessage message)
    {
        String basePath = message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
        if (basePath == null)
        {
            String listenerPath = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_LISTENER_PATH);
            String requestPath = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY);
            if (listenerPath.contains(requestPath))
            {
                return requestPath;
            }
            int slashCount = StringUtils.countMatches(listenerPath, "/");
            int matchPrefixIndex = StringUtils.ordinalIndexOf(requestPath, "/", slashCount);
            basePath = requestPath.substring(0, matchPrefixIndex);
        }
        return basePath;
    }
}
