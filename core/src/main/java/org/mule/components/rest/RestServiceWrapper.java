/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.rest;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.properties.MessagePropertyExtractor;
import org.mule.util.properties.PropertyExtractor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This component can used to proxy REST style services as local Mule Components. It
 * can be configured with a service URL plus a number of properties that allow you to
 * configure the parameters and error conditions on the service.
 */
public class RestServiceWrapper implements Callable, Initialisable
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
    private UMOFilter errorFilter;
    private String errorExpression;

    private PropertyExtractor propertyExtractor = new MessagePropertyExtractor();

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

    public void setRequiredParams(Map requiredParams)
    {
        this.requiredParams = requiredParams;
    }

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

    public UMOFilter getErrorFilter()
    {
        return errorFilter;
    }

    public void setErrorFilter(UMOFilter errorFilter)
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

    public void initialise() throws InitialisationException
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

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        String tempUrl;
        Object request = eventContext.getTransformedMessage();
        Object requestBody;
        if (urlFromMessage)
        {
            tempUrl = eventContext.getMessage().getStringProperty(REST_SERVICE_URL, null);
            if (tempUrl == null)
            {
                throw new IllegalArgumentException(
                    CoreMessages.propertyIsNotSetOnEvent(REST_SERVICE_URL).toString());
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
            
            setRESTParams(urlBuffer, eventContext.getMessage(), request, requiredParams, false, null);
            setRESTParams(urlBuffer, eventContext.getMessage(), request, optionalParams, true, null);
        }
        else //if post
        {
            StringBuffer requestBodyBuffer = new StringBuffer();
            eventContext.getMessage().setProperty(CONTENT_TYPE, CONTENT_TYPE_VALUE);
            
            setRESTParams(urlBuffer, eventContext.getMessage(), request, requiredParams, false, requestBodyBuffer);
            setRESTParams(urlBuffer, eventContext.getMessage(), request, optionalParams, true, requestBodyBuffer);
            
            requestBody = requestBodyBuffer.toString();
        }

        tempUrl = urlBuffer.toString();
        logger.info("Invoking REST service: " + tempUrl);

        eventContext.getMessage().setProperty(HTTP_METHOD, httpMethod);

        UMOMessage result = eventContext.sendEvent(new MuleMessage(requestBody, eventContext.getMessage()),
            tempUrl);

        if (isErrorPayload(result))
        {
            handleException(
                new RestServiceException(CoreMessages.failedToInvokeRestService(tempUrl), result), result);
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

    //if requestBodyBuffer is null, it means that the request is a GET, otherwise it is a POST and  
    //requestBodyBuffer must contain the body of the http method at the end of this function call
    private void setRESTParams(StringBuffer url, UMOMessage msg, Object body, Map args, boolean optional, StringBuffer requestBodyBuffer)
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
            Object value = propertyExtractor.getProperty(exp, msg);

            if (value == null)
            {
                if (!optional)
                {
                    throw new IllegalArgumentException(CoreMessages.propertyIsNotSetOnEvent(exp).toString());
                }
            }
            else if (requestBodyBuffer != null) //implies this is a POST
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
                Object[] requestArray = (Object[])body;
                for(int i=0; i<payloadParameterNames.size(); i++)
                {
                    if (requestBodyBuffer != null)
                    {
                        requestBodyBuffer.append(sep).append(payloadParameterNames.get(i)).append('=').append(requestArray[i].toString());
                    }
                    else
                    {
                        url.append(sep).append(payloadParameterNames.get(i)).append('=').append(requestArray[i].toString());
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

    protected boolean isErrorPayload(UMOMessage message)
    {
        return errorFilter != null && errorFilter.accept(message);
    }

    protected void handleException(RestServiceException e, UMOMessage result) throws Exception
    {
        throw e;
    }
}
