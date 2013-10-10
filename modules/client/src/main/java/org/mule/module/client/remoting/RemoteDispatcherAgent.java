/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.client.remoting;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.client.remoting.notification.RemoteDispatcherNotification;
import org.mule.module.client.remoting.notification.RemoteDispatcherNotificationListener;
import org.mule.transformer.wire.SerializedMuleMessageWireFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>RemoteDispatcherAgent</code> manages the server endpoint that receives Admin and
 * remote client requests
 */
public class RemoteDispatcherAgent extends AbstractAgent
{
    public static final String AGENT_NAME = "RemoteDispatcherServer";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(RemoteDispatcherAgent.class);

    private WireFormat wireFormat;

    private InboundEndpoint endpoint;


    public RemoteDispatcherAgent()
    {
        super(AGENT_NAME);
    }

    /**
     * Should be a 1 line description of the agent
     */
    public String getDescription()
    {
        return getName() + ": accepting connections on " + endpoint.getEndpointURI().getAddress();
    }

    public void start() throws MuleException
    {
        // nothing to do
    }

    public void stop() throws MuleException
    {
        // nothing to do
    }

    public void dispose()
    {
        // nothing to do
    }


    public void initialise() throws InitialisationException
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("remote-endpoint").getMessage());
        }

        if (wireFormat == null)
        {
            wireFormat = new SerializedMuleMessageWireFormat();
        }
        wireFormat.setMuleContext(muleContext);

        
        //Register the Remote Dispatcher Notification support
        muleContext.getNotificationManager().addInterfaceToType(
                RemoteDispatcherNotificationListener.class,
                RemoteDispatcherNotification.class);

        try
        {
            // Check for override
            if (muleContext.getRegistry().lookupService(RemoteDispatcherComponent.MANAGER_COMPONENT_NAME) != null)
            {
                logger.info("Mule manager component has already been initialised, ignoring server url");
            }
            else
            {

                Service component = RemoteDispatcherComponent.getSerivce(endpoint, wireFormat,
                    muleContext.getConfiguration().getDefaultEncoding(), muleContext.getConfiguration()
                        .getDefaultResponseTimeout(), muleContext);
                muleContext.getRegistry().registerService(component);
            }
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public String toString()
    {
        String address = "not set";
        if(endpoint!=null)
        {
             address = endpoint.getEndpointURI().getAddress();
        }
        return "RemoteDispatcherAgent{" + "remote-endpoint='" + address + "'" + "}";
    }

    public WireFormat getWireFormat()
    {
        return wireFormat;
    }

    public void setWireFormat(WireFormat wireFormat)
    {
        this.wireFormat = wireFormat;
    }

    public InboundEndpoint getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(InboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }
}
