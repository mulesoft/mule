/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.MessageRequester;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.ReceiveException;
import org.mule.context.notification.EndpointMessageNotification;

import java.util.List;

/**
 * The Message Requester is used to explicitly request messages from a message channel or
 * resource rather than subscribing to inbound events or polling for messages.
 * This is often used programatically but will not be used for inbound endpoints
 * configured on services.
 */
public abstract class AbstractMessageRequester extends AbstractTransportMessageHandler implements MessageRequester
{
    private List<Transformer> defaultInboundTransformers;

    public AbstractMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected ConnectableLifecycleManager createLifecycleManager()
    {
        return new ConnectableLifecycleManager<MessageRequester>(getRequesterName(), this);
    }

    /**
     * Method used to perform any initialisation work. If a fatal error occurs during
     * initialisation an <code>InitialisationException</code> should be thrown,
     * causing the Mule instance to shutdown. If the error is recoverable, say by
     * retrying to connect, a <code>RecoverableException</code> should be thrown.
     * There is no guarantee that by throwing a Recoverable exception that the Mule
     * instance will not shut down.
     * 
     * @throws org.mule.api.lifecycle.InitialisationException if a fatal error occurs
     *             causing the Mule instance to shutdown
     * @throws org.mule.api.lifecycle.RecoverableException if an error occurs that
     *             can be recovered from
     */
    @Override
    public final void initialise() throws InitialisationException
    {
        defaultInboundTransformers = connector.getDefaultInboundTransformers(endpoint);               
        super.initialise();
    }

    protected String getRequesterName()
    {
        return getConnector().getName() + ".requester." + System.identityHashCode(this);
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocol causes an exception
     */
    public final MuleMessage request(long timeout) throws Exception
    {
        try
        {
            EndpointMessageNotification beginNotification = null;
            if (connector.isEnableMessageEvents())
            {
                MuleMessage dummyMessage = new DefaultMuleMessage(NullPayload.getInstance(), endpoint.getMuleContext());
                beginNotification = new EndpointMessageNotification(dummyMessage, endpoint, null, EndpointMessageNotification.MESSAGE_REQUEST_BEGIN);
            }
            // Make sure we are connected
            connect();
            MuleMessage result = null;
            result = doRequest(timeout);
            if (result != null)
            {
                String rootId = result.getInboundProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY);
                if (rootId != null)
                {
                    result.setMessageRootId(rootId);
                    result.removeProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY, PropertyScope.INBOUND);
                }
                if (beginNotification != null)
                {
                    result.propagateRootId(beginNotification.getSource());
                }
                if (!endpoint.isDisableTransportTransformer())
                {
                    result = applyInboundTransformers(result);
                }
                if (beginNotification != null)
                {
                    connector.fireNotification(beginNotification);
                    connector.fireNotification(new EndpointMessageNotification(result, endpoint, null,
                        EndpointMessageNotification.MESSAGE_REQUEST_END));
                }
            }
            return result;
        }
        catch (ReceiveException e)
        {
            disposeAndLogException();
            throw e;
        }
        catch (Exception e)
        {
            disposeAndLogException();
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    protected MuleMessage applyInboundTransformers(MuleMessage message) throws MuleException
    {
        return getTransformationService().applyTransformers(message, null, defaultInboundTransformers);
    }

    @Override
    protected WorkManager getWorkManager() throws MuleException
    {
        return connector.getRequesterWorkManager();
    }
    
    public InboundEndpoint getEndpoint()
    {
        return (InboundEndpoint) super.getEndpoint();
    }
    
    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected abstract MuleMessage doRequest(long timeout) throws Exception;

}
