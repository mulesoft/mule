/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.glue;

import electric.glue.context.ApplicationContext;
import electric.glue.context.ServiceContext;
import electric.registry.Registry;
import electric.registry.RegistryException;
import electric.server.http.HTTP;
import electric.service.virtual.VirtualService;
import electric.util.Context;
import electric.util.interceptor.ReceiveThreadContext;
import electric.util.interceptor.SendThreadContext;
import org.mule.DisposeException;
import org.mule.InitialisationException;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleDescriptor;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.soap.ServiceProxy;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

import java.util.Iterator;
import java.util.Map;

/**
 * <code>GlueMessageReceiver</code> is used to recieve Glue bounded services
 * for Mule compoennts.
 *
 * services are bound in the Glue Registry using the Virtualservice implementation
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class GlueMessageReceiver extends AbstractMessageReceiver
{
    public GlueMessageReceiver(UMOConnector connector, UMOComponent component,
                               UMOEndpoint endpoint, boolean createServer) throws InitialisationException
    {
        super.create(connector, component, endpoint);
        try
        {
            Class[] interfaces = ServiceProxy.getInterfacesForComponent(component);
            if(interfaces.length==0) {
                throw new InitialisationException("No service interfaces could be found for component: " + component.getDescriptor().getName());
            }
            VirtualService.enable();
            VirtualService vService = new VirtualService(interfaces, ServiceProxy.createGlueServiceHandler(this,  endpoint.isSynchronous()));

            if(createServer) {
                registerContextHeaders();
                HTTP.startup(getEndpointURI().getScheme() + "://" + getEndpointURI().getHost() + ":" + getEndpointURI().getPort());
            }
            //Add initialisation callback for the Glue service
            //The callback will actually register the service
            MuleDescriptor desc =(MuleDescriptor)component.getDescriptor();
            String serviceName = getEndpointURI().getPath();
            if(!serviceName.endsWith("/")) {
                serviceName += "/";
            }
            serviceName += component.getDescriptor().getName();
            desc.addInitialisationCallback(new GlueInitialisationCallback(vService, serviceName, new ServiceContext()));

        } catch (ClassNotFoundException e)
        {
            throw new InitialisationException("Failed to load component class: " + e.getMessage(), e);
        }  catch (UMOException e)
        {
            throw new InitialisationException("Failed to register component as a service: " + e.getMessage(), e);
        }catch (Exception e)
        {
            throw new InitialisationException("Failed to start soap server: " + e.getMessage(), e);
        }
    }

    protected void registerContextHeaders()
    {
        ApplicationContext.addOutboundSoapRequestInterceptor(new SendThreadContext(MuleProperties.MULE_CORRELATION_ID_PROPERTY));
        ApplicationContext.addOutboundSoapRequestInterceptor(new SendThreadContext(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY));
        ApplicationContext.addOutboundSoapRequestInterceptor(new SendThreadContext(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY));
        ApplicationContext.addOutboundSoapRequestInterceptor(new SendThreadContext(MuleProperties.MULE_REPLY_TO_PROPERTY, true));

        ApplicationContext.addInboundSoapRequestInterceptor( new ReceiveThreadContext( MuleProperties.MULE_CORRELATION_ID_PROPERTY ) );
        ApplicationContext.addInboundSoapRequestInterceptor( new ReceiveThreadContext( MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY ) );
        ApplicationContext.addInboundSoapRequestInterceptor( new ReceiveThreadContext( MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY ) );
        ApplicationContext.addInboundSoapRequestInterceptor( new ReceiveThreadContext( MuleProperties.MULE_REPLY_TO_PROPERTY, true ) );
    }


    /**
     * Template method to dispose any resources associated with this receiver.  There
     * is not need to dispose the connector as this is already done by the framework
     *
     * @throws org.mule.umo.UMOException
     */
    protected void doDispose() throws UMOException
    {
        try
        {
            Registry.unpublish(component.getDescriptor().getName());
        } catch (RegistryException e)
        {
            throw new DisposeException("Failed to unregister soap service: " + e.getMessage(), e);
        }
    }

    protected Context getContext() {
        Context c = null;
        if(endpoint.getProperties() != null) {
            c = (Context)endpoint.getProperties().get("glueContext");
            if(c==null && endpoint.getProperties().size() > 0) {
                c = new Context();
                for (Iterator iterator = endpoint.getProperties().entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    c.addProperty(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return c;
    }
}
