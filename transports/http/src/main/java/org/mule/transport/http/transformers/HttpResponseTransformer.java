/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.http.CacheControlHeader;
import org.mule.transport.http.CookieHelper;
import org.mule.transport.http.CookieWrapper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;

public class HttpResponseTransformer extends AbstractMessageTransformer
{
    private Map<String, String> headers = new HashMap<String, String>();
    private List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
    private String body;
    private String contentType;
    private String status;
    private String version;
    private CacheControlHeader cacheControl;

    public HttpResponseTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.create(HttpResponse.class));
    }

    @Override
    public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException
    {
        try
        {
            HttpResponse httpResponse = getHttpResponse(msg);

            propagateMessageProperties(httpResponse, msg);
            checkVersion(msg);
            setStatus(httpResponse, msg);
            setContentType(httpResponse, msg);
            setHeaders(httpResponse, msg);
            setCookies(httpResponse, msg);
            setCacheControl(httpResponse, msg);
            String date = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US).format(new Date());
            httpResponse.setHeader(new Header(HttpConstants.HEADER_DATE, date));
            setBody(httpResponse, msg);
            return httpResponse;
        }
        catch(Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    private void propagateMessageProperties(HttpResponse response, MuleMessage message)
    {
        copyOutboundProperties(response, message);
        copyCorrelationIdProperties(response, message);
        copyReplyToProperty(response, message);
    }

    private void copyCorrelationIdProperties(HttpResponse response, MuleMessage message)
    {
        if(message.getCorrelationId() != null)
        {
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_CORRELATION_ID_PROPERTY,
                    message.getCorrelationId()));
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                    String.valueOf(message.getCorrelationGroupSize())));
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                    String.valueOf(message.getCorrelationSequence())));
        }
    }

    private void copyReplyToProperty(HttpResponse response, MuleMessage message)
    {
        if(message.getReplyTo() != null)
        {
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_REPLY_TO_PROPERTY,
                    message.getReplyTo().toString()));
        }
    }

    protected void copyOutboundProperties(HttpResponse response, MuleMessage message)
    {
        for(String headerName : message.getOutboundPropertyNames())
        {
            Object headerValue = message.getOutboundProperty(headerName);
            if(headerValue != null)
            {
                if(isMuleProperty(headerName))
                {
                    addMuleHeader(response, headerName, headerValue);
                }
                else if(isMultiValueCookie(headerName, headerValue))
                {
                    addMultiValueCookie(response, (Cookie[]) headerValue);
                }
                else
                {
                    response.setHeader(new Header(headerName, headerValue.toString()));
                }
            }
        }
    }

    private void addMuleHeader(HttpResponse response, String headerName, Object headerValue)
    {
        response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + headerName, headerValue.toString()));
    }

    private boolean isMuleProperty(String headerName)
    {
        return headerName.startsWith(MuleProperties.PROPERTY_PREFIX);
    }

    private void addMultiValueCookie(HttpResponse response, Cookie[] cookies)
    {
        Cookie[] arrayOfCookies = CookieHelper.asArrayOfCookies(cookies);
        for (Cookie cookie : arrayOfCookies)
        {
            response.addHeader(new Header(HttpConstants.HEADER_COOKIE_SET,
                CookieHelper.formatCookieForASetCookieHeader(cookie)));
        }
    }

    private boolean isMultiValueCookie(String headerName, Object headerValue)
    {
        return HttpConstants.HEADER_COOKIE_SET.equals(headerName)
                && headerValue instanceof Cookie[];
    }

    private HttpResponse getHttpResponse(MuleMessage message)
    {
        HttpResponse httpResponse;

        if(message.getPayload() instanceof HttpResponse)
        {
            httpResponse = (HttpResponse) message.getPayload();
        }
        else
        {
            httpResponse = new HttpResponse();
        }

        return httpResponse;
    }


    protected void setCacheControl(HttpResponse response, MuleMessage message)
    {
        if(cacheControl != null)
        {
            cacheControl.evaluate(message, muleContext.getExpressionManager());
            String cacheControlValue = cacheControl.toString();
            if(!"".equals(cacheControlValue))
            {
                if(headers.get(HttpConstants.HEADER_CACHE_CONTROL) != null)
                {
                    Header cacheControlHeader = response.getFirstHeader(HttpConstants.HEADER_CACHE_CONTROL);
                    if(cacheControlHeader != null)
                    {
                        cacheControlValue += "," + cacheControlHeader.getValue();
                    }
                }
                response.setHeader(new Header(HttpConstants.HEADER_CACHE_CONTROL, cacheControlValue));
            }
        }
    }

    protected void setBody(HttpResponse response, MuleMessage message) throws TransformerException
    {
        try
        {
            if(body != null)
            {
                response.setBody(muleContext.getExpressionManager().parse(body, message));
            }
            else
            {
                response.setBody(message);
            }
        }
        catch(Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void setCookies(HttpResponse response, MuleMessage message) throws TransformerException
    {
        if(!cookies.isEmpty())
        {
            for(CookieWrapper cookie : cookies)
            {
                try
                {
                    cookie.evaluate(message, muleContext.getExpressionManager());
                    response.addHeader(new Header(HttpConstants.HEADER_COOKIE_SET,
                                                   CookieHelper.formatCookieForASetCookieHeader(cookie.createCookie())));

                }
                catch(Exception e)
                {
                    throw new TransformerException(this, e);
                }

            }
        }
    }

    protected void setHeaders(HttpResponse response, MuleMessage message)
    {
        if(headers != null && !headers.isEmpty())
        {
            for(String headerName : headers.keySet())
            {
                String name = evaluate(headerName, message);
                String value = headers.get(headerName);
                response.setHeader(new Header(name, evaluate(value, message)));
            }
        }
    }

    protected void checkVersion(MuleMessage message)
    {
        version = HttpConstants.HTTP10;
        //version = message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY);
        //if(version == null)
        //{
        //   version = HttpConstants.HTTP11;
        //}
    }

    private void setStatus(HttpResponse response, MuleMessage message) throws TransformerException
    {
        if(status != null)
        {
            try
            {
                response.setStatusLine(HttpVersion.parse(version), Integer.valueOf(evaluate(status, message)));
            }
            catch(ProtocolException e)
            {
                throw new TransformerException(this, e);
            }
        }
    }

    protected void setContentType(HttpResponse response, MuleMessage message)
    {
        if(contentType == null)
        {
            contentType = getDefaultContentType(message);

        }
        response.setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, evaluate(contentType, message)));
    }

    private String evaluate(String value, MuleMessage message)
    {
        Object realValue = value;

        if (value != null && muleContext.getExpressionManager().isExpression(value.toString()))
        {
            realValue = muleContext.getExpressionManager().evaluate(value.toString(), message);
        }

        return String.valueOf(realValue);
    }

    private String getDefaultContentType(MuleMessage message)
    {
        String contentType = message.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        if(contentType == null)
        {
            contentType = HttpConstants.DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }


    public void setHeaders(Map<String, String> headers)
    {
        this.headers.putAll(headers);
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public void setCookies(List<CookieWrapper> cookies)
    {
        this.cookies = cookies;
    }

    public void addHeader(String key, String value)
    {
        headers.put(key, value);
    }

    public void setCacheControl(CacheControlHeader cacheControl)
    {
        this.cacheControl = cacheControl;
    }

    public String getVersion()
    {
        return version;
    }

}
