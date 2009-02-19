/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.StreamPayloadRequestEntity;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.SerializationUtils;

/**
 * <code>ObjectToHttpClientMethodRequest</code> transforms a MuleMessage into a
 * HttpClient HttpMethod that represents an HttpRequest.
 */

public class ObjectToHttpClientMethodRequest extends AbstractMessageAwareTransformer implements MuleContextAware
{

    private MuleContext muleContext;

    public ObjectToHttpClientMethodRequest()
    {
        setReturnClass(HttpMethod.class);
        registerSourceType(MuleMessage.class);
        registerSourceType(byte[].class);
        registerSourceType(String.class);
        registerSourceType(InputStream.class);
        registerSourceType(OutputHandler.class);
        registerSourceType(NullPayload.class);
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    protected int addParameters(String queryString, PostMethod postMethod, MuleMessage msg)
    {
        // Parse the HTTP argument list and convert to a NameValuePair
        // collection

        if (StringUtils.isBlank(queryString))
        {
            return 0;
        }

        String currentParam;
        int equals;
        equals = queryString.indexOf("&");
        if (equals > -1)
        {
            currentParam = queryString.substring(0, equals);
            queryString = queryString.substring(equals + 1);
        }
        else
        {
            currentParam = queryString;
            queryString = StringUtils.EMPTY;
        }
        int parameterIndex = -1;
        while (StringUtils.isNotBlank(currentParam))
        {
            String paramName, paramValue;
            equals = currentParam.indexOf("=");
            if (equals > -1)
            {
                paramName = currentParam.substring(0, equals);
                paramValue = currentParam.substring(equals + 1);
                //Run query params through the expression evaluator
//                Object temp = DefaultExpressionManager.evaluate(paramValue, msg, "$[", true);
//                if (temp != null)
//                {
//                    //Process param collections
//                    if (temp instanceof List)
//                    {
//                        StringBuffer buf = new StringBuffer();
//                        List list = (List) temp;
//                        for (Iterator iterator = list.iterator(); iterator.hasNext();)
//                        {
//                            Object object = iterator.next();
//                            buf.append(object).append(",");
//                        }
//                        parameterIndex++;
//                        postMethod.addParameter(paramName, buf.toString());
//                    }
//                    else if (temp instanceof Map)
//                    {
//                        Map map = (Map) temp;
//                        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();)
//                        {
//                            Map.Entry entry = (Map.Entry) iterator.next();
//                            parameterIndex++;
//                            postMethod.addParameter(entry.getKey().toString(), entry.getValue().toString());
//                        }
//                    }
//                    else
//                    {
//                        parameterIndex++;
//                        postMethod.addParameter(paramName, temp.toString());
//                    }
//                }
//                else
//                {
                    parameterIndex++;
                    postMethod.addParameter(paramName, paramValue);
                //}


            }
            equals = queryString.indexOf("&");
            if (equals > -1)
            {
                currentParam = queryString.substring(0, equals);
                queryString = queryString.substring(equals + 1);
            }
            else
            {
                currentParam = queryString;
                queryString = StringUtils.EMPTY;
            }
        }
        return parameterIndex + 1;
    }

    public Object transform(MuleMessage msg, String outputEncoding) throws TransformerException
    {
        Object src = msg.getPayload();

        String endpoint = msg.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);
        if (endpoint == null)
        {
            throw new TransformerException(
                    HttpMessages.eventPropertyNotSetCannotProcessRequest(
                            MuleProperties.MULE_ENDPOINT_PROPERTY), this);
        }

        String method = msg.getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
        try
        {
            //Allow Expressions to be embedded
            endpoint = endpoint.replaceAll("%23", "#");
            endpoint = muleContext.getExpressionManager().parse(endpoint, msg, true);
            URI uri = new URI(endpoint);
            HttpMethod httpMethod;

            if (HttpConstants.METHOD_GET.equals(method))
            {
                httpMethod = new GetMethod(uri.toString());
                String paramName = URLEncoder.encode(msg.getStringProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY,
                                                                           HttpConnector.DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY), outputEncoding);
                String paramValue = URLEncoder.encode(src.toString(), outputEncoding);

                String query = uri.getRawQuery();
                if (!(src instanceof NullPayload) && !StringUtils.EMPTY.equals(src))
                {
                    if (query == null)
                    {
                        query = paramName + "=" + paramValue;
                    }
                    else
                    {
                        query += "&" + paramName + "=" + paramValue;
                    }
                }
                httpMethod.setQueryString(query);

            }
            else if (HttpConstants.METHOD_POST.equalsIgnoreCase(method))
            {
                PostMethod postMethod = new PostMethod(uri.toString());
                String paramName = msg.getStringProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY, null);

                if (paramName == null)
                {
                    // Call method to manage the parameter array
                    addParameters(uri.getQuery(), postMethod, msg);
                    setupEntityMethod(src, outputEncoding, msg, uri, postMethod);
                }
                else
                {
                    postMethod.addParameter(paramName, src.toString());
                }

                httpMethod = postMethod;
            }
            else if (HttpConstants.METHOD_PUT.equalsIgnoreCase(method))
            {
                PutMethod putMethod = new PutMethod(uri.toString());

                setupEntityMethod(src, outputEncoding, msg, uri, putMethod);

                httpMethod = putMethod;
            }
            else if (HttpConstants.METHOD_DELETE.equalsIgnoreCase(method))
            {
                httpMethod = new DeleteMethod(uri.toString());
            }
            else if (HttpConstants.METHOD_HEAD.equalsIgnoreCase(method))
            {
                httpMethod = new HeadMethod(uri.toString());
            }
            else if (HttpConstants.METHOD_OPTIONS.equalsIgnoreCase(method))
            {
                httpMethod = new OptionsMethod(uri.toString());
            }
            else if (HttpConstants.METHOD_TRACE.equalsIgnoreCase(method))
            {
                httpMethod = new TraceMethod(uri.toString());
            }
            else
            {
                throw new TransformerException(HttpMessages.unsupportedMethod(method));
            }

            // Allow the user to set HttpMethodParams as an object on the message
            HttpMethodParams params = (HttpMethodParams) msg.removeProperty(HttpConnector.HTTP_PARAMS_PROPERTY);
            if (params != null)
            {
                httpMethod.setParams(params);
            }
            else
            {
                // TODO we should probably set other properties here
                String httpVersion = msg.getStringProperty(HttpConnector.HTTP_VERSION_PROPERTY,
                                                           HttpConstants.HTTP11);
                if (HttpConstants.HTTP10.equals(httpVersion))
                {
                    httpMethod.getParams().setVersion(HttpVersion.HTTP_1_0);
                }
                else
                {
                    httpMethod.getParams().setVersion(HttpVersion.HTTP_1_1);
                }
            }

            setHeaders(httpMethod, msg);

            return httpMethod;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void setupEntityMethod(Object src,
                                     String encoding,
                                     MuleMessage msg,
                                     URI uri,
                                     EntityEnclosingMethod postMethod)
            throws UnsupportedEncodingException, TransformerException
    {
        // Dont set a POST payload if the body is a Null Payload.
        // This way client calls
        // can control if a POST body is posted explicitly
        if (!(msg.getPayload() instanceof NullPayload))
        {
            // See if we have a MIME type set
            String mimeType = (String)msg.getProperty(HttpConstants.HEADER_CONTENT_TYPE, PropertyScope.OUTBOUND);

            if (src instanceof String)
            {
                // Ensure that we strip the encoding information from the
                // encoding type
                if (mimeType != null)
                {
                    int parameterIndex = mimeType.indexOf(";");
                    if (parameterIndex > 0)
                    {
                        mimeType = mimeType.substring(0, parameterIndex);
                    }
                }
                if (mimeType == null)
                {
                    mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                }

                postMethod.setRequestEntity(new StringRequestEntity(src.toString(), mimeType,
                        encoding));
                return;
            }


            if (mimeType == null)
            {
                mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
            }

            if (encoding != null
                    && !"UTF-8".equals(encoding.toUpperCase())
                    && mimeType.indexOf("charset") == -1)
            {
                mimeType += "; charset=" + encoding;
            }

            if (src instanceof InputStream)
            {
                // TODO Danger here! We don't know if the content is
                // really text or not
                if (mimeType == null)
                {
                    mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                }
                postMethod.setRequestEntity(new InputStreamRequestEntity((InputStream) src,
                        mimeType));
            }
            else if (src instanceof byte[])
            {
                if (mimeType == null)
                {
                    mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                }
                postMethod.setRequestEntity(new ByteArrayRequestEntity((byte[]) src,
                        mimeType));
            }
            else if (src instanceof OutputHandler)
            {
                MuleEvent event = RequestContext.getEvent();
                postMethod.setRequestEntity(new StreamPayloadRequestEntity((OutputHandler) src, event));
            }
            else
            {
                if (mimeType == null)
                {
                    mimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                }

                byte[] buffer = SerializationUtils.serialize((Serializable) src);
                postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer, mimeType));
            }
        }

    }

    protected void setHeaders(HttpMethod httpMethod, MuleMessage msg)
    {
        // Standard requestHeaders
        String headerValue;
        String headerName;

        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
        {
            headerName = (String) iterator.next();

            if (headerName.equalsIgnoreCase(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY))
            {
                if (logger.isInfoEnabled())
                {
                    logger.warn("Deprecation warning:  There is not need to set custom headers using: " + HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY
                        + " you can now add the properties directly to the outbound endpoint or use the OUTBOUND property scope on the message.");
                }
                
                Map customHeaders = (Map) msg.getProperty(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY);
                if (customHeaders != null)
                {   
                    for (Iterator headerItr = customHeaders.entrySet().iterator(); headerItr.hasNext();)
                    {
                        Map.Entry entry = (Map.Entry) headerItr.next();
                        if (entry.getValue() != null)
                        {
                            httpMethod.addRequestHeader(entry.getKey().toString(), entry.getValue().toString());
                        }
                    }
                }
            }
            else if (HttpConstants.REQUEST_HEADER_NAMES.get(headerName) == null
                        && !HttpConnector.HTTP_INBOUND_PROPERTIES.contains(headerName))
            {
                headerValue = msg.getStringProperty(headerName, null);
                if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX))
                {
                    headerName = new StringBuffer(30).append("X-").append(headerName).toString();
                }

                httpMethod.addRequestHeader(headerName, headerValue);
            }
        }

        Set attNams = msg.getAttachmentNames();
        if (msg.getPayload() instanceof InputStream
                && attNams != null && attNams.size() > 0)
        {
            // must set this for receiver to properly parse attachments
            httpMethod.addRequestHeader(HttpConstants.HEADER_CONTENT_TYPE, "multipart/related");
        }

    }

    protected String paramToString(Object param)
    {
        StringBuffer buf = new StringBuffer();
        if (param instanceof List)
        {
            List list = (List) param;
            for (Iterator iterator = list.iterator(); iterator.hasNext();)
            {
                Object object = iterator.next();
                buf.append(object).append(",");
            }
            return buf.toString();
        }
        else if (param instanceof Map)
        {
            Map map = (Map) param;
            for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iterator.next();
                buf.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            return buf.toString().substring(0, buf.length() - 1);
        }
        else
        {
            return param.toString();
        }
    }
}
