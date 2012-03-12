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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
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

    public HttpResponseTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.create(HttpResponse.class));
    }

    @Override
    public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException
    {
        HttpResponse httpResponse = new HttpResponse();

        checkVersion(msg);
        setStatus(httpResponse, msg);
        setContentType(httpResponse, msg);
        setHeaders(httpResponse, msg);
        setCookies(httpResponse, msg);

        String date = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US).format(new Date());
        httpResponse.setHeader(new Header(HttpConstants.HEADER_DATE, date));

        setBody(httpResponse, msg);

        return httpResponse;
    }


    private void setBody(HttpResponse response, MuleMessage message) throws TransformerException
    {
        MuleMessage bodyContent = message;
        if(body != null)
        {
            Object processBody = muleContext.getExpressionManager().parse(body, message);
            bodyContent = new DefaultMuleMessage(processBody, muleContext);
        }
        try
        {
            response.setBody(bodyContent);
            setBodyContentLength(response, bodyContent);
        }
        catch(Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    private void setBodyContentLength(HttpResponse response, MuleMessage msg) throws Exception
    {
        if (!response.containsHeader(HttpConstants.HEADER_CONTENT_LENGTH)
                && !response.containsHeader(HttpConstants.HEADER_TRANSFER_ENCODING))
        {
            if (response.hasBody())
            {
                long len = response.getContentLength();
                if (len < 0)
                {
                    if (response.getHttpVersion().lessEquals(HttpVersion.HTTP_1_0))
                    {
                        // Ensure that we convert the payload to an in memory representation
                        // so we don't end up with a chunked response
                        len = msg.getPayloadAsBytes().length;
                        response.setBody(msg);
                        response.setHeader(new Header(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(len)));
                    }
                    else
                    {
                        response.addHeader(new Header(HttpConstants.HEADER_TRANSFER_ENCODING, "chunked"));
                    }
                }
                else
                {
                    response.setHeader(new Header(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(len)));
                }
            }
            else
            {
                response.addHeader(new Header(HttpConstants.HEADER_CONTENT_LENGTH, "0"));
            }
        }
    }


    private void setCookies(HttpResponse response, MuleMessage message) throws TransformerException
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

    private void setHeaders(HttpResponse response, MuleMessage message)
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
        setDefaultVersion(message);
    }

    private void setStatus(HttpResponse response, MuleMessage message) throws TransformerException
    {
        if(status == null)
        {
            status = String.valueOf(HttpConstants.SC_OK);
        }
        try
        {
            response.setStatusLine(HttpVersion.parse(version), Integer.valueOf(evaluate(status, message)));
        }
        catch(ProtocolException e)
        {
            throw new TransformerException(this, e);
        }

    }

    private void setContentType(HttpResponse response, MuleMessage message)
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

    private void setDefaultVersion(MuleMessage message)
    {
        version = message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY);
        if(version == null)
        {
           version = HttpConstants.HTTP11;
        }
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

}
