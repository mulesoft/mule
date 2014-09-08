/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.remoting;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.RequestContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.component.SimpleCallableJavaComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.message.DefaultExceptionPayload;
import org.mule.model.seda.SedaService;
import org.mule.module.client.i18n.ClientMessages;
import org.mule.module.client.remoting.notification.RemoteDispatcherNotification;
import org.mule.object.SingletonObjectFactory;
import org.mule.transport.AbstractConnector;
import org.mule.transport.NullPayload;
import org.mule.util.MapUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>RemoteDispatcherComponent</code> is a MuleManager interal server component
 * responsible for receiving remote requests and dispatching them locally. This
 * allows developer to tunnel requests through http ssl to a Mule instance behind a
 * firewall
 *
 * <b>Deprecated as of 3.6.0</b>
 */
@Deprecated
public class RemoteDispatcherComponent implements Callable, Initialisable
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(RemoteDispatcherComponent.class);

    public static final String MANAGER_COMPONENT_NAME = "_muleManagerComponent";

    /**
     * Use Serialization by default
     */
    protected WireFormat wireFormat;

    protected String encoding;

    protected int synchronousEventTimeout = 5000;
    
    protected InboundEndpoint inboundEndpoint;

    protected MuleContext muleContext;

    public RemoteDispatcherComponent(InboundEndpoint inboundEndpoint, WireFormat wireFormat, String encoding, int synchronousEventTimeout)
    {
        this.inboundEndpoint = inboundEndpoint;
        this.wireFormat = wireFormat;
        this.encoding = encoding;
        this.synchronousEventTimeout = synchronousEventTimeout;
    }
    
    public void initialise() throws InitialisationException
    {
        if (inboundEndpoint == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("inboundEndpoint"), this);
        }
        if (wireFormat == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("wireFormat"), this);
        }
    }

    public Object onCall(MuleEventContext context) throws Exception
    {
        muleContext = context.getMuleContext();
        byte[] messageBytes = (byte[]) context.transformMessage(byte[].class);
        if(new String(messageBytes).equals(ServerHandshake.SERVER_HANDSHAKE_PROPERTY))
        {
            return doHandshake(context);
        }

        Object result;
        logger.debug("Message received by RemoteDispatcherComponent");
        ByteArrayInputStream in = new ByteArrayInputStream((byte[]) context.transformMessage(DataType.BYTE_ARRAY_DATA_TYPE));
        RemoteDispatcherNotification action = (RemoteDispatcherNotification) ((MuleMessage)wireFormat.read(in)).getPayload();

        // because we serialized a message inside a message, we need to inject the the muleContext ourselves
        //TODO review the serialization format for RemoteDispatching
        if(action.getMessage()!=null)
        {
            Method m = action.getMessage().getClass().getDeclaredMethod("initAfterDeserialisation", MuleContext.class);
            m.setAccessible(true);
            m.invoke(action.getMessage(), muleContext);
        }

        if (RemoteDispatcherNotification.ACTION_INVOKE == action.getAction())
        {
            result = invokeAction(action, context);
        }
        else if (RemoteDispatcherNotification.ACTION_SEND == action.getAction() ||
                 RemoteDispatcherNotification.ACTION_DISPATCH == action.getAction())
        {
            result = sendAction(action, context);
        }
        else if (RemoteDispatcherNotification.ACTION_RECEIVE == action.getAction())
        {
            result = receiveAction(action, context);
        }
        else
        {
            result = handleException(null, new DefaultMuleException(
                CoreMessages.eventTypeNotRecognised("RemoteDispatcherNotification:" + action.getAction())));
        }
        return result;
    }

    protected ServerHandshake doHandshake(MuleEventContext context) throws TransformerException
    {
        ServerHandshake handshake  = new ServerHandshake();
        handshake.setWireFormatClass(wireFormat.getClass().getName());
        return handshake;
    }

    protected Object invokeAction(RemoteDispatcherNotification action, MuleEventContext context) throws MuleException
    {
        String destComponent;
        MuleMessage result = null;
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
            Object flowConstruct = muleContext.getRegistry().lookupObject(destComponent);
            if (!(flowConstruct instanceof FlowConstruct && flowConstruct instanceof MessageProcessor))
            {
                return handleException(null, new DefaultMuleException(ClientMessages.noSuchFlowConstruct(destComponent)));
            }

            // Need to do this otherise when the event is invoked the
            // transformer associated with the Mule Admin queue will be invoked, but
            // the message will not be of expected type
            EndpointBuilder builder = new EndpointURIEndpointBuilder(inboundEndpoint);
            // TODO - is this correct? it stops any other transformer from being set
            builder.setTransformers(new LinkedList());
            InboundEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(builder);
            MuleEvent event = new DefaultMuleEvent(action.getMessage(), ep, context.getFlowConstruct(),
                context.getSession());
            event = RequestContext.setEvent(event);

            if (context.getExchangePattern().hasResponse())
            {
                MuleEvent resultEvent = ((MessageProcessor) flowConstruct).process(event);
                result = resultEvent == null || VoidMuleEvent.getInstance().equals(resultEvent)
                                                                                               ? null
                                                                                               : resultEvent.getMessage();
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
            else
            {
                ((MessageProcessor) flowConstruct).process(event);
                return null;
            }
        }
        else
        {
            return handleException(result, new DefaultMuleException(
                CoreMessages.couldNotDetermineDestinationComponentFromEndpoint(endpoint)));
        }
    }

    protected Object sendAction(RemoteDispatcherNotification action, MuleEventContext context) throws MuleException
    {
        MuleMessage result = null;
        OutboundEndpoint endpoint = null;
        MuleContext managementContext = context.getMuleContext();
        try
        {
            if (RemoteDispatcherNotification.ACTION_DISPATCH == action.getAction())
            {
                endpoint = managementContext.getEndpointFactory().getOutboundEndpoint(
                    action.getResourceIdentifier());
                context.dispatchEvent(action.getMessage(), endpoint);
                return null;
            }
            else
            {
                EndpointFactory endpointFactory = managementContext.getEndpointFactory();

                EndpointBuilder endpointBuilder = endpointFactory.getEndpointBuilder(action.getResourceIdentifier());
                endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);

                endpoint = managementContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
                result = context.sendEvent(action.getMessage(), endpoint);
                if (result == null || VoidMuleEvent.getInstance().equals(result))
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

    protected Object receiveAction(RemoteDispatcherNotification action, MuleEventContext context) throws MuleException
    {
        MuleMessage result = null;
        try
        {
            ImmutableEndpoint endpoint = context.getMuleContext().getEndpointFactory()
                .getOutboundEndpoint(action.getResourceIdentifier());

            long timeout = MapUtils.getLongValue(action.getProperties(),
                MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, getSynchronousEventTimeout());

            result = endpoint.getConnector().request(action.getResourceIdentifier(), timeout);
            if (result != null)
            {
                // See if there is a default transformer on the connector
                List transformers = ((AbstractConnector) endpoint.getConnector()).getDefaultInboundTransformers(endpoint);
                if (transformers != null)
                {
                    result.applyTransformers(null, transformers);
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


    public static Service getSerivce(InboundEndpoint endpoint,
                                                    WireFormat wireFormat,
                                                    String encoding,
                                                    int eventTimeout,
                                                    MuleContext muleContext) throws MuleException
    {
        try
        {
            Service service = new SedaService(muleContext);
            service.setName(MANAGER_COMPONENT_NAME);
            service.setModel(muleContext.getRegistry().lookupSystemModel());

            RemoteDispatcherComponent rdc = new RemoteDispatcherComponent(endpoint, wireFormat, encoding, new Integer(eventTimeout));
            
            final SimpleCallableJavaComponent component = new SimpleCallableJavaComponent(
                new SingletonObjectFactory(rdc));
            component.setMuleContext(muleContext);
            service.setComponent(component);


            if (!(service.getMessageSource() instanceof CompositeMessageSource))
            {
                throw new IllegalStateException("Only 'CompositeMessageSource' is supported with RemoteDispatcherService");
            }

            ((CompositeMessageSource) service.getMessageSource()).addSource(endpoint);

            return service;
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
    protected Object handleException(MuleMessage result, Throwable e)
    {
        logger.error("Failed to process admin request: " + e.getMessage(), e);
        if (result == null || VoidMuleEvent.getInstance().equals(result))
        {
            result = new DefaultMuleMessage(NullPayload.getInstance(), (Map) null, muleContext);
        }
        result.setExceptionPayload(new DefaultExceptionPayload(e));
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wireFormat.write(out, result, getEncoding());
            return out.toByteArray();
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
