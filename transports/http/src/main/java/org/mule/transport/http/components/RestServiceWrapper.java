/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.components;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.component.AbstractComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.transport.NullPayload;
import org.mule.util.expression.ExpressionEvaluator;
import org.mule.util.expression.ExpressionEvaluatorManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This service can used to proxy REST style services as local Mule Components. It
 * can be configured with a service URL plus a number of properties that allow you to
 * configure the parameters and error conditions on the service.
 */
public class RestServiceWrapper extends AbstractComponent
{

    public static final String REST_SERVICE_URL = "rest.service.url";
    public static final String GET = "GET";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";
    public static final String HTTP_METHOD = "http.method";

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private String serviceUrl;
    private boolean urlFromMessage = false;
    private Map requiredParams = new HashMap();
    private Map optionalParams = new HashMap();
    private String httpMethod = "GET";
    private List payloadParameterNames;
    private Filter errorFilter;
    private String errorExpression;

    public String getServiceUrl()
    {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl)
    {
        this.serviceUrl = serviceUrl;
    }

    public boolean isUrlFromMessage()
    {
        return urlFromMessage;
    }

    public void setUrlFromMessage(boolean urlFromMessage)
    {
        this.urlFromMessage = urlFromMessage;
    }

    public Map getRequiredParams()
    {
        return requiredParams;
    }

    /**
     * Required params that are pulled from the message. If these params don't exist
     * the call will fail Note that you can use
     * {@link org.mule.util.expression.ExpressionEvaluator} expressions such as
     * xpath, header, xquery, etc
     * 
     * @param requiredParams
     */
    public void setRequiredParams(Map requiredParams)
    {
        this.requiredParams = requiredParams;
    }

    /**
     * Optional params that are pulled from the message. If these params don't exist
     * execution will continue. Note that you can use {@link ExpressionEvaluator}
     * expressions such as xpath, header, xquery, etc
     * 
     * @param requiredParams
     */
    public Map getOptionalParams()
    {
        return optionalParams;
    }

    public void setOptionalParams(Map optionalParams)
    {
        this.optionalParams = optionalParams;
    }

    public String getHttpMethod()
    {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    public List getPayloadParameterNames()
    {
        return payloadParameterNames;
    }

    public void setPayloadParameterNames(List payloadParameterNames)
    {
        this.payloadParameterNames = payloadParameterNames;
    }

    public Filter getFilter()
    {
        return errorFilter;
    }

    public void setFilter(Filter errorFilter)
    {
        this.errorFilter = errorFilter;
    }

    public String getErrorExpression()
    {
        return errorExpression;
    }

    public void setErrorExpression(String errorExpression)
    {
        this.errorExpression = errorExpression;
    }

    protected void doInitialise() throws InitialisationException
    {
        if (serviceUrl == null && !urlFromMessage)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("serviceUrl"), this);
        }
        else if (serviceUrl != null)
        {
            try
            {
                new URL(serviceUrl);
            }
            catch (MalformedURLException e)
            {
                throw new InitialisationException(e, this);
            }
        }

        if (errorFilter == null)
        {
            if (errorExpression == null)
            {
                // We'll set a default filter that checks the return code
                errorFilter = new MessagePropertyFilter("http.status!=200");
                logger.info("Setting default error filter to MessagePropertyFilter('http.status!=200')");
            }
            else
            {
                errorFilter = new RegExFilter(errorExpression);
            }
        }
    }

    public MuleMessage doOnCall(MuleEvent event) throws Exception
    {
        String tempUrl;
        MuleMessage result = null;

        Object request = event.transformMessage();
        Object requestBody;
        if (urlFromMessage)
        {
            tempUrl = event.getMessage().getStringProperty(REST_SERVICE_URL, null);
            if (tempUrl == null)
            {
                throw new IllegalArgumentException(CoreMessages.propertyIsNotSetOnEvent(REST_SERVICE_URL)
                    .toString());
            }
        }
        else
        {
            tempUrl = serviceUrl;
        }
        StringBuffer urlBuffer = new StringBuffer(tempUrl);

        if (GET.equalsIgnoreCase(this.httpMethod))
        {
            requestBody = NullPayload.getInstance();

            setRESTParams(urlBuffer, event.getMessage(), request, requiredParams, false, null);
            setRESTParams(urlBuffer, event.getMessage(), request, optionalParams, true, null);
        }
        else
        // if post
        {
            StringBuffer requestBodyBuffer = new StringBuffer();
            event.getMessage().setProperty(CONTENT_TYPE, CONTENT_TYPE_VALUE);
            setRESTParams(urlBuffer, event.getMessage(), request, requiredParams, false, requestBodyBuffer);
            setRESTParams(urlBuffer, event.getMessage(), request, optionalParams, true, requestBodyBuffer);
            requestBody = requestBodyBuffer.toString();
        }

        tempUrl = urlBuffer.toString();
        logger.info("Invoking REST service: " + tempUrl);

        event.getMessage().setProperty(HTTP_METHOD, httpMethod);

        result = RequestContext.getEventContext().sendEvent(
            new DefaultMuleMessage(requestBody, event.getMessage()), tempUrl);
        if (isErrorPayload(result))
        {
            handleException(new RestServiceException(CoreMessages.failedToInvokeRestService(tempUrl), result),
                result);
        }

        return result;
    }

    private String getSeparator(String url)
    {
        String sep;

        if (url.indexOf("?") > -1)
        {
            sep = "&";
        }
        else
        {
            sep = "?";
        }

        return sep;
    }

    private String updateSeparator(String sep)
    {
        if (sep.compareTo("?") == 0 || sep.compareTo("") == 0)
        {
            return ("&");
        }

        return sep;
    }

    // if requestBodyBuffer is null, it means that the request is a GET, otherwise it
    // is a POST and
    // requestBodyBuffer must contain the body of the http method at the end of this
    // function call
    private void setRESTParams(StringBuffer url,
                               MuleMessage msg,
                               Object body,
                               Map args,
                               boolean optional,
                               StringBuffer requestBodyBuffer)
    {
        String sep;

        if (requestBodyBuffer == null)
        {
            sep = getSeparator(url.toString());
        }
        else
        {
            sep = "";
        }

        for (Iterator iterator = args.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            String exp = (String) entry.getValue();
            Object value = ExpressionEvaluatorManager.evaluate(exp, msg);

            if (value == null)
            {
                if (!optional)
                {
                    throw new IllegalArgumentException(CoreMessages.propertyIsNotSetOnEvent(exp).toString());
                }
            }
            else if (requestBodyBuffer != null) // implies this is a POST
            {
                requestBodyBuffer.append(sep);
                requestBodyBuffer.append(name).append('=').append(value);
            }
            else
            {
                url.append(sep);
                url.append(name).append('=').append(value);
            }

            sep = updateSeparator(sep);
        }

        if (!optional && payloadParameterNames != null)
        {
            if (body instanceof Object[])
            {
                Object[] requestArray = (Object[]) body;
                for (int i = 0; i < payloadParameterNames.size(); i++)
                {
                    if (requestBodyBuffer != null)
                    {
                        requestBodyBuffer.append(sep).append(payloadParameterNames.get(i)).append('=').append(
                            requestArray[i].toString());
                    }
                    else
                    {
                        url.append(sep).append(payloadParameterNames.get(i)).append('=').append(
                            requestArray[i].toString());
                    }

                    sep = updateSeparator(sep);
                }
            }
            else
            {
                if (payloadParameterNames.get(0) != null)
                {
                    if (requestBodyBuffer != null)
                    {
                        requestBodyBuffer.append(payloadParameterNames.get(0)).append('=').append(body.toString());
                    }
                    else
                    {
                        url.append(sep).append(payloadParameterNames.get(0)).append('=').append(body.toString());
                    }
                }
            }
        }
    }

    protected boolean isErrorPayload(MuleMessage message)
    {
        return errorFilter != null && errorFilter.accept(message);
    }

    protected void handleException(RestServiceException e, MuleMessage result) throws Exception
    {
        throw e;
    }

    // @Override
    protected void doOnEvent(MuleEvent event)
    {
        try
        {
            onCall(event);
        }
        catch (MuleException e)
        {
            logger.error(e);
        }
    }

    // @Override
    protected void doDispose()
    {
        // no-op
    }

    // @Override
    protected void doStart() throws MuleException
    {
        // no-op
    }

    // @Override
    protected void doStop() throws MuleException
    {
        // no-op
    }
}
