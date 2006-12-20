/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.extensions;

import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.RequestContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.providers.soap.axis.extras.AxisCleanAndAddProperties;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;

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
 * TODO document
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
        // UMOEvent event =
        // (UMOEvent)call.getProperty(MuleProperties.MULE_EVENT_PROPERTY);
        // Get the dispatch endpoint
        String uri = msgContext.getStrProp(MessageContext.TRANS_URL);
        UMOImmutableEndpoint requestEndpoint = (UMOImmutableEndpoint)call.getProperty(MuleProperties.MULE_ENDPOINT_PROPERTY);
        UMOImmutableEndpoint endpoint = null;
        try
        {
            endpoint = lookupEndpoint(uri);
        }
        catch (UMOException e)
        {
            requestEndpoint.getConnector().handleException(e);
            return;
        }

        try
        {
            if (requestEndpoint.getConnector() instanceof AxisConnector)
            {
                msgContext.setTypeMappingRegistry(((AxisConnector)requestEndpoint.getConnector()).getAxisServer()
                    .getTypeMappingRegistry());
            }
            Object payload = null;
            int contentLength = 0;
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
            }
            else
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                msgContext.getRequestMessage().writeTo(baos);
                baos.close();
                payload = baos.toByteArray();
            }

            Map props = new HashMap();
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
            if ((RequestContext.getEvent() != null) && (RequestContext.getEvent().getMessage() != null))
            {
                props = AxisCleanAndAddProperties.cleanAndAdd(RequestContext.getEventContext());            
            }

            if (call.useSOAPAction())
            {
                uri = call.getSOAPActionURI();
            }
            props.put("SOAPAction", uri);
            if (contentLength > 0)
            {
                props.put(HttpConstants.HEADER_CONTENT_LENGTH, Integer.toString(contentLength)); // necessary
                                                                                                    // for
                                                                                                    // supporting
                                                                                                    // httpclient
            }

            if (props.get(HttpConstants.HEADER_CONTENT_TYPE) == null)
            {
                props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
            }
            UMOMessage message = new MuleMessage(payload, props);
            UMOSession session = new MuleSession(message,
                ((AbstractConnector)endpoint.getConnector()).getSessionHandler());

            UMOEvent dispatchEvent = new MuleEvent(message, endpoint, session, sync);
            logger.info("Making Axis soap request on: " + uri);
            if (logger.isDebugEnabled())
            {
                logger.debug("Soap request is:\n" + payload.toString());
            }
            if (sync)
            {
                // We need to rewrite the endpoint on the event to set the
                // reomoteSync property
                MuleEndpoint syncEndpoint = new MuleEndpoint(dispatchEvent.getEndpoint());
                syncEndpoint.setRemoteSync(true);
                dispatchEvent = new MuleEvent(dispatchEvent.getMessage(), syncEndpoint,
                    dispatchEvent.getSession(), dispatchEvent.isSynchronous());
                UMOMessage result = session.sendEvent(dispatchEvent);
                if (result != null)
                {
                    byte[] response = result.getPayloadAsBytes();
                    Message responseMessage = new Message(response);
                    msgContext.setResponseMessage(responseMessage);

                }
                else
                {
                    logger.warn("No response message was returned from synchronous call to: " + uri);
                }
                // remove temp file created for streaming
                if (payload instanceof File)
                {
                    ((File)payload).delete();
                }
            }
            else
            {
                session.dispatchEvent(dispatchEvent);
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

    protected UMOEndpoint lookupEndpoint(String uri) throws UMOException
    {
        UMODescriptor axis = MuleManager.getInstance().getModel().getDescriptor(
            AxisConnector.AXIS_SERVICE_COMPONENT_NAME);
        UMOEndpointURI endpoint = new MuleEndpointURI(uri);
        UMOEndpoint ep;
        if (axis != null)
        {
            synchronized (endpointsCache)
            {
                ep = (UMOEndpoint)endpointsCache.get(endpoint.getAddress());
                if (ep == null)
                {
                    updateEndpointCache(axis.getOutboundRouter());
                    ep = (UMOEndpoint)endpointsCache.get(endpoint.getAddress());
                    if (ep == null)
                    {
                        logger.debug("Dispatch Endpoint uri: " + uri
                                     + " not found on the cache. Creating the endpoint instead.");
                        ep = new MuleEndpoint(uri, false);
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
            ep = new MuleEndpoint(uri, false);
        }
        return ep;
    }

    private void updateEndpointCache(UMOOutboundMessageRouter router)
    {
        endpointsCache.clear();
        for (Iterator iterator = router.getRouters().iterator(); iterator.hasNext();)
        {
            UMOOutboundRouter r = (UMOOutboundRouter)iterator.next();
            for (Iterator iterator1 = r.getEndpoints().iterator(); iterator1.hasNext();)
            {
                UMOEndpoint endpoint = (UMOEndpoint)iterator1.next();
                endpointsCache.put(endpoint.getEndpointURI().getAddress(), endpoint);
            }
        }
    }
}
