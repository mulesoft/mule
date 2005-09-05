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

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.NullPayload;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Utility;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;

/**
 * <code>ObjectToHttpClientMethodRequest</code> transforms a UMOMessage into a
 * HttpClient HttpMethod that represents an HttpRequest.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ObjectToHttpClientMethodRequest extends AbstractEventAwareTransformer
{
    private List requestHeaders = null;
    private List responseHeaders = null;

    public ObjectToHttpClientMethodRequest()
    {
        setReturnClass(HttpMethod.class);
        requestHeaders = new ArrayList(Arrays.asList(HttpConstants.REQUEST_HEADER_NAMES));
        responseHeaders = new ArrayList(Arrays.asList(HttpConstants.RESPONSE_HEADER_NAMES));
    }
    private int addParameters(String queryString, PostMethod postMethod)
    {
        //Parse the HTTP argument list and convert to a NameValuePair collection

        if(queryString==null || queryString.length()==0) return 0;

        String currentParam;
        int equals;
        equals = queryString.indexOf("&");
        if(equals > -1) {
            currentParam = queryString.substring(0, equals);
            queryString = queryString.substring(equals + 1);
        } else {
            currentParam = queryString;
            queryString = "";
        }
        int parameterIndex = -1;
        while (currentParam != "") {
            String paramName, paramValue;
            equals = currentParam.indexOf("=");
            if (equals > -1) {
                paramName = currentParam.substring(0, equals);
                paramValue = currentParam.substring(equals + 1);
                parameterIndex ++;
                postMethod.addParameter(paramName, paramValue);
            }
            equals = queryString.indexOf("&");
            if(equals > -1) {
                currentParam = queryString.substring(0, equals);
                queryString = queryString.substring(equals + 1);
            } else {
                currentParam = queryString;
                queryString = "";
            }
        }
        return parameterIndex + 1;
    }

    public Object transform(Object src, UMOEventContext context) throws TransformerException
    {
        String endpoint = (String) context.getProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);
        if (endpoint == null) {
            throw new TransformerException(new Message(Messages.EVENT_PROPERTY_X_NOT_SET_CANT_PROCESS_REQUEST,
                                                       MuleProperties.MULE_ENDPOINT_PROPERTY), this);
        }
        String method = (String) context.getProperty(HttpConnector.HTTP_METHOD_PROPERTY, "POST");

        try {
            URI uri = new URI(endpoint);
            HttpMethod httpMethod = null;

            if (HttpConstants.METHOD_GET.equals(method)) {
                httpMethod = new GetMethod(uri.toString());
                setHeaders(httpMethod, context);
                String paramName = (String) context.getProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY,
                                                                HttpConnector.DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY);

                String query = uri.getQuery();
                if (query == null) {
                    query = paramName + "=" + src.toString();
                } else {
                    query += "&" + paramName + "=" + src.toString();
                }
                httpMethod.setQueryString(query);

            } else {
                PostMethod postMethod = new PostMethod(uri.toString());
                setHeaders(postMethod, context);
                String paramName = (String) context.getProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY);
                postMethod.setRequestContentLength(PostMethod.CONTENT_LENGTH_AUTO);
                if (paramName == null) {
                    //Call method to manage the parameter array
                    addParameters(uri.getQuery(), postMethod);
                    //Dont set a POST payload if the body is a Null Payload.  This way client calls
                    //can control if a POST body is posted explicitly
                   if(!(context.getMessage().getPayload() instanceof NullPayload)) {
                        if (src instanceof String) {
                            postMethod.setRequestBody(new ByteArrayInputStream(src.toString().getBytes()));
                        } else {
                            byte[] buffer = Utility.objectToByteArray(src);
                            postMethod.setRequestBody(new ByteArrayInputStream(buffer));
                        }
                    }
                } else {
                    postMethod.addParameter(paramName, src.toString());
                }

                httpMethod = postMethod;

            }

            return httpMethod;
        } catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }

    protected void setHeaders(HttpMethod httpMethod, UMOEventContext context) {
        // Standard requestHeaders
            Map.Entry header;
            String headerName;
            Map p = context.getProperties();
            for (Iterator iterator = p.entrySet().iterator(); iterator.hasNext();) {
                header = (Map.Entry) iterator.next();
                headerName = header.getKey().toString();
                if (!responseHeaders.contains(headerName) && header.getValue() instanceof String) {
                    if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX)) {
                        headerName = "X-" + headerName;
                    }
                    //Make sure we have a valid header name otherwise we will corrupt the request
                    if (headerName.startsWith("Content-Length") && httpMethod.getResponseHeader("Content-Length")==null) {
                        httpMethod.addRequestHeader(headerName, (String) header.getValue());
                    }else {
                        httpMethod.addRequestHeader(headerName, (String) header.getValue());
                    }
                }
            }
    }
}
