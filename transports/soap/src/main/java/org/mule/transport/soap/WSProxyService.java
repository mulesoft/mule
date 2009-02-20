/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap;

import org.mule.DefaultMuleMessage;
import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is implemented to act as a Proxy for a Web Service. It listens for
 * requests on the inbound endpoint and if it encounters the "WSDL" property in the
 * address, it will fetch the WSDL from the original web service and return it back.
 * In case the wsdlFile property is set, when the WSProxyService encounters a request
 * for the wsdl, instead of fetching the WSDL from the original web service, it will
 * return back the file expressed in the property. When a normal SOAP request is
 * encountered, it will forward the call to the web service with no modifications to
 * the SOAP message. The outbound router of this class must include the address of
 * the webservice to be proxied. No need to include the method name as a parameter in
 * the address, since it will be in the SOAP message as well. Furthermore a property
 * named uriWsdl can optionally be set which as the name suggests, indicate the URL
 * of the WSDL for the service. If this property is not set, the address of the WSDL
 * will be assumed to be the value of uriWebservice followed by "?WSDL". It is
 * important to note that both urls' of the webservice to be proxied and the WSDL
 * address must contain no xfire or axis endpoints, just plain http endpoints. Even
 * the inbound endpoint of the WSProxyService must be residing on an http protocol
 * (with no xfire or axis).
 */
public class WSProxyService implements Callable, ServiceAware, Initialisable
{

    private String urlWebservice;
    private String wsdlEndpoint;
    private String wsdlFile;
    private String wsdlFileContents;
    private boolean useFile = false;

    private Service service;

    private static final String HTTP_REQUEST = "http.request";
    private static final String WSDL_PARAM_1 = "?wsdl";
    private static final String WSDL_PARAM_2 = "&wsdl";

    /** This is an internal semaphore, not a property */
    private boolean lazyInit = true;
    
    protected static transient Log logger = LogFactory.getLog(WSProxyService.class);

    /**
     * @return returns the url of the WSDL
     */
    public String getWsdlEndpoint()
    {
        return wsdlEndpoint;
    }

    /**
     * @param urlWsdl Sets the property urlWsdl (the url of the WSDL of the web
     *            service)
     */
    public void setWsdlEndpoint(String urlWsdl)
    {
        this.wsdlEndpoint = urlWsdl;
    }

    /**
     * @return returns the location of the local wsdl
     */
    public String getWsdlFile()
    {
        return wsdlFile;
    }

    /**
     * @param wsdlFile sets the location of the local wsdl file
     */
    public void setWsdlFile(String wsdlFile)
    {
        this.wsdlFile = wsdlFile;
    }

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (wsdlEndpoint == null && lazyInit)
        {
            initialise();
        }
        
        // retrieve the message
        MuleMessage message = eventContext.getMessage();

        // retrieve the original http request. This will be used to check if the user
        // asked for the WSDL or just for the service
        String httpRequest = ((String)message.getProperty(HTTP_REQUEST)).toLowerCase();

        // check if the inbound endpoint contains the WSDL parameter
        if ((httpRequest.indexOf(WSDL_PARAM_1) != -1) || (httpRequest.indexOf(WSDL_PARAM_2) != -1))
        {
            logger.debug("Retrieving WSDL from web service");

            String wsdlString;

            if (this.useFile)
            {
                // the processing is stopped so that the result is not passed through the
                // outbound router but will be passed back as a result
                eventContext.setStopFurtherProcessing(true);
                return wsdlFileContents;
            }
            MuleContext muleContext = MuleServer.getMuleContext();
            InboundEndpoint webServiceEndpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint(this.wsdlEndpoint);

            MuleMessage replyWSDL = eventContext.requestEvent(webServiceEndpoint, eventContext.getTimeout());

            wsdlString = replyWSDL.getPayloadAsString();

            // create a new mule message with the new WSDL
            String realWSDLURI = wsdlEndpoint.split("\\?")[0];
            String proxyWSDLURI = eventContext.getEndpointURI().toString();
            
            wsdlString = wsdlString.replaceAll(realWSDLURI, proxyWSDLURI);
            if (wsdlString.indexOf("localhost") > -1)
            {
                wsdlString = wsdlString.replaceAll("localhost", InetAddress.getLocalHost().getHostName());
            }
            
            DefaultMuleMessage modifiedWsdl = new DefaultMuleMessage(wsdlString);
            logger.debug("WSDL retrieved successfully");

            // the processing is stopped so that the result is not passed through the
            // outbound router but will be passed back as a result
            eventContext.setStopFurtherProcessing(true);

            return modifiedWsdl;
        }
        else
        // forward the normal call on the outbound router without any modification
        {
            logger.debug("Forwarding SOAP message");
            return eventContext.transformMessage();
        }
    }

    // called once upon initialisation
    public void setService(Service service)
    {
        this.service = service;
    }

    public void initialise() throws InitialisationException
    {
        if (service != null)
        {        
            OutboundRouter router = (OutboundRouter)service.getOutboundRouter().getRouters().get(0);
            ImmutableEndpoint endpoint = (ImmutableEndpoint)router.getEndpoints().get(0);
            this.urlWebservice = endpoint.getEndpointURI().getAddress();
    
            // remove any params from the url
            int paramIndex;
            if ((paramIndex = this.urlWebservice.indexOf("?")) != -1)
            {
                this.urlWebservice = this.urlWebservice.substring(0, paramIndex);
            }
    
            // if the wsdlFile property is not empty, the onCall() will use this file for WSDL requests
            if (StringUtils.isNotBlank(this.wsdlFile))
            {
                try
                {
                    this.wsdlFileContents = IOUtils.getResourceAsString(this.wsdlFile, getClass());
    
                    if (StringUtils.isNotBlank(this.wsdlFileContents))
                    {
                        this.useFile = true;
                        logger.info("Using file " + this.wsdlFile + " as WSDL file");
                    }
                }
                catch (IOException fileError)
                {
                    throw new InitialisationException(CoreMessages.failedToLoad(this.wsdlFile), this);
                }
            }
    
            if (!this.useFile)
            {
                // if no wsdl property is set, create one which will include the original
                // url of the webservice followed by ?WSDL
                if (StringUtils.isBlank(this.wsdlEndpoint))
                {
                    if (urlWebservice == null)
                    {
                        throw new InitialisationException(MessageFactory.createStaticMessage("urlWebservice has not been set, service has not been initialized properly"), this);
                    }
                    this.wsdlEndpoint = this.urlWebservice.concat("?WSDL");
                    logger.info("Defaulting to: " + this.wsdlEndpoint);
                }
                else
                {
                    logger.info("Using url " + this.wsdlEndpoint + " as WSDL");
                }
            }
        }
        else if (!lazyInit)
        {
            // Service not injected yet, try lazy init (i.e., upon onCall()).
            logger.debug("Service has not yet been injected, lazy initialization will be used.");
            lazyInit = true;
        }
        else
        {
            // We're already in lazy init and the service is still not set, so throw an exception.
            throw new InitialisationException(MessageFactory.createStaticMessage("Service not set, this service has not been initialized properly."), this);                        
        }
    }
}
