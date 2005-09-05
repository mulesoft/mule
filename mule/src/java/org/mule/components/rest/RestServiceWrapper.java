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
 *
 */
package org.mule.components.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.NullPayload;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.SgmlCodec;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RestServiceWrapper implements Callable, Initialisable
{
    public static final String REST_SERVICE_URL = "rest.service.url";

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private String serviceUrl;
    private boolean urlFromMessage = false;
    private List reqiredParams = new ArrayList();
    private List optionalParams = new ArrayList();
    private String httpMethod = "GET";
    private String payloadParameterName;
    private UMOFilter errorFilter;
    private String errorExpression;

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public boolean isUrlFromMessage() {
        return urlFromMessage;
    }

    public void setUrlFromMessage(boolean urlFromMessage) {
        this.urlFromMessage = urlFromMessage;
    }

    public List getReqiredParams() {
        return reqiredParams;
    }

    public void setReqiredParams(List reqiredParams) {
        this.reqiredParams = reqiredParams;
    }

    public List getOptionalParams() {
        return optionalParams;
    }

    public void setOptionalParams(List optionalParams) {
        this.optionalParams = optionalParams;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPayloadParameterName() {
        return payloadParameterName;
    }

    public void setPayloadParameterName(String payloadParameterName) {
        this.payloadParameterName = payloadParameterName;
    }

    public UMOFilter getErrorFilter() {
        return errorFilter;
    }

    public void setErrorFilter(UMOFilter errorFilter) {
        this.errorFilter = errorFilter;
    }

    public String getErrorExpression() {
        return errorExpression;
    }

    public void setErrorExpression(String errorExpression) {
        this.errorExpression = errorExpression;
    }

    public void initialise() throws InitialisationException, RecoverableException {
        if(serviceUrl==null && !urlFromMessage) {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "serviceUrl"), this);
        } else {
            try {
                new URL(serviceUrl);
            } catch (MalformedURLException e) {
                throw new InitialisationException(e, this);
            }
        }

        if(errorFilter==null) {
            if(errorExpression==null) {
                //We'll set a default filter that checks the return code
                errorFilter = new MessagePropertyFilter("http.status!=200");
                logger.info("Setting default error filter to MessagePropertyFilter('http.status!=200')");
            } else {
                errorFilter = new RegExFilter(errorExpression);
            }
        }
    }

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        String tempUrl;
        Object request = eventContext.getTransformedMessage();
        Object requestBody = request;
        if(urlFromMessage) {
            tempUrl = (String)eventContext.getProperty(REST_SERVICE_URL);
            if(tempUrl==null) {
                throw new IllegalArgumentException(new Message(Messages.X_PROPERTY_IS_NOT_SET_ON_EVENT, REST_SERVICE_URL).toString());
            }
        } else {
            tempUrl = serviceUrl;
        }
        StringBuffer urlBuffer = new StringBuffer(tempUrl);

        Map params = new HashMap(eventContext.getProperties());
        if(payloadParameterName!=null) {
            params.put(payloadParameterName, request);
            requestBody = new NullPayload();
        } else if(request instanceof Map) {
            params.putAll((Map)request);
            requestBody = new NullPayload();
        }

        setRESTParams(urlBuffer, params, reqiredParams, false);
        setRESTParams(urlBuffer, params, optionalParams, true);

        tempUrl = urlBuffer.toString();
        logger.info("Invoking REST service: " + tempUrl);

        UMOEndpointURI endpointURI = new MuleEndpointURI(SgmlCodec.encodeString(tempUrl));
        eventContext.getMessage().setProperty("http.method", httpMethod);

        UMOMessage result = eventContext.sendEvent(new MuleMessage(requestBody, eventContext.getProperties()), endpointURI);

        if(isErrorPayload(result)) {
            handleException(new RestServiceException(
                    new Message(Messages.FAILED_TO_INVOKE_REST_SERVICE_X, tempUrl), result),
                    result);
        }
        return result;
    }

    private void setRESTParams(StringBuffer url, Map params, List args, boolean optional) {
        char sep;
        if(url.indexOf("?") > -1) {
            sep = '&';
        } else {
            sep = '?';
        }
        String arg;
        Object value;
        for (Iterator iterator = args.iterator(); iterator.hasNext();) {
            arg = (String) iterator.next();
            value = params.get(arg);
            if(value==null && !optional) {
                throw new IllegalArgumentException(new Message(Messages.X_PROPERTY_IS_NOT_SET_ON_EVENT, arg).toString());
            }
            url.append(sep);
            sep = '&';
            url.append(arg).append("=").append(value);
        }
        if(!optional && payloadParameterName!=null) {
            url.append(sep).append(payloadParameterName).append("=").append(params.get(payloadParameterName));
        }
    }

    protected boolean isErrorPayload(UMOMessage message)
    {
        if(errorFilter!=null) {
            return errorFilter.accept(message);
        }
        return false;
    }

    protected void handleException(RestServiceException e, UMOMessage result) throws Exception {
        throw e;
    }
}
