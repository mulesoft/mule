/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.admin;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.internal.notifications.AdminNotification;
import org.mule.impl.message.ExceptionPayload;
import org.mule.providers.AbstractConnector;
import org.mule.providers.NullPayload;
import org.mule.transformers.wire.WireFormat;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.transformer.UMOTransformer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>MuleManagerComponent</code> is a MuleManager interal server component
 * responsible for receiving remote requests and dispatching them locally. This
 * allows developer to tunnel requests through http ssl to a Mule instance behind a
 * firewall
 */

public class MuleManagerComponent implements Callable, Initialisable
{
    /**
     * logger used by this class
     */
    protected static Log logger = LogFactory.getLog(MuleManagerComponent.class);

    public static final String MANAGER_COMPONENT_NAME = "_muleManagerComponent";
    public static final String MANAGER_ENDPOINT_NAME = "_muleManagerEndpoint";

    /**
     * Use Serialization by default
     */
    protected WireFormat wireFormat;

    public void initialise() throws InitialisationException
    {
        if (wireFormat == null)
        {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "wireFormat"), this);
        }
    }

    public Object onCall(UMOEventContext context) throws Exception
    {
        Object result;
        logger.debug("Message received by MuleManagerComponent");
        ByteArrayInputStream in = new ByteArrayInputStream(context.getTransformedMessageAsBytes());
        AdminNotification action = (AdminNotification)wireFormat.read(in);
        if (AdminNotification.ACTION_INVOKE == action.getAction())
        {
            result = invokeAction(action, context);
        }
        else if (AdminNotification.ACTION_SEND == action.getAction())
        {
            result = sendAction(action, context);
        }
        else if (AdminNotification.ACTION_DISPATCH == action.getAction())
        {
            result = sendAction(action, context);
        }
        else if (AdminNotification.ACTION_RECEIVE == action.getAction())
        {
            result = receiveAction(action, context);
        }
        else
        {
            result = handleException(null, new MuleException(new Message(
                Messages.EVENT_TYPE_X_NOT_RECOGNISED, "AdminNotification:" + action.getAction())));
        }
        return result;
    }

    protected Object invokeAction(AdminNotification action, UMOEventContext context) throws UMOException
    {
        String destComponent = null;
        UMOMessage result = null;
        String endpoint = action.getResourceIdentifier();
        if (action.getResourceIdentifier().startsWith("mule:"))
        {
            destComponent = endpoint.substring(endpoint.lastIndexOf("/") + 1);
        }
        else
        {
            destComponent = endpoint;
        }

        if (destComponent != null)
        {
            UMOSession session = MuleManager.getInstance().getModel().getComponentSession(destComponent);
            // Need to do this otherise when the event is invoked the
            // transformer associated with the Mule Admin queue will be invoked, but
            // the
            // message will not be of expected type
            UMOEndpoint ep = new MuleEndpoint(RequestContext.getEvent().getEndpoint());
            ep.setTransformer(null);
            UMOEvent event = new MuleEvent(action.getMessage(), ep, context.getSession(),
                context.isSynchronous());
            RequestContext.setEvent(event);

            if (context.isSynchronous())
            {
                result = session.getComponent().sendEvent(event);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                wireFormat.write(out, result);
                return out.toByteArray();
            }
            else
            {
                session.getComponent().dispatchEvent(event);
                return null;
            }
        }
        else
        {
            return handleException(result, new MuleException(new Message(
                Messages.COULD_NOT_DETERMINE_DESTINATION_COMPONENT_FROM_ENDPOINT_X, endpoint)));
        }
    }

    protected Object sendAction(AdminNotification action, UMOEventContext context) throws UMOException
    {
        UMOMessage result = null;
        try
        {
            UMOEndpoint endpoint = new MuleEndpoint(action.getResourceIdentifier(), false);

            if (AdminNotification.ACTION_DISPATCH == action.getAction())
            {
                context.dispatchEvent(action.getMessage(), endpoint);
                return null;
            }
            else
            {
                endpoint.setRemoteSync(true);
                result = context.sendEvent(action.getMessage(), endpoint);
                if (result == null)
                {
                    return null;
                }
                else
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    wireFormat.write(out, result);
                    return out.toByteArray();
                }
            }
        }
        catch (Exception e)
        {
            return handleException(result, e);
        }
    }

    protected Object receiveAction(AdminNotification action, UMOEventContext context) throws UMOException
    {
        UMOMessage result = null;
        try
        {
            UMOEndpointURI endpointUri = new MuleEndpointURI(action.getResourceIdentifier());
            UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri,
                UMOEndpoint.ENDPOINT_TYPE_SENDER);

            UMOMessageDispatcher dispatcher = endpoint.getConnector().getDispatcher(endpoint);
            long timeout = MapUtils.getLongValue(action.getProperties(),
                MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, MuleManager.getConfiguration()
                    .getSynchronousEventTimeout());

            UMOEndpointURI ep = new MuleEndpointURI(action.getResourceIdentifier());
            result = dispatcher.receive(ep, timeout);
            if (result != null)
            {
                // See if there is a default transformer on the connector
                UMOTransformer trans = ((AbstractConnector)endpoint.getConnector()).getDefaultInboundTransformer();
                if (trans != null)
                {
                    Object payload = trans.transform(result.getPayload());
                    result = new MuleMessage(payload, result);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                wireFormat.write(out, result);
                return out.toByteArray();
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            return handleException(result, e);
        }

    }

    public static final UMODescriptor getDescriptor(UMOConnector connector,
                                                    UMOEndpointURI endpointUri,
                                                    WireFormat wireFormat) throws UMOException
    {
        UMOEndpoint endpoint = new MuleEndpoint();
        endpoint.setConnector(connector);
        endpoint.setEndpointURI(endpointUri);
        endpoint.setName(MANAGER_ENDPOINT_NAME);
        endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_RECEIVER);

        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setName(MANAGER_COMPONENT_NAME);
        descriptor.setInboundEndpoint(endpoint);
        descriptor.setImplementation(MuleManagerComponent.class.getName());
        descriptor.setContainerManaged(false);
        Map props = new HashMap();
        props.put("wireFormat", wireFormat);
        descriptor.setProperties(props);
        return descriptor;
    }

    /**
     * Wraps an execption into a MuleMessage with an Exception payload and returns
     * the Xml representation of it
     * 
     * @param result the result of the invocation or null if the exception occurred
     *            before or during the invocation
     * @param e the Exception thrown
     * @return an Xml String message result
     */
    protected String handleException(UMOMessage result, Throwable e)
    {
        logger.error("Failed to process admin request: " + e.getMessage(), e);
        if (result == null)
        {
            result = new MuleMessage(new NullPayload(), (Map)null);
        }
        result.setExceptionPayload(new ExceptionPayload(e));
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wireFormat.write(out, result);
            return out.toString(MuleManager.getConfiguration().getEncoding());
        }
        catch (Exception e1)
        {
            logger.error(e.toString(), e);
            return e.getMessage();
        }
    }

    public WireFormat getWireFormat()
    {
        return wireFormat;
    }

    public void setWireFormat(WireFormat wireFormat)
    {
        this.wireFormat = wireFormat;
    }
}
