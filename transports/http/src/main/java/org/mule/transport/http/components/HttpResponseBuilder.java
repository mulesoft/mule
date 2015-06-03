/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.components;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NonBlockingSupported;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.http.CacheControlHeader;
import org.mule.transport.http.CookieHelper;
import org.mule.transport.http.CookieWrapper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.i18n.HttpMessages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseBuilder extends AbstractMessageProcessorOwner
    implements Initialisable, MessageProcessor, NonBlockingSupported
{
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseBuilder.class);

    private Map<String, String> headers = new HashMap<String, String>();
    private List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
    private String contentType;
    private String status;
    private String version;
    private CacheControlHeader cacheControl;
    private boolean propagateMuleProperties = false;
    private AbstractTransformer bodyTransformer;
    private SimpleDateFormat expiresHeaderFormatter;
    private SimpleDateFormat dateFormatter;

    private List<MessageProcessor> ownedMessageProcessor = new ArrayList<MessageProcessor>();

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        expiresHeaderFormatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
        expiresHeaderFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (HttpConstants.SERVER_TIME_ZONE_PROPERTY.isEnabled())
        {
            logger.warn(HttpMessages.dateInServerTimeZone().getMessage());
            dateFormatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
        }
        else
        {
            dateFormatter = expiresHeaderFormatter;
        }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleMessage msg = event.getMessage();

        HttpResponse httpResponse = getHttpResponse(msg);

        propagateMessageProperties(httpResponse, msg);
        checkVersion(msg);
        setStatus(httpResponse, msg);
        setContentType(httpResponse, msg);
        setHeaders(httpResponse, msg);
        setCookies(httpResponse, msg);
        setCacheControl(httpResponse, msg);
        setDateHeader(httpResponse, new Date());
        setBody(httpResponse, msg, event);

        msg.setPayload(httpResponse);
        return event;
    }

    protected void setDateHeader(HttpResponse httpResponse, Date date)
    {
        httpResponse.setHeader(new Header(HttpConstants.HEADER_DATE, dateFormatter.format(date)));
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return ownedMessageProcessor;
    }

    protected void setBody(HttpResponse response, MuleMessage message, MuleEvent event) throws MuleException
    {
        if(bodyTransformer != null)
        {
            message.applyTransformers(event, bodyTransformer);
        }

        try
        {
            // If the payload is already HttpResponse then it already has the body set
            if(!(message.getPayload() instanceof HttpResponse))
            {
                response.setBody(message);
            }
        }
        catch(Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    private void propagateMessageProperties(HttpResponse response, MuleMessage message)
    {
        copyOutboundProperties(response, message);
        if(propagateMuleProperties)
        {
            copyCorrelationIdProperties(response, message);
            copyReplyToProperty(response, message);
        }
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
                    if(propagateMuleProperties)
                    {
                        addMuleHeader(response, headerName, headerValue);
                    }
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
            cacheControl.parse(message, muleContext.getExpressionManager());
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

    protected void setCookies(HttpResponse response, MuleMessage message) throws MuleException
    {
        if(!cookies.isEmpty())
        {
            for(CookieWrapper cookie : cookies)
            {
                try
                {
                    cookie.parse(message, muleContext.getExpressionManager());
                    response.addHeader(new Header(HttpConstants.HEADER_COOKIE_SET,
                                                   CookieHelper.formatCookieForASetCookieHeader(cookie.createCookie())));

                }
                catch(Exception e)
                {
                    throw new DefaultMuleException(e);
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
                String name = parse(headerName, message);
                String value = headers.get(headerName);
                if(HttpConstants.HEADER_EXPIRES.equals(name))
                {
                    response.setHeader(new Header(name, evaluateDate(value, message)));
                }
                else
                {
                    response.setHeader(new Header(name, parse(value, message)));
                }
            }
        }
    }

    protected void checkVersion(MuleMessage message)
    {
        version = message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY);
        if(version == null)
        {
           version = HttpConstants.HTTP11;
        }
    }

    private void setStatus(HttpResponse response, MuleMessage message) throws MuleException
    {
        if(status != null)
        {
            try
            {
                response.setStatusLine(HttpVersion.parse(version), Integer.valueOf(parse(status, message)));
            }
            catch(ProtocolException e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

    protected void setContentType(HttpResponse response, MuleMessage message)
    {
        if(contentType == null)
        {
            contentType = getDefaultContentType(message);

        }
        response.setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, parse(contentType, message)));
    }

    private String parse(String value, MuleMessage message)
    {
        if(value != null)
        {
            return muleContext.getExpressionManager().parse(value, message);
        }
        return value;
    }

    private String evaluateDate(String value, MuleMessage message)
    {
        Object realValue = value;

        if (value != null && muleContext.getExpressionManager().isExpression(value))
        {
            realValue = muleContext.getExpressionManager().evaluate(value, message);
        }

        if(realValue instanceof Date)
        {
            return expiresHeaderFormatter.format(realValue);
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

    public void setPropagateMuleProperties(boolean propagateMuleProperties)
    {
        this.propagateMuleProperties = propagateMuleProperties;
    }

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.bodyTransformer = (AbstractTransformer) messageProcessor;
        ownedMessageProcessor.add(bodyTransformer);
    }


}
