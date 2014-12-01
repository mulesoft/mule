/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import com.google.common.net.MediaType;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import javax.activation.DataHandler;

/**
 * Maps an HTTP response into a Mule event. A new message is set in the event with the contents of the response.
 * The body will be set as payload by default (except that the target attribute is set in the requester, in that case
 * the enricher expression provided will be used to set the response). Headers are mapped as inbound properties.
 * The status code is mapped as an inbound property {@code HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY}.
 */
public class HttpResponseToMuleEvent
{
    private DefaultHttpRequester requester;
    private MuleContext muleContext;

    private AttributeEvaluator parseResponse;

    public HttpResponseToMuleEvent(DefaultHttpRequester requester, MuleContext muleContext, AttributeEvaluator parseResponse)
    {
        this.requester = requester;
        this.muleContext = muleContext;
        this.parseResponse = parseResponse;
    }

    public void convert(MuleEvent muleEvent, HttpResponse response) throws MessagingException
    {
        muleEvent.setMessage(new DefaultMuleMessage(muleEvent.getMessage().getPayload(), muleContext));

        String responseContentType = response.getHeaderValue(HttpHeaders.Names.CONTENT_TYPE.toLowerCase());

        InputStream responseInputStream = ((InputStreamHttpEntity) response.getEntity()).getInputStream();

        // Map headers in the response as inbound properties
        for (String headerName : response.getHeaderNames())
        {
            String headerValue = response.getHeaderValue(headerName);
            muleEvent.getMessage().setProperty(headerName, headerValue, PropertyScope.INBOUND);
        }

        muleEvent.getMessage().setProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY, response.getStatusCode(), PropertyScope.INBOUND);

        if(responseContentType != null)
        {
            MediaType mediaType = MediaType.parse(responseContentType);
            String encoding = mediaType.charset().isPresent() ? mediaType.charset().get().name() : Charset.defaultCharset().name();
            muleEvent.getMessage().setEncoding(encoding);
            if (parseResponse.resolveBooleanValue(muleEvent) && responseContentType.startsWith("multipart/"))
            {
                try
                {
                    final Collection<HttpPartDataSource> httpParts = HttpPartDataSource.createFrom(HttpParser.parseMultipartContent(responseInputStream, responseContentType));
                    DefaultMuleMessage message = (DefaultMuleMessage) muleEvent.getMessage();
                    setResponsePayload(NullPayload.getInstance(), muleEvent);
                    for (HttpPartDataSource httpPart : httpParts)
                    {
                        message.addInboundAttachment(httpPart.getName(), new DataHandler(httpPart));
                    }
                }
                catch (Exception e)
                {
                    throw new MessagingException(muleEvent, e);
                }
            }
            else if (responseContentType.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.toLowerCase()))
            {
                setResponsePayload(HttpParser.decodeString(IOUtils.toString(responseInputStream), encoding), muleEvent);
            }
            else
            {
                setResponsePayload(responseInputStream, muleEvent);
            }
        }
        else
        {
            setResponsePayload(responseInputStream, muleEvent);
        }

    }

    /**
     * Stores the response payload (body of the HTTP response) in the Mule message according to the "target"
     * property. If empty, it will be stored in the payload. If not, it will use the target expression to enrich
     * the message with the body of the response.
     */
    private void setResponsePayload(Object payload, MuleEvent muleEvent)
    {
        if (StringUtils.isEmpty(requester.getTarget()) || DefaultHttpRequester.DEFAULT_PAYLOAD_EXPRESSION.equals(requester.getTarget()) )
        {
            muleEvent.getMessage().setPayload(payload);
        }
        else
        {
            muleContext.getExpressionManager().enrich(requester.getTarget(), muleEvent, payload);
        }
    }


}
