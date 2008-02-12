/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis.extensions;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.soap.SoapConstants;
import org.mule.transport.soap.axis.AxisConnector;
import org.mule.transport.soap.axis.extras.AxisCleanAndAddProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.axis.AxisFault;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.handlers.BasicHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An Axis handler that will dispatch the SOAP event via a Mule endpoint
 */
public class UniversalSender extends BasicHandler
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7943380365092172940L;

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Map endpointsCache = new HashMap();

    public void invoke(MessageContext msgContext) throws AxisFault
    {
        boolean sync = true;
        Call call = (Call)msgContext.getProperty("call_object");
        if (call == null)
        {
            throw new IllegalStateException(
                "The call_object property must be set on the message context to the client Call object");
        }
        if (Boolean.TRUE.equals(call.getProperty("axis.one.way")))
        {
            sync = false;
        }
        // Get the event stored in call
        // If a receive call is made there will be no event
        // MuleEvent event =
        // (MuleEvent)call.getProperty(MuleProperties.MULE_EVENT_PROPERTY);
        // Get the dispatch endpoint
        String uri = msgContext.getStrProp(MessageContext.TRANS_URL);
        ImmutableEndpoint requestEndpoint = (ImmutableEndpoint)call
            .getProperty(MuleProperties.MULE_ENDPOINT_PROPERTY);
        
        ImmutableEndpoint endpoint;

        // put username and password in URI if they are set on the current event
        if (msgContext.getUsername() != null)
        {
            String[] tempEndpoint = uri.split("//");
            String credentialString = msgContext.getUsername() + ":"
                                      + msgContext.getPassword();
            uri = tempEndpoint[0] + "//" + credentialString + "@" + tempEndpoint[1];
            try
            {
                endpoint = lookupEndpoint(uri);
            }
            catch (MuleException e)
            {
                requestEndpoint.getConnector().handleException(e);
                return;
            }
        }
        else
        {
            try
            {
                endpoint = lookupEndpoint(uri);
            }
            catch (MuleException e)
            {
                requestEndpoint.getConnector().handleException(e);
                return;
            }
        }

        try
        {
            if (requestEndpoint.getConnector() instanceof AxisConnector)
            {
                msgContext.setTypeMappingRegistry(((AxisConnector)requestEndpoint.getConnector())
                    .getAxis().getTypeMappingRegistry());
            }
            
            Map props = new HashMap();
            Object payload = null;
            int contentLength = 0;
            String contentType = null;
            if (msgContext.getRequestMessage().countAttachments() > 0)
            {
                File temp = File.createTempFile("soap", ".tmp");
                temp.deleteOnExit(); // TODO cleanup files earlier (IOUtils has a
                // file tracker)
                FileOutputStream fos = new FileOutputStream(temp);
                msgContext.getRequestMessage().writeTo(fos);
                fos.close();
                contentLength = (int)temp.length();
                payload = new FileInputStream(temp);
                contentType = "multipart/related";
            }
            else
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                msgContext.getRequestMessage().writeTo(baos);
                baos.close();
                payload = baos.toByteArray();
            }

            // props.putAll(event.getProperties());
            for (Iterator iterator = msgContext.getPropertyNames(); iterator.hasNext();)
            {
                String name = (String)iterator.next();
                if (!name.equals("call_object") && !name.equals("wsdl.service"))
                {
                    props.put(name, msgContext.getProperty(name));
                }
            }

            // add all custom headers, filter out all mule headers (such as
            // MULE_SESSION) except
            // for MULE_USER header. Filter out other headers like "soapMethods" and
            // MuleProperties.MULE_METHOD_PROPERTY and "soapAction"
            // and also filter out any http related header
            if ((RequestContext.getEvent() != null)
                && (RequestContext.getEvent().getMessage() != null))
            {
                props = AxisCleanAndAddProperties.cleanAndAdd(RequestContext.getEventContext());
            }
            
            // with jms and vm the default SOAPAction will result in the name of the endpoint, which we may not necessarily want. This should be set manually on the endpoint
            String scheme = requestEndpoint.getEndpointURI().getScheme(); 
            if (!("vm".equalsIgnoreCase(scheme) || "jms".equalsIgnoreCase(scheme)))
            {
                if (call.useSOAPAction())
                {
                    uri = call.getSOAPActionURI();
                }
                props.put(SoapConstants.SOAP_ACTION_PROPERTY_CAPS, uri);
            }
            if (contentLength > 0)
            {
                props.put(HttpConstants.HEADER_CONTENT_LENGTH, Integer.toString(contentLength)); // necessary
                // for
                // supporting
                // httpclient
            }

            
            if (props.get(HttpConstants.HEADER_CONTENT_TYPE) == null)
            {
                if (contentType == null)
                {
                    contentType = "text/xml";
                }
                
                props.put(HttpConstants.HEADER_CONTENT_TYPE, contentType);
            }
            MuleMessage message = new DefaultMuleMessage(payload, props);
            MuleSession session = RequestContext.getEventContext().getSession();

            logger.info("Making Axis soap request on: " + uri);
            if (logger.isDebugEnabled())
            {
                logger.debug("Soap request is:\n" + new String((payload instanceof byte[] ? (byte[])payload : payload.toString().getBytes())));
            }

            if (sync)
            {
                MuleContext muleContext = MuleServer.getMuleContext();
                EndpointBuilder builder = new EndpointURIEndpointBuilder(endpoint, muleContext);
                builder.setRemoteSync(true);
                ImmutableEndpoint syncEndpoint = muleContext.getRegistry()
                    .lookupEndpointFactory()
                    .getOutboundEndpoint(builder);
                MuleEvent dispatchEvent = new DefaultMuleEvent(message, syncEndpoint, session, sync);
                MuleMessage result = endpoint.send(dispatchEvent);

                if (result != null)
                {
                    byte[] response = result.getPayloadAsBytes();
                    Message responseMessage = new Message(response);
                    msgContext.setResponseMessage(responseMessage);

                }
                else
                {
                    logger
                        .warn("No response message was returned from synchronous call to: " + uri);
                }
                // remove temp file created for streaming
                if (payload instanceof File)
                {
                    ((File)payload).delete();
                }
            }
            else
            {
                MuleEvent dispatchEvent = new DefaultMuleEvent(message, endpoint, session, sync);
                endpoint.dispatch(dispatchEvent);
            }
        }
        catch (AxisFault axisFault)
        {
            throw axisFault;
        }
        catch (Exception e)
        {
            throw new AxisFault(e.getMessage(), new Throwable(e));
        }

    }

    protected ImmutableEndpoint lookupEndpoint(String uri) throws MuleException
    {
        Service axis = RegistryContext.getRegistry().lookupService(AxisConnector.AXIS_SERVICE_COMPONENT_NAME);
        EndpointURI endpoint = new MuleEndpointURI(uri);
        MuleContext muleContext = MuleServer.getMuleContext(); 
        ImmutableEndpoint ep;

        if (axis != null)
        {
            synchronized (endpointsCache)
            {
                ep = (ImmutableEndpoint)endpointsCache.get(endpoint.getAddress());
                if (ep == null)
                {
                    updateEndpointCache(axis.getOutboundRouter());
                    ep = (ImmutableEndpoint)endpointsCache.get(endpoint.getAddress());
                    if (ep == null)
                    {
                        logger.debug("Dispatch Endpoint uri: " + uri
                                     + " not found on the cache. Creating the endpoint instead.");
                        ep = muleContext.getRegistry().lookupEndpointFactory()
                                .getOutboundEndpoint(uri);
                    }
                    else
                    {
                        logger.info("Found endpoint: " + uri + " on the Axis service component");
                    }
                }
                else
                {
                    logger.info("Found endpoint: " + uri + " on the Axis service component");
                }
            }
        }
        else
        {
            ep = muleContext.getRegistry().lookupEndpointFactory()
                    .getOutboundEndpoint(uri);
        }
        return ep;
    }

    private void updateEndpointCache(OutboundRouterCollection router)
    {
        endpointsCache.clear();
        for (Iterator iterator = router.getRouters().iterator(); iterator.hasNext();)
        {
            OutboundRouter r = (OutboundRouter)iterator.next();
            for (Iterator iterator1 = r.getEndpoints().iterator(); iterator1.hasNext();)
            {
                ImmutableEndpoint endpoint = (ImmutableEndpoint)iterator1.next();
                endpointsCache.put(endpoint.getEndpointURI().getAddress(), endpoint);
            }
        }
    }
}
