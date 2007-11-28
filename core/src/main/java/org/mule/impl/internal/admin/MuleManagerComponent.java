/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.admin;

import org.mule.MuleException;
import org.mule.MuleServer;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.RequestContext;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.internal.notifications.AdminNotification;
import org.mule.impl.message.ExceptionPayload;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.providers.AbstractConnector;
import org.mule.providers.NullPayload;
import org.mule.transformers.wire.WireFormat;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointFactory;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.MapUtils;
import org.mule.util.object.SimpleObjectFactory;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    protected static final Log logger = LogFactory.getLog(MuleManagerComponent.class);

    public static final String MANAGER_COMPONENT_NAME = "_muleManagerComponent";
    public static final String MANAGER_ENDPOINT_NAME = "_muleManagerEndpoint";

    /**
     * Use Serialization by default
     */
    protected WireFormat wireFormat;

    protected String encoding;

    protected int synchronousEventTimeout = 5000;

    public void initialise() throws InitialisationException
    {
        if (wireFormat == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("wireFormat"), this);
        }
    }
    public Object onCall(UMOEventContext context) throws Exception
    {
        Object result;
        logger.debug("Message received by MuleManagerComponent");
        ByteArrayInputStream in = new ByteArrayInputStream(context.getTransformedMessageAsBytes());
        AdminNotification action = (AdminNotification) wireFormat.read(in);
        if (AdminNotification.ACTION_INVOKE == action.getAction())
        {
            result = invokeAction(action, context);
        }
        else if (AdminNotification.ACTION_SEND == action.getAction() ||
                 AdminNotification.ACTION_DISPATCH == action.getAction())
        {
            result = sendAction(action, context);
        }
        else if (AdminNotification.ACTION_RECEIVE == action.getAction())
        {
            result = receiveAction(action, context);
        }
        else
        {
            result = handleException(null, new MuleException(
                CoreMessages.eventTypeNotRecognised("AdminNotification:" + action.getAction())));
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
            UMOSession session = new MuleSession(MuleServer.getManagementContext().getRegistry().lookupComponent(
                destComponent));
            // Need to do this otherise when the event is invoked the
            // transformer associated with the Mule Admin queue will be invoked, but
            // the message will not be of expected type
            UMOManagementContext managementContext = MuleServer.getManagementContext();
            UMOEndpointBuilder builder = new EndpointURIEndpointBuilder(RequestContext.getEvent().getEndpoint(), managementContext);
            // TODO - is this correct? it stops any other transformer from being set
            builder.setTransformers(new LinkedList());
            UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(builder);
            UMOEvent event = new MuleEvent(action.getMessage(), ep, context.getSession(), context.isSynchronous());
            event = RequestContext.setEvent(event);

            if (context.isSynchronous())
            {
                result = session.getComponent().sendEvent(event);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                wireFormat.write(out, result, getEncoding());
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
            return handleException(result, new MuleException(
                CoreMessages.couldNotDetermineDestinationComponentFromEndpoint(endpoint)));
        }
    }

    protected Object sendAction(AdminNotification action, UMOEventContext context) throws UMOException
    {
        UMOMessage result = null;
        UMOImmutableEndpoint endpoint = null;
        UMOManagementContext managementContext = context.getManagementContext();
        try
        {
            if (AdminNotification.ACTION_DISPATCH == action.getAction())
            {
                endpoint = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
                    action.getResourceIdentifier());
                context.dispatchEvent(action.getMessage(), endpoint);
                return null;
            }
            else
            {
                UMOEndpointFactory endpointFactory = managementContext.getRegistry().lookupEndpointFactory();
                UMOEndpointBuilder endpointBuilder = endpointFactory.getEndpointBuilder(action.getResourceIdentifier());
                endpointBuilder.setRemoteSync(true);
                endpoint = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointBuilder);
                result = context.sendEvent(action.getMessage(), endpoint);
                if (result == null)
                {
                    return null;
                }
                else
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    wireFormat.write(out, result, getEncoding());
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
            UMOImmutableEndpoint endpoint = context.getManagementContext()
                .getRegistry()
                .lookupEndpointFactory()
                .getOutboundEndpoint(action.getResourceIdentifier());

            long timeout = MapUtils.getLongValue(action.getProperties(),
                MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, getSynchronousEventTimeout());

            result = endpoint.getConnector().request(action.getResourceIdentifier(), timeout);
            if (result != null)
            {
                // See if there is a default transformer on the connector
                List transformers = ((AbstractConnector) endpoint.getConnector()).getDefaultInboundTransformers();
                if (transformers != null)
                {
                    result.applyTransformers(transformers);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                wireFormat.write(out, result, getEncoding());
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


    public static final UMOComponent getComponent(UMOEndpointBuilder endpointBuilder,
                                                    WireFormat wireFormat,
                                                    String encoding,
                                                    int eventTimeout,
                                                    UMOManagementContext managementContext) throws UMOException
    {
        try
        {
            UMOComponent component = new SedaComponent();
            component.setName(MANAGER_COMPONENT_NAME);
            component.setModel(managementContext.getRegistry().lookupSystemModel());

            Map props = new HashMap();
            props.put("wireFormat", wireFormat);
            props.put("encoding", encoding);
            props.put("synchronousEventTimeout", new Integer(eventTimeout));
            component.setServiceFactory(new SimpleObjectFactory(MuleManagerComponent.class, props));

            component.setManagementContext(managementContext);
            //component.initialise();
    
            endpointBuilder.setName(MANAGER_ENDPOINT_NAME);
            UMOImmutableEndpoint endpoint = managementContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint(endpointBuilder);
            component.getInboundRouter().addEndpoint(endpoint);

            return component;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, null);
        }
    }

    /**
     * Wraps an exception into a MuleMessage with an Exception payload and returns
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
            result = new MuleMessage(NullPayload.getInstance(), (Map) null);
        }
        result.setExceptionPayload(new ExceptionPayload(e));
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wireFormat.write(out, result, getEncoding());
            return out.toString(getEncoding());
        }
        catch (Exception e1)
        {
            // TODO MULE-863: Is this sufficient?
            // log the inner exception here since the earlier exception was logged earlier
            logger.error("Failed to format message, using direct string (details at debug level): " + e1.getMessage());
            logger.debug(e1.toString(), e1);
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

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public int getSynchronousEventTimeout()
    {
        return synchronousEventTimeout;
    }

    public void setSynchronousEventTimeout(int synchronousEventTimeout)
    {
        this.synchronousEventTimeout = synchronousEventTimeout;
    }
}
