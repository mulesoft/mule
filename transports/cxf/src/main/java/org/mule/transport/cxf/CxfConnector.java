/*
 * $Id: CxfConnector.java 6612 2007-05-18 05:28:06Z hasari $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.context.notification.ManagerNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.context.notification.ManagerNotification;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.model.seda.SedaService;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.AbstractConnector;
import org.mule.transport.cxf.transport.MuleUniversalTransport;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.object.SingletonObjectFactory;

import java.util.ArrayList;
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

    public static final String CXF = "cxf";
    public static final String CXF_SERVICE_COMPONENT_NAME = "_cxfServiceComponent";
    public static final String CONFIGURATION_LOCATION = "configurationLocation";
    public static final String DEFAULT_MULE_NAMESPACE_URI = "http://www.muleumo.org";
    public static final String BUS_PROPERTY = CXF;

    // The CXF Bus object
    private Bus bus;
    private String configurationLocation;
    private String defaultFrontend = CxfConstants.JAX_WS_FRONTEND;
    private List<SedaService> components = new ArrayList<SedaService>();
    
    public CxfConnector()
    {
        super();
        registerProtocols();
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
        return CXF;
    }

    protected void doInitialise() throws InitialisationException
    {
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
        
        // Registers the listener
        try{
        	muleContext.registerListener(this);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
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

    protected void doStart() throws MuleException
    {

    }

    protected void doStop() throws MuleException
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
    protected void registerReceiverWithMuleService(MessageReceiver receiver, EndpointURI ep)
        throws MuleException
    {
        CxfMessageReceiver cxfReceiver = (CxfMessageReceiver) receiver;
        Server server = cxfReceiver.getServer();

        // TODO MULE-2228 Simplify this API
        SedaService c = new SedaService();
        c.setName(CXF_SERVICE_COMPONENT_NAME + server.getEndpoint().getService().getName() + c.hashCode());            
        c.setModel(muleContext.getRegistry().lookupSystemModel());
        
        CxfServiceComponent svcComponent = new CxfServiceComponent((CxfMessageReceiver)receiver);
        svcComponent.setBus(bus);
        
        SingletonObjectFactory of = new SingletonObjectFactory(svcComponent);
        of.setService(c);
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
        }

        QName serviceName = server.getEndpoint().getEndpointInfo().getName();

        EndpointBuilder serviceEndpointbuilder = new EndpointURIEndpointBuilder(endpoint, muleContext);
        serviceEndpointbuilder.setSynchronous(sync);
        serviceEndpointbuilder.setName(ep.getScheme() + ":" + serviceName.getLocalPart());
        // Set the transformers on the endpoint too
        serviceEndpointbuilder.setTransformers(receiver.getEndpoint().getTransformers());
        serviceEndpointbuilder.setResponseTransformers(receiver.getEndpoint().getResponseTransformers());
        // set the filter on the axis endpoint on the real receiver endpoint
        serviceEndpointbuilder.setFilter(receiver.getEndpoint().getFilter());
        // set the Security filter on the axis endpoint on the real receiver
        // endpoint
        serviceEndpointbuilder.setSecurityFilter(receiver.getEndpoint().getSecurityFilter());

        // TODO Do we really need to modify the existing receiver endpoint? What happnes if we don't security,
        // filters and transformers will get invoked twice?
        EndpointBuilder receiverEndpointBuilder = new EndpointURIEndpointBuilder(receiver.getEndpoint(),
            muleContext);
        receiverEndpointBuilder.setTransformers(TransformerUtils.UNDEFINED);
        receiverEndpointBuilder.setResponseTransformers(TransformerUtils.UNDEFINED);
        // Remove the Axis filter now
        receiverEndpointBuilder.setFilter(null);
        // Remove the Axis Receiver Security filter now
        receiverEndpointBuilder.setSecurityFilter(null);

        ImmutableEndpoint serviceEndpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(serviceEndpointbuilder);

        ImmutableEndpoint receiverEndpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(receiverEndpointBuilder);

        receiver.setEndpoint(receiverEndpoint);
        
        c.setInboundRouter(new DefaultInboundRouterCollection());
        c.getInboundRouter().addEndpoint(serviceEndpoint);
        
        components.add(c);
    }

    /**
     * The method determines the key used to store the receiver against.
     * 
     * @param service the service for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the service
     * @return the key to store the newly created receiver against. In this case it
     *         is the service name, which is equivilent to the Axis service name.
     */
    @Override
    protected Object getReceiverKey(Service service, ImmutableEndpoint endpoint)
    {
        if (endpoint.getEndpointURI().getPort() == -1)
        {
            return service.getName();
        }
        else
        {
            return endpoint.getEndpointURI().getAddress();
        }
    }

    public void onNotification(ServerNotification event)
    {
        // We need to register the CXF service service once the model
        // starts because
        // when the model starts listeners on components are started, thus
        // all listener
        // need to be registered for this connector before the CXF service
        // service is registered. The implication of this is that to add a
        // new service and a
        // different http port the model needs to be restarted before the
        // listener is available
        if (event.getAction() == ManagerNotification.MANAGER_STARTED)
        {
            for (Service c : components)
            {
                try
                {
                    muleContext.getRegistry().registerService(c);
                }
                catch (MuleException e)
                {
                    handleException(e);
                }
            }
        }
    }
    
    public boolean isSyncEnabled(ImmutableEndpoint endpoint)
    {
        String scheme = endpoint.getEndpointURI().getScheme().toLowerCase();
        if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ssl") || scheme.equals("tcp")
            || scheme.equals("servlet"))
        {
            return true;
        }
        else
        {
            return super.isSyncEnabled(endpoint);
        }
    }
}
