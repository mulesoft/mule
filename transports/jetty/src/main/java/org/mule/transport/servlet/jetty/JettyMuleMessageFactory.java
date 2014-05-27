/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.ServletMuleMessageFactory;

import javax.servlet.http.HttpServletRequest;

public class JettyMuleMessageFactory extends ServletMuleMessageFactory
{

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        super.addProperties(message, transportMessage);
        HttpServletRequest request = (HttpServletRequest) transportMessage;
        String requestUriWithoutParameters = request.getRequestURI();
        String queryString = request.getQueryString();
        String requestUriWithParameters = requestUriWithoutParameters;
        if (queryString != null)
        {
            requestUriWithParameters += "?" + queryString;
        }
        message.setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, requestUriWithParameters, PropertyScope.INBOUND);
        message.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, requestUriWithoutParameters, PropertyScope.INBOUND);
        message.setProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS, request.getRemoteAddr(), PropertyScope.INBOUND);
        message.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, requestUriWithoutParameters, PropertyScope.INBOUND);
        message.setProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY, request.getRequestURL().toString(), PropertyScope.INBOUND);
        message.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, request.getMethod(), PropertyScope.INBOUND);
    }

}
