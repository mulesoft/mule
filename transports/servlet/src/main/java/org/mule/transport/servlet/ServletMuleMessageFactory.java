/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import static org.apache.commons.lang.StringUtils.EMPTY;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.EnumerationUtils;

public class ServletMuleMessageFactory extends AbstractMuleMessageFactory
{
    private static final String REMOTE_ADDRESS_HEADER = "remoteAddress";

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[] { HttpServletRequest.class };
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        HttpServletRequest request = (HttpServletRequest) transportMessage;

        String method = request.getMethod();
        if (HttpConstants.METHOD_GET.equalsIgnoreCase(method))
        {
            return queryString(request);
        }
        else
        {
            return extractPayloadFromPostRequest(request);
        }
    }

    protected Object extractPayloadFromPostRequest(HttpServletRequest request) throws Exception
    {
        /*
         * Servlet Spec v2.5:
         *
         * SRV.3.1.1
         * When Parameters Are Available
         *
         * The following are the conditions that must be met before post form data will
         * be populated to the parameter set:
         *
         * 1. The request is an HTTP or HTTPS request.
         * 2. The HTTP method is POST.
         * 3. The content type is application/x-www-form-urlencoded.
         * 4. The servlet has made an initial call of any of the getParameter family of meth-
         *    ods on the request object.
         *
         * If the conditions are not met and the post form data is not included in the
         * parameter set, the post data must still be available to the servlet via the request
         * object's input stream. If the conditions are met, post form data will no longer be
         * available for reading directly from the request object's input stream.
         *
         * -----------------------------------------------------------------------------------
         *
         * In plain english:if you call getInputStream on a POSTed request before you call one of
         * the getParameter* methods, you'll lose the parameters. So we touch the parameters first
         * and only then we return the input stream that will be the payload of the message.
         */
        request.getParameterNames();

        return request.getInputStream();
    }

    protected String queryString(HttpServletRequest request)
    {
        StringBuilder buf = new StringBuilder(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null)
        {
            buf.append("?");
            buf.append(queryString);
        }

        return buf.toString();
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        HttpServletRequest request = (HttpServletRequest) transportMessage;

        setupRequestParameters(request, message);
        setupEncoding(request, message);
        setupSessionId(request, message);
        setupContentType(request, message);
        setupCharacterEncoding(request, message);
        setupRemoteAddress(request, message);
        setupMessageProperties(request, message);
    }

    @SuppressWarnings("unchecked")
    protected void setupRequestParameters(HttpServletRequest request, DefaultMuleMessage message)
    {
        Enumeration<String> parameterNames = request.getParameterNames();
        if (parameterNames != null)
        {
            Map<String, Object> parameterProperties = new HashMap<String, Object>();
            Map<String, Object> parameterMap = new HashMap<String, Object>();
            while (parameterNames.hasMoreElements())
            {
                String name = parameterNames.nextElement();
                String key = ServletConnector.PARAMETER_PROPERTY_PREFIX + name;
                String value = request.getParameterValues(name)[0];

                parameterProperties.put(key, value);
                parameterMap.put(name, value);
            }

            // support for the HttpRequestToParameterMap transformer: put the map of request
            // parameters under a well defined key into the message properties as well
            parameterProperties.put(ServletConnector.PARAMETER_MAP_PROPERTY_KEY, parameterMap);

            // make servlet and jetty compatible with http transport
            parameterProperties.put(HttpConnector.HTTP_QUERY_PARAMS, parameterMap);

            message.addInboundProperties(parameterProperties);
        }
    }

    protected void setupEncoding(HttpServletRequest request, MuleMessage message)
    {
        String contentType = request.getContentType();
        if (contentType != null)
        {
            int index = contentType.indexOf("charset");
            if (index > -1)
            {
                int semicolonIndex = contentType.lastIndexOf(";");
                String encoding;
                if (semicolonIndex > index)
                {
                    encoding = contentType.substring(index + 8, semicolonIndex);
                }
                else
                {
                    encoding = contentType.substring(index + 8);
                }
                // some stacks send quotes around the charset encoding
                message.setEncoding(encoding.replaceAll("\"", "").trim());
            }
        }
    }

    protected void setupSessionId(HttpServletRequest request, MuleMessage message)
    {
        try
        {
            // We wrap this call as on some App Servers (Websphere) it can cause an NPE
            HttpSession session = request.getSession(false);
            if (session != null)
            {
                ((DefaultMuleMessage) message).setInboundProperty(ServletConnector.SESSION_ID_PROPERTY_KEY, session.getId());
            }
        }
        catch (Exception e)
        {
            // C'est la vie
        }
    }

    protected void setupContentType(HttpServletRequest request, DefaultMuleMessage message)
    {
        String contentType = request.getContentType();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ServletConnector.CONTENT_TYPE_PROPERTY_KEY, contentType);

        message.addInboundProperties(properties);
    }

    protected void setupCharacterEncoding(HttpServletRequest request, DefaultMuleMessage message)
    {
        String characterEncoding = request.getCharacterEncoding();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ServletConnector.CHARACTER_ENCODING_PROPERTY_KEY, characterEncoding);

        message.addInboundProperties(properties);
    }

    protected void setupRemoteAddress(HttpServletRequest request, DefaultMuleMessage message)
    {
        message.setInboundProperty(REMOTE_ADDRESS_HEADER, request.getRemoteAddr());
    }

    protected void setupMessageProperties(HttpServletRequest request, DefaultMuleMessage message)
    {
        Map<String, Object> messageProperties = new HashMap<String, Object>();

        copyParameters(request, messageProperties);
        copyAttributes(request, messageProperties);
        copyHeaders(request, messageProperties);

        message.addInboundProperties(messageProperties);
    }

    protected void copyParameters(HttpServletRequest request, Map<String, Object> messageProperties)
    {
        Map<?, ?> parameterMap = request.getParameterMap();
        if (parameterMap != null && parameterMap.size() > 0)
        {
            for (Map.Entry<?, ?> entry : parameterMap.entrySet())
            {
                String key = (String) entry.getKey();
                Object value = entry.getValue();
                if (value != null)
                {
                    if (value.getClass().isArray() && ((Object[]) value).length == 1)
                    {
                        value = ((Object[]) value)[0];
                    }
                }

                messageProperties.put(key, value);
            }
        }
    }

    protected void copyAttributes(HttpServletRequest request, Map<String, Object> messageProperties)
    {
        for (Enumeration<?> e = request.getAttributeNames(); e.hasMoreElements();)
        {
            String key = (String) e.nextElement();
            messageProperties.put(key, request.getAttribute(key));
        }
    }

    protected void copyHeaders(HttpServletRequest request, Map<String, Object> messageProperties)
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        for (Enumeration<?> e = request.getHeaderNames(); e.hasMoreElements();)
        {
            String key = (String)e.nextElement();
            String realKey = key;
            Object realValue;

            if (key.startsWith(HttpConstants.X_PROPERTY_PREFIX))
            {
                realKey = key.substring(2);
            }

            // Workaround for containers that strip the port from the Host header.
            // This is needed so Mule components can figure out what port they're on.
            if (HttpConstants.HEADER_HOST.equalsIgnoreCase(key))
            {
                realKey = HttpConstants.HEADER_HOST;

                String value = request.getHeader(key);
                int port = request.getLocalPort();
                if (!value.contains(":") && port != 80 && port != 443)
                {
                    value = value + ":" + port;
                }
                realValue = value;
            }
            else
            {
                Enumeration<?> valueEnum = request.getHeaders(key);
                List<?> values = EnumerationUtils.toList(valueEnum);
                if (values.size() > 1)
                {
                    realValue = values.toArray();
                }
                else
                {
                    realValue = values.size() == 1 ? values.get(0) : EMPTY;
                }
            }
            headers.put(realKey, realValue);
        }
        messageProperties.put(HttpConnector.HTTP_HEADERS, headers);
        messageProperties.putAll(headers);
    }
}
