/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.config.MuleManifest;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;
import org.mule.transport.NullPayload;
import org.mule.util.DataTypeUtils;
import org.mule.transport.http.CookieHelper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Converts a {@link MuleMessage} into an Http response.
 */
public class MuleMessageToHttpResponse extends AbstractMessageTransformer
{
    
    private static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(HttpConstants.DATE_FORMAT_RFC822).withLocale(Locale.US);
    
    public static String formatDate(long time)
    {
        return dateFormatter.print(time);
    }

    private String server;

    public MuleMessageToHttpResponse()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.create(HttpResponse.class));
    }

    @Override
    public void initialise() throws InitialisationException
    {
        // When running with the source code, Meta information is not set
        // so product name and version are not available, hence we hard code
        if (MuleManifest.getProductName() == null)
        {
            server = "Mule/SNAPSHOT";
        }
        else
        {
            server = MuleManifest.getProductName() + "/" + MuleManifest.getProductVersion();
        }

        if(HttpConstants.SERVER_TIME_ZONE_PROPERTY.isEnabled())
        {
            logger.warn(HttpMessages.dateInServerTimeZone());
            dateFormatter = dateFormatter.withZone(DateTimeZone.forID(TimeZone.getDefault().getID()));
        }
        else
        {
            dateFormatter = dateFormatter.withZone(DateTimeZone.forID("GMT"));
        }
    }

    @Override
    public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException
    {
        Object src = msg.getPayload();

        // Note this transformer excepts Null as we must always return a result
        // from the Http
        // connector if a response transformer is present
        if (src instanceof NullPayload)
        {
            src = StringUtils.EMPTY;
        }

        try
        {
            HttpResponse response;
            if (src instanceof HttpResponse)
            {
                response = (HttpResponse) src;
            }
            else
            {
                response = createResponse(src, outputEncoding, msg);
            }

            // Ensure there's a content type header
            if (!response.containsHeader(HttpConstants.HEADER_CONTENT_TYPE))
            {
                response.addHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE,
                        HttpConstants.DEFAULT_CONTENT_TYPE));
            }

            // Ensure there's a content length or transfer encoding header
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

                            Header header = new Header(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(len));
                            response.setHeader(header);
                        }
                        else
                        {
                            Header header = new Header(HttpConstants.HEADER_TRANSFER_ENCODING, "chunked");
                            response.addHeader(header);
                        }
                    }
                    else
                    {
                        Header header = new Header(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(len));
                        response.setHeader(header);
                    }
                }
                else
                {
                    Header header = new Header(HttpConstants.HEADER_CONTENT_LENGTH, "0");
                    response.addHeader(header);
                }
            }

            // See if the the client explicitly handles connection persistence
            String connHeader = msg.getOutboundProperty(HttpConstants.HEADER_CONNECTION);
            if (connHeader != null)
            {
                if (connHeader.equalsIgnoreCase("keep-alive"))
                {
                    response.setKeepAlive(true);
                }
                if (connHeader.equalsIgnoreCase("close"))
                {
                    response.setKeepAlive(false);
                }
            }

            final String method = msg.getOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY);
            if ("HEAD".equalsIgnoreCase(method))
            {
                // this is a head request, we don't want to send the actual content
                response.setBody((MuleMessage) null);
            }
            return response;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

    }

    protected HttpResponse createResponse(Object src, String encoding, MuleMessage msg)
            throws IOException, TransformerException
    {
        HttpResponse response = new HttpResponse();

        Object tmp = msg.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        int status = HttpConstants.SC_OK;

        if (tmp != null)
        {
            status = Integer.valueOf(tmp.toString());
        }
        else if (msg.getExceptionPayload() != null)
        {
            status = HttpConstants.SC_INTERNAL_SERVER_ERROR;
        }

        String version = msg.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY);
        if (version == null)
        {
            version = HttpConstants.HTTP11;
        }

        String contentType = msg.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        if (contentType == null)
        {
            DataType<?> dataType = msg.getDataType();
            if (!MimeTypes.ANY.equals(dataType.getMimeType()))
            {
                contentType = DataTypeUtils.getContentType(dataType);
            }
            else
            {
                contentType = msg.getInvocationProperty(HttpConstants.HEADER_CONTENT_TYPE);
            }
        }

        response.setStatusLine(HttpVersion.parse(version), status);
        if (contentType != null)
        {
            response.setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, contentType));
        }
        setDateHeader(response, System.currentTimeMillis());
        response.setHeader(new Header(HttpConstants.HEADER_SERVER, server));

        String etag = msg.getOutboundProperty(HttpConstants.HEADER_ETAG);
        if (etag != null)
        {
            response.setHeader(new Header(HttpConstants.HEADER_ETAG, etag));
        }
        response.setFallbackCharset(encoding);

        Collection<String> headerNames = new LinkedList<String>();
        headerNames.addAll(HttpConstants.RESPONSE_HEADER_NAMES.values());
        headerNames.addAll(HttpConstants.GENERAL_AND_ENTITY_HEADER_NAMES.values());

        for (String headerName : headerNames)
        {
            if (HttpConstants.HEADER_COOKIE_SET.equals(headerName))
            {
                // TODO This have to be improved. We shouldn't have to look in all
                // scopes
                Object cookiesObject = msg.getInvocationProperty(headerName);
                if (cookiesObject == null)
                {
                    cookiesObject = msg.getOutboundProperty(headerName);
                }
                if (cookiesObject == null)
                {
                    cookiesObject = msg.getInboundProperty(headerName);
                }
                if (cookiesObject == null)
                {
                    continue;
                }

                if (!(cookiesObject instanceof Cookie[]))
                {
                    response.addHeader(new Header(headerName, cookiesObject.toString()));
                }
                else
                {
                    Cookie[] arrayOfCookies = CookieHelper.asArrayOfCookies(cookiesObject);
                    for (Cookie cookie : arrayOfCookies)
                    {
                        response.addHeader(new Header(headerName,
                            CookieHelper.formatCookieForASetCookieHeader(cookie)));
                    }
                }
            }
            else
            {
                Object value = msg.getOutboundProperty(headerName);
                if (value == null)
                {
                    value = msg.getInvocationProperty(headerName);
                }
                if (value != null)
                {
                    response.setHeader(new Header(headerName, value.toString()));
                }
            }
        }
        
        Map customHeaders = msg.getOutboundProperty(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY);
        if (customHeaders != null)
        {
            throw new TransformerException(HttpMessages.customHeaderMapDeprecated(), this);
        }

        //attach the outbound properties to the message
        for (String headerName : msg.getOutboundPropertyNames())
        {
            if (response.getFirstHeader(headerName) != null)
            {
                //keep headers already set
                continue;
            }
            Object v = msg.getOutboundProperty(headerName);
            if (v != null)
            {
                if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX))
                {
                    headerName = HttpConstants.CUSTOM_HEADER_PREFIX + headerName;
                }
                response.setHeader(new Header(headerName, v.toString()));
            }
        }

        if (msg.getCorrelationId() != null)
        {
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_CORRELATION_ID_PROPERTY,
                    msg.getCorrelationId()));
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX
                    + MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                    String.valueOf(msg.getCorrelationGroupSize())));
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX
                    + MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                    String.valueOf(msg.getCorrelationSequence())));
        }
        if (msg.getReplyTo() != null)
        {
            response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_REPLY_TO_PROPERTY,
                    msg.getReplyTo().toString()));
        }

        try
        {
            response.setBody(msg);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        return response;
    }

    protected void setDateHeader(HttpResponse response, long millis)
    {
        response.setHeader(new Header(HttpConstants.HEADER_DATE, formatDate(millis)));
    }

    @Override
    public boolean isAcceptNull()
    {
        return true;
    }
}
