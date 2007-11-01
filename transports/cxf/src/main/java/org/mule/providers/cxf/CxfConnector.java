/*
 * $Id: CxfConnector.java 6612 2007-05-18 05:28:06Z hasari $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.ManagerNotificationListener;
import org.mule.impl.internal.notifications.NotificationException;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.providers.AbstractConnector;
import org.mule.providers.cxf.transport.MuleUniversalTransport;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.object.SingletonObjectFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;

/**
 * Connects Mule to a CXF bus instance.
 */
public class CxfConnector extends AbstractConnector implements ManagerNotificationListener
{
    public static final String CXF_SERVICE_COMPONENT_NAME = "_cxfServiceComponent";
    public static final String CONFIGURATION_LOCATION = "configurationLocation";
    public static final String DEFAULT_MULE_NAMESPACE_URI = "http://www.muleumo.org";
    public static final String BUS_PROPERTY = "cxf";

    // The CXF Bus object
    private Bus bus;
    private String configurationLocation;
    private String defaultFrontend = CxfConstants.JAX_WS_FRONTEND;
    private List<SedaComponent> components = new ArrayList<SedaComponent>();
    
    public CxfConnector()
    {
        super();
        registerProtocols();
        this.setEnableMessageEvents(true);
    }

    protected void registerProtocols()
    {
        registerSupportedProtocol("http");
        registerSupportedProtocol("https");
        registerSupportedProtocol("jms");
        registerSupportedProtocol("vm");
        registerSupportedProtocol("servlet");
    }

    public String getProtocol()
    {
        return "cxf";
    }

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            managementContext.registerListener(this);
        }
        catch (NotificationException e)
        {
            throw new InitialisationException(e, this);
        }
        if (configurationLocation != null)
        {
            bus = new SpringBusFactory().createBus(configurationLocation);
        }
        else
        {
            bus = new SpringBusFactory().createBus();
        }

        MuleUniversalTransport transport = new MuleUniversalTransport(this);
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/http", transport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http", transport);
        dfm.registerDestinationFactory(MuleUniversalTransport.TRANSPORT_ID, transport);

        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http", transport);
        extension.registerConduitInitiator(MuleUniversalTransport.TRANSPORT_ID, transport);
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {

    }

    protected void doStop() throws UMOException
    {
        bus.shutdown(true);
    }

    public Bus getCxfBus()
    {
        return bus;
    }

    public void setCxfBus(Bus bus)
    {
        this.bus = bus;
    }

    public String getConfigurationLocation()
    {
        return configurationLocation;
    }

    public void setConfigurationLocation(String configurationLocation)
    {
        this.configurationLocation = configurationLocation;
    }

    public String getDefaultFrontend()
    {
        return defaultFrontend;
    }

    public void setDefaultFrontend(String defaultFrontend)
    {
        this.defaultFrontend = defaultFrontend;
    }

    @SuppressWarnings("unchecked")
    protected void registerReceiverWithMuleService(UMOMessageReceiver receiver, UMOEndpointURI ep)
        throws UMOException
    {
        CxfMessageReceiver cxfReceiver = (CxfMessageReceiver) receiver;
        Server server = cxfReceiver.getServer();

        // TODO MULE-2228 Simplify this API
        SedaComponent c = new SedaComponent();
        c.setName(CXF_SERVICE_COMPONENT_NAME + server.getEndpoint().getService().getName());            
        c.setModel(managementContext.getRegistry().lookupSystemModel());
        
        CxfServiceComponent svcComponent = new CxfServiceComponent((CxfMessageReceiver)receiver);
        svcComponent.setBus(bus);
        
        SingletonObjectFactory of = new SingletonObjectFactory(svcComponent);
        of.setComponent(c);
        of.initialise();
        c.setServiceFactory(of);
        
        // No determine if the endpointUri requires a new connector to be
        // registed in the case of http we only need to register the new
        // endpointUri if the port is different
        String endpoint = receiver.getEndpointURI().getAddress();
        String scheme = ep.getScheme().toLowerCase();

        boolean sync = receiver.getEndpoint().isSynchronous();

        // If we are using sockets then we need to set the endpoint name appropiately
        // and if using http/https
        // we need to default to POST and set the Content-Type
        if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ssl") || scheme.equals("tcp")
            || scheme.equals("servlet"))
        {
            receiver.getEndpoint().getProperties().put(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
            receiver.getEndpoint().getProperties().put(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");

            // Default to using synchronous for socket based protocols unless the
            // synchronous property has been set explicitly
            if (!receiver.getEndpoint().isSynchronousSet())
            {
                sync = true;
            }
        }

        QName serviceName = server.getEndpoint().getEndpointInfo().getName();

        UMOEndpointBuilder builder = new EndpointURIEndpointBuilder(endpoint, managementContext);
        builder.setSynchronous(sync);
        builder.setName(ep.getScheme() + ":" + serviceName.getLocalPart());

        // Set the transformers on the endpoint too
        builder.setTransformers(receiver.getEndpoint().getTransformers());
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setTransformers(new LinkedList());

        builder.setResponseTransformers(receiver.getEndpoint().getResponseTransformers());
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setResponseTransformers(new LinkedList());

        // set the filter on the axis endpoint on the real receiver endpoint
        builder.setFilter(receiver.getEndpoint().getFilter());
        // Remove the Axis filter now
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setFilter(null);

        // set the Security filter on the axis endpoint on the real receiver
        // endpoint
        builder.setSecurityFilter(receiver.getEndpoint().getSecurityFilter());
        // Remove the Axis Receiver Security filter now
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setSecurityFilter(null);

        UMOImmutableEndpoint serviceEndpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(builder, managementContext);

        c.setInboundRouter(new InboundRouterCollection());
        c.getInboundRouter().addEndpoint(serviceEndpoint);
        
        components.add(c);
    }

    /**
     * The method determines the key used to store the receiver against.
     * 
     * @param component the component for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the component
     * @return the key to store the newly created receiver against. In this case it
     *         is the component name, which is equivilent to the Axis service name.
     */
    @Override
    protected Object getReceiverKey(UMOComponent component, UMOImmutableEndpoint endpoint)
    {
        if (endpoint.getEndpointURI().getPort() == -1)
        {
            return component.getName();
        }
        else
        {
            return endpoint.getEndpointURI().getAddress();
        }
    }

    public void onNotification(UMOServerNotification event)
    {
        // We need to register the CXF service component once the model
        // starts because
        // when the model starts listeners on components are started, thus
        // all listener
        // need to be registered for this connector before the CXF service
        // component is registered. The implication of this is that to add a
        // new service and a
        // different http port the model needs to be restarted before the
        // listener is available
        if (event.getAction() == ManagerNotification.MANAGER_STARTED)
        {
            for (UMOComponent c : components)
            {
                try
                {
                    managementContext.getRegistry().registerComponent(c, managementContext);
                }
                catch (UMOException e)
                {
                    handleException(e);
                }
            }
        }
    }
}
