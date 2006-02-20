/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.http.transformers;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.providers.NullPayload;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.HttpResponse;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Utility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>UMOMessageToHttpResponse</code> converts a UMOMEssage into an Http
 * response.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UMOMessageToHttpResponse extends AbstractEventAwareTransformer {

    public static final String CUSTOM_HEADER_PREFIX = "X-";
    private SimpleDateFormat format = null;
    private String server = null;

    public UMOMessageToHttpResponse() {
        registerSourceType(Object.class);
        setReturnClass(Object.class);

        format = new SimpleDateFormat(HttpConstants.DATE_FORMAT);

        //When running with the source code, Meta information is not set
        //so product name and version are not available, hence we hard code
        if (MuleManager.getConfiguration().getProductName() == null) {
            server = "Mule/SNAPSHOT";
        } else {
            server = MuleManager.getConfiguration().getProductName() + "/"
                    + MuleManager.getConfiguration().getProductVersion();
        }
    }

    public Object transform(Object src, UMOEventContext context) throws TransformerException {
        //Note this transformer excepts Null as we must always return a result from the Http
        //connector if a response transformer is present
        if (src instanceof NullPayload) src = Utility.EMPTY_STRING;

        try {
            HttpResponse response = null;
            if(src instanceof HttpResponse) {
                response = (HttpResponse)src;
            } else {
                response = createResponse(src, context);
            }

            // Ensure there's a content type header
            if (!response.containsHeader(HttpConstants.HEADER_CONTENT_TYPE)) {
                response.addHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE));
            }

            // Ensure there's a content length or transfer encoding header
            if (!response.containsHeader(HttpConstants.HEADER_CONTENT_LENGTH) && !response.containsHeader(HttpConstants.HEADER_TRANSFER_ENCODING)) {
                InputStream content = response.getBody();
                if (content != null) {
                    long len = response.getContentLength();
                    if (len < 0) {
                        if (response.getHttpVersion().lessEquals(HttpVersion.HTTP_1_0)) {
                            throw new IOException("Chunked encoding not supported for HTTP version "
                                    + response.getHttpVersion());
                        }
                        Header header = new Header(HttpConstants.HEADER_TRANSFER_ENCODING, "chunked");
                        response.addHeader(header);
                    } else {
                        Header header = new Header(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(len));
                        response.setHeader(header);
                    }
                } else {
                    Header header = new Header(HttpConstants.HEADER_CONTENT_LENGTH, "0");
                    response.addHeader(header);
                }
            }

            if (!response.containsHeader(HttpConstants.HEADER_CONNECTION)) {
                // See if the the client explicitly handles connection persistence
                String connHeader = context.getStringProperty(HttpConstants.HEADER_CONNECTION);
                if (connHeader != null) {
                    if (connHeader.equalsIgnoreCase("keep-alive")) {
                        Header header = new Header(HttpConstants.HEADER_CONNECTION, "keep-alive");
                        response.addHeader(header);
                        response.setKeepAlive(true);
                    }
                    if (connHeader.equalsIgnoreCase("close")) {
                        Header header = new Header(HttpConstants.HEADER_CONNECTION, "close");
                        response.addHeader(header);
                        response.setKeepAlive(false);
                    }
                } else {
                    // Use protocol default connection policy
                    if (response.getHttpVersion().greaterEquals(HttpVersion.HTTP_1_1)) {
                        response.setKeepAlive(true);
                    } else {
                        response.setKeepAlive(false);
                    }
                }
            }
            if ("HEAD".equalsIgnoreCase(context.getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY))) {
                // this is a head request, we don't want to send the actualy content
                response.setBody(null);
            }
            return response;
        } catch (IOException e) {
            throw new TransformerException(this, e);
        }

    }

    protected HttpResponse createResponse(Object src, UMOEventContext context) throws IOException {
        HttpResponse response = new HttpResponse();

        int status = context.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, HttpConstants.SC_OK);
        String version = context.getStringProperty(HttpConnector.HTTP_VERSION_PROPERTY, HttpConstants.HTTP11);
        String date = format.format(new Date());
        String contentType = context.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE);

        response.setStatusLine(HttpVersion.parse(version), status);
        response.setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, contentType));
        response.setHeader(new Header(HttpConstants.HEADER_DATE, date));
        response.setHeader(new Header(HttpConstants.HEADER_SERVER, server));
        if (context.getProperty(HttpConstants.HEADER_EXPIRES) == null) {
            response.setHeader(new Header(HttpConstants.HEADER_EXPIRES, date));
        }

        String headerName;
        String value;
        for (Iterator iterator = HttpConstants.RESPONSE_HEADER_NAMES.values().iterator(); iterator.hasNext();) {
            headerName = (String) iterator.next();
            value = context.getStringProperty(headerName);
            if (value != null) {
                response.setHeader(new Header(headerName, value));
            }
        }
        // Custom responseHeaderNames
        Map customHeaders = (Map) context.getProperty(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY);
        if (customHeaders != null) {
            Map.Entry entry;
            for (Iterator iterator = customHeaders.entrySet().iterator(); iterator.hasNext();) {
                entry = (Map.Entry) iterator.next();
                if (entry.getValue() != null) {
                    response.setHeader(new Header(entry.getKey().toString(), entry.getValue().toString()));
                }
            }
        }

        // Mule properties
        UMOMessage m = context.getMessage();
        String user = (String) m.getProperty(MuleProperties.MULE_USER_PROPERTY);
        if (user != null) {
            response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MuleProperties.MULE_USER_PROPERTY, user));
        }
        if (m.getCorrelationId() != null) {
            response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MuleProperties.MULE_CORRELATION_ID_PROPERTY, m.getCorrelationId()));
            response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, String.valueOf(m.getCorrelationGroupSize())));
            response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, String.valueOf(m.getCorrelationSequence())));
        }
        if (m.getReplyTo() != null) {
            response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MuleProperties.MULE_REPLY_TO_PROPERTY, m.getReplyTo().toString()));
        }
        if(src instanceof InputStream) {
            response.setBody((InputStream)src);
        } else if (src instanceof String) {
            response.setBodyString(src.toString());
        } else {
            response.setBody(new ByteArrayInputStream(Utility.objectToByteArray(src)));
        }
        return response;
    }

    public boolean isAcceptNull() {
        return true;
    }
}
