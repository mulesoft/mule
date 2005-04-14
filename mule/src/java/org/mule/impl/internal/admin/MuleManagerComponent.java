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
 */
package org.mule.impl.internal.admin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.internal.events.AdminEvent;
import org.mule.providers.AbstractConnector;
import org.mule.transformers.xml.XmlToObject;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.PropertiesHelper;

/**
 * <code>MuleManagerComponent</code> is a MuleManager interal server component responsible
 * for receiving remote requests as dispatching them locally
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleManagerComponent implements Callable, Initialisable
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleManagerComponent.class);

    public static final String MANAGER_COMPONENT_NAME = "_muleManagerComponent";
    public static final String MANAGER_PROVIDER_NAME = "_muleManagerProvider";

    private XmlToObject remoteTransformer;
    private XStream xstream;

    public void initialise() throws InitialisationException
    {
        xstream = new XStream(new XppDriver());
        remoteTransformer = new XmlToObject();
        remoteTransformer.setReturnClass(AdminEvent.class);
    }
    public Object onCall(UMOEventContext context) throws Exception
    {
        String xml = context.getMessageAsString();
        logger.debug("Message received by MuleManagerComponent");
        AdminEvent action = (AdminEvent)remoteTransformer.transform(xml);
        if(AdminEvent.ACTION_INVOKE == action.getAction()) {
            return invokeAction(action, context);
        } else if(AdminEvent.ACTION_SEND==action.getAction()) {
            return sendAction(action, context);
        } else if(AdminEvent.ACTION_DISPATCH==action.getAction()) {
            return sendAction(action, context);
        } else if(AdminEvent.ACTION_RECEIVE==action.getAction()) {
            return receiveAction(action, context);
        }
        return null;
    }

    protected Object invokeAction(AdminEvent action, UMOEventContext context) throws UMOException
    {
        String destComponent = null;
        String endpoint = action.getEndpoint();
        if(action.getEndpoint().startsWith("mule:")) {
            destComponent = endpoint.substring(endpoint.lastIndexOf("/")+ 1);
        } else {
            destComponent = endpoint;
        }

        if(destComponent!=null) {
            UMOSession session = MuleManager.getInstance().getModel().getComponentSession(destComponent);
            RequestContext.rewriteEvent(action.getMessage());
            //Need to do this otherise when the evne tis invoked the transformer
            //Associated with the Mule Admin queue will be invoked, btu the message
            //will not be of expected type
            UMOEvent event = RequestContext.getEvent();
            event.getEndpoint().setTransformer(null);
            if(context.isSynchronous()) {

                UMOMessage result = session.getComponent().sendEvent(event);
                return xstream.toXML(result);
            } else {
                session.getComponent().dispatchEvent(event);
                return null;
            }
        } else {
            throw new MuleException(new Message(Messages.EVENT_PROPERTY_X_NOT_SET_CANT_PROCESS_REQUEST, MuleProperties.COMPONENT_NAME_PROPERTY));
        }
    }

    protected Object sendAction(AdminEvent action, UMOEventContext context) throws UMOException
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI(action.getEndpoint());
        try
        {
            if(AdminEvent.ACTION_DISPATCH==action.getAction()) {
                context.dispatchEvent(action.getMessage(), endpointUri);
                return null;
            } else {
                UMOMessage result = context.sendEvent(action.getMessage(), endpointUri);
                if(result!=null) {
                    return xstream.toXML(result);
                } else {
                    return null;
                }
            }
        } catch (Exception e)
        {
            throw new DispatchException(action.getMessage(), new MuleEndpoint(action.getEndpoint(), true), e);
        }

    }

    protected Object receiveAction(AdminEvent action, UMOEventContext context) throws UMOException
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI(action.getEndpoint());
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);

        UMOMessageDispatcher dispatcher = endpoint.getConnector().getDispatcher(action.getEndpoint());
        long timeout = PropertiesHelper.getLongProperty(action.getProperties(), "timeout", MuleManager.getConfiguration().getSynchronousEventTimeout());

        try
        {
            UMOEndpointURI ep = new MuleEndpointURI(action.getEndpoint());
            UMOMessage result = dispatcher.receive(ep, timeout);
            if(result!=null) {
                //See if there is a default transformer on the connector
                UMOTransformer trans = ((AbstractConnector)endpoint.getConnector()).getDefaultInboundTransformer();
                if(trans!=null) {
                    Object payload = trans.transform(result.getPayload());
                    result = new MuleMessage(payload, result.getProperties());
                }
                return xstream.toXML(result);
            } else {
                return null;
            }
        } catch (Exception e)
        {
            throw new ReceiveException(endpointUri, timeout, e);
        }

    }

    public static final UMODescriptor getDescriptor(UMOConnector connector, UMOEndpointURI endpointUri) throws UMOException
    {
        UMOEndpoint endpoint = new MuleEndpoint();
        endpoint.setConnector(connector);
        endpoint.setEndpointURI(endpointUri);
        endpoint.setName(MANAGER_PROVIDER_NAME);
        endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_RECEIVER);

        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setName(MANAGER_COMPONENT_NAME);
        descriptor.setInboundEndpoint(endpoint);
        descriptor.setImplementation(MuleManagerComponent.class.getName());
        descriptor.setContainerManaged(false);
        return descriptor;
    }
}
