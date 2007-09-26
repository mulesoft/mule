/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.UMODescriptorAware;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.NullPayload;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;

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
 * 
 */
public class WSProxyService implements Callable, UMODescriptorAware, Initialisable
{

    private String urlWebservice;
    private String wsdlEndpoint;
    private String wsdlFile;
    private String wsdlFileContents;
    private boolean useFile = false;

    private static final String HTTP_REQUEST = "http.request";
    private static final String WSDL_PARAM_1 = "?wsdl";
    private static final String WSDL_PARAM_2 = "&wsdl";

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

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        // retrieve the message
        UMOMessage message = eventContext.getMessage();

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

            UMOEndpoint webServiceEndpoint = new MuleEndpoint(this.wsdlEndpoint, false);
            UMOMessage replyWSDL = eventContext.sendEvent(new MuleMessage(NullPayload.getInstance()), webServiceEndpoint);

            wsdlString = replyWSDL.getPayloadAsString();

            // find all dependencies and change them
            wsdlString = wsdlString.replaceAll(this.urlWebservice, eventContext.getEndpointURI().getAddress());

            // create a new mule message with the new WSDL
            MuleMessage modifiedWsdl = new MuleMessage(wsdlString, message);

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
            return eventContext.getMessage();
        }
    }

    // called once upon initialisation
    public void setDescriptor(UMODescriptor descriptor)
    {
        UMOOutboundRouter router = (UMOOutboundRouter)descriptor.getOutboundRouter().getRouters().get(0);
        UMOEndpoint endpoint = (UMOEndpoint)router.getEndpoints().get(0);
        this.urlWebservice = endpoint.getEndpointURI().getAddress();

        // remove any params from the url
        int paramIndex;
        if ((paramIndex = this.urlWebservice.indexOf("?")) != -1)
        {
            this.urlWebservice = this.urlWebservice.substring(0, paramIndex);
        }
    }

    public void initialise() throws InitialisationException
    {
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
                this.wsdlEndpoint = this.urlWebservice.concat("?WSDL");
                logger.info("Defaulting to: " + this.wsdlEndpoint);
            }
            else
            {
                logger.info("Using url " + this.wsdlEndpoint + " as WSDL");
            }
        }

    }
}
