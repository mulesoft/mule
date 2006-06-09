/*
 * $Id$
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

import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.NullPayload;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>ObjectToHttpClientMethodRequest</code> transforms a UMOMessage into a
 * HttpClient HttpMethod that represents an HttpRequest.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ObjectToHttpClientMethodRequest extends AbstractEventAwareTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5726306151419912371L;

    private SerializableToByteArray serializableToByteArray;

    public ObjectToHttpClientMethodRequest()
    {
        setReturnClass(HttpMethod.class);
        serializableToByteArray = new SerializableToByteArray();
    }

    private int addParameters(String queryString, PostMethod postMethod)
    {
        // Parse the HTTP argument list and convert to a NameValuePair
        // collection

        if (StringUtils.isEmpty(queryString)) {
            return 0;
        }

        String currentParam;
        int equals;
        equals = queryString.indexOf("&");
        if (equals > -1) {
            currentParam = queryString.substring(0, equals);
            queryString = queryString.substring(equals + 1);
        }
        else {
            currentParam = queryString;
            queryString = StringUtils.EMPTY;
        }
        int parameterIndex = -1;
        while (StringUtils.isNotBlank(currentParam)) {
            String paramName, paramValue;
            equals = currentParam.indexOf("=");
            if (equals > -1) {
                paramName = currentParam.substring(0, equals);
                paramValue = currentParam.substring(equals + 1);
                parameterIndex++;
                postMethod.addParameter(paramName, paramValue);
            }
            equals = queryString.indexOf("&");
            if (equals > -1) {
                currentParam = queryString.substring(0, equals);
                queryString = queryString.substring(equals + 1);
            }
            else {
                currentParam = queryString;
                queryString = StringUtils.EMPTY;
            }
        }
        return parameterIndex + 1;
    }

    public Object transform(Object src, String encoding, UMOEventContext context)
            throws TransformerException
    {
        UMOMessage msg = context.getMessage();

        String endpoint = msg.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);
        if (endpoint == null) {
            throw new TransformerException(new Message(
                    Messages.EVENT_PROPERTY_X_NOT_SET_CANT_PROCESS_REQUEST,
                    MuleProperties.MULE_ENDPOINT_PROPERTY), this);
        }

        String method = msg.getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
        try {
            URI uri = new URI(endpoint);
            HttpMethod httpMethod = null;

            if (HttpConstants.METHOD_GET.equals(method)) {
                httpMethod = new GetMethod(uri.toString());
                setHeaders(httpMethod, context);
                String paramName = msg.getStringProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY,
                        HttpConnector.DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY);
                String query = uri.getQuery();
                if (!(src instanceof NullPayload) && !StringUtils.EMPTY.equals(src)) {
                    if (query == null) {
                        query = paramName + "=" + src.toString();
                    }
                    else {
                        query += "&" + paramName + "=" + src.toString();
                    }
                }
                httpMethod.setQueryString(query);

            }
            else {
                PostMethod postMethod = new PostMethod(uri.toString());
                setHeaders(postMethod, context);
                String paramName = msg.getStringProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY, null);
                // postMethod.setRequestContentLength(PostMethod.CONTENT_LENGTH_AUTO);
                if (paramName == null) {
                    // Call method to manage the parameter array
                    addParameters(uri.getQuery(), postMethod);
                    // Dont set a POST payload if the body is a Null Payload.
                    // This way client calls
                    // can control if a POST body is posted explicitly
                    if (!(context.getMessage().getPayload() instanceof NullPayload)) {
                        // See if we have a MIME type set
                        String mimeType = msg.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, null);

                        if (src instanceof String) {
                            // Ensure that we strip the encoding information from the encoding type
                            if (mimeType != null) {
                                int parameterIndex = mimeType.indexOf(";");
                                if (parameterIndex > 0) {
                                    mimeType = mimeType.substring(0, parameterIndex);
                                }
                            }
                            if (mimeType == null) mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                            if (encoding == null) encoding = MuleManager.getConfiguration().getEncoding();
                            postMethod.setRequestEntity(new StringRequestEntity(src.toString(), mimeType, encoding));
                        }
                        else if (src instanceof InputStream) {
                            // TODO Danger here! We don't know if the content is really text or not
                            if (mimeType == null) mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                            postMethod.setRequestEntity(new InputStreamRequestEntity((InputStream)src, mimeType));
                        }
                        else {
                            // TODO Danger here! We don't know if the content is really text or not
                            if (mimeType == null) mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                            byte[] buffer = (byte[])serializableToByteArray.doTransform(src, encoding);
                            postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer, mimeType));
                        }
                    }
                }
                else {
                    postMethod.addParameter(paramName, src.toString());
                }

                httpMethod = postMethod;

            }

            return httpMethod;
        }
        catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }

    protected void setHeaders(HttpMethod httpMethod, UMOEventContext context)
    {
        // Standard requestHeaders
        String headerValue;
        String headerName;
        UMOMessage msg = context.getMessage();
        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();) {
            headerName = (String)iterator.next();
            headerValue = msg.getStringProperty(headerName, null);
            if (HttpConstants.REQUEST_HEADER_NAMES.get(headerName) == null) {
                if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX)) {
                    headerName = new StringBuffer(30).append("X-").append(headerName).toString();
                }
                // Make sure we have a valid header name otherwise we will
                // corrupt the request
                if (headerName.startsWith(HttpConstants.HEADER_CONTENT_LENGTH)
                        && httpMethod.getResponseHeader(HttpConstants.HEADER_CONTENT_LENGTH) == null) {
                    httpMethod.addRequestHeader(headerName, headerValue);
                }
                else {
                    // TODO why is this the same code as the previous branch?
                    httpMethod.addRequestHeader(headerName, headerValue);
                }
            }
        }

        if (context.getMessage().getPayload() instanceof InputStream) {
            // must set this for receiver to properly parse attachments
            httpMethod.addRequestHeader(HttpConstants.HEADER_CONTENT_TYPE, "multipart/related");
        }
    }
}
