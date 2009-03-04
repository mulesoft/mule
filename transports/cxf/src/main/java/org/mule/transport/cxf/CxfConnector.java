/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.spring.SpringRegistry;
import org.mule.context.notification.MuleContextNotification;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.model.seda.SedaService;
import org.mule.object.SingletonObjectFactory;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.transport.AbstractConnector;
import org.mule.transport.cxf.transport.MuleUniversalTransport;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import edu.emory.mathcs.backport.java.util.Collections;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.springframework.context.ApplicationContext;

/**
 * Connects Mule to a CXF bus instance.
 */
public class CxfConnector extends AbstractConnector implements MuleContextNotificationListener
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
    private List<SedaService> services = Collections.synchronizedList(new ArrayList<SedaService>());
    private Map<String, Server> uriToServer = new HashMap<String, Server>();
    private boolean initializeStaticBusInstance = true;
    
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
    }
    
    public boolean supportsProtocol(String protocol)
    {
        // we can listen on any protocol
        return protocol.startsWith("cxf:") || super.supportsProtocol(protocol);
    }
    
    public String getProtocol()
    {
        return CXF;
    }

    protected void doInitialise() throws InitialisationException
    {
        ApplicationContext context = (ApplicationContext) muleContext.getRegistry().lookupObject(SpringRegistry.SPRING_APPLICATION_CONTEXT);
        
        if (configurationLocation != null)
        {
            bus = new SpringBusFactory(context).createBus(configurationLocation, true);
        }
        else
        {
            bus = new SpringBusFactory(context).createBus((String)null, true);
        }
        
        if (!initializeStaticBusInstance)
        {
            BusFactory.setDefaultBus(null);
        }
        
        MuleUniversalTransport transport = new MuleUniversalTransport(this);
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/http", transport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http", transport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http/configuration", transport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/http/", transport);
        dfm.registerDestinationFactory("http://www.w3.org/2003/05/soap/bindings/HTTP/", transport);
        dfm.registerDestinationFactory(MuleUniversalTransport.TRANSPORT_ID, transport);

        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http", transport);
        extension.registerConduitInitiator(MuleUniversalTransport.TRANSPORT_ID, transport);
        
        // Registers the listener
        try
        {
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
    protected void registerReceiverWithMuleService(MessageReceiver receiver, EndpointURI ep) throws MuleException
    {
        CxfMessageReceiver cxfReceiver = (CxfMessageReceiver) receiver;
        Server server = cxfReceiver.getServer();

        uriToServer.put(server.getEndpoint().getEndpointInfo().getAddress(), server);
        
        // TODO MULE-2228 Simplify this API
        SedaService c = new SedaService();
        c.setName(CXF_SERVICE_COMPONENT_NAME + server.getEndpoint().getService().getName() + c.hashCode());
        c.setModel(muleContext.getRegistry().lookupSystemModel());

        CxfServiceComponent svcComponent = new CxfServiceComponent(this, (CxfMessageReceiver) receiver);
        svcComponent.setBus(bus);

        c.setComponent(new DefaultJavaComponent(new SingletonObjectFactory(svcComponent)));

        // No determine if the endpointUri requires a new connector to be
        // registed in the case of http we only need to register the new
        // endpointUri if the port is different
        String endpoint = receiver.getEndpointURI().getAddress();
        String scheme = ep.getScheme().toLowerCase();

        InboundEndpoint originalEndpoint = receiver.getEndpoint();
        boolean sync = originalEndpoint.isSynchronous();

        // If we are using sockets then we need to set the endpoint name appropiately
        // and if using http/https
        // we need to default to POST and set the Content-Type
        if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ssl") || scheme.equals("tcp")
            || scheme.equals("servlet"))
        {
            originalEndpoint.getProperties().put(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
            originalEndpoint.getProperties().put(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
        }

        QName serviceName = server.getEndpoint().getEndpointInfo().getName();
        
        EndpointBuilder protocolEndpointBuilder = new EndpointURIEndpointBuilder(endpoint, muleContext);
        protocolEndpointBuilder.setSynchronous(sync);
        protocolEndpointBuilder.setName(ep.getScheme() + ":" + serviceName.getLocalPart());
        protocolEndpointBuilder.setTransactionConfig(originalEndpoint.getTransactionConfig());
        
        EndpointBuilder receiverEndpointBuilder = new EndpointURIEndpointBuilder(originalEndpoint,
            muleContext);
        
        // Apply the transformers to the correct endpoint
        EndpointBuilder transformerEndpoint;
        if (cxfReceiver.isApplyTransformersToProtocol())
        {
            transformerEndpoint = protocolEndpointBuilder; 
            receiverEndpointBuilder.setTransformers(Collections.emptyList());
            receiverEndpointBuilder.setResponseTransformers(Collections.emptyList());
        }
        else
        {  
            transformerEndpoint = receiverEndpointBuilder;
        }
        
        // Ensure that the transformers aren't empty before setting them. Otherwise Mule will get confused
        // and won't add the default transformers.
        if (originalEndpoint.getTransformers() != null && !originalEndpoint.getTransformers().isEmpty())
        {
            transformerEndpoint.setTransformers(originalEndpoint.getTransformers());
        }

        if (originalEndpoint.getResponseTransformers() != null && !originalEndpoint.getResponseTransformers().isEmpty())
        {
            transformerEndpoint.setResponseTransformers(originalEndpoint.getResponseTransformers());
        }
        
        // apply the filters to the correct endpoint
        EndpointBuilder filterEndpoint;
        if (cxfReceiver.isApplyFiltersToProtocol())
        {
            filterEndpoint = protocolEndpointBuilder;   
            receiverEndpointBuilder.setFilter(null);                                                                                                
        }
        else
        {  
            filterEndpoint = receiverEndpointBuilder;
        }
        filterEndpoint.setFilter(originalEndpoint.getFilter());
        
        // apply the security filter to the correct endpoint
        EndpointBuilder secFilterEndpoint;
        if (cxfReceiver.isApplySecurityToProtocol())
        {
            secFilterEndpoint = protocolEndpointBuilder;   
            receiverEndpointBuilder.setSecurityFilter(null);                                                                                               
        }
        else
        {  
            secFilterEndpoint = receiverEndpointBuilder;
        }             
        secFilterEndpoint.setSecurityFilter(originalEndpoint.getSecurityFilter());

        String connectorName = (String) originalEndpoint.getProperty(CxfConstants.PROTOCOL_CONNECTOR);
        if (connectorName != null) 
        {
            protocolEndpointBuilder.setConnector(muleContext.getRegistry().lookupConnector(connectorName));
        }
        
        InboundEndpoint protocolEndpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(protocolEndpointBuilder);

        InboundEndpoint receiverEndpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(receiverEndpointBuilder);

        receiver.setEndpoint(receiverEndpoint);
        
        c.setInboundRouter(new DefaultInboundRouterCollection());
        c.getInboundRouter().addEndpoint(protocolEndpoint);
        services.add(c);
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
    protected Object getReceiverKey(Service service, InboundEndpoint endpoint)
    {
        if (service.getName().startsWith("_cxfServiceComponent"))
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
        if (event.getAction() == MuleContextNotification.CONTEXT_STARTED)
        {
            for (Service c : services)
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
    
    public boolean isSyncEnabled(String protocol)
    {
        protocol = protocol.toLowerCase();
        if (protocol.equals("http") || protocol.equals("https") || protocol.equals("ssl") || protocol.equals("tcp")
            || protocol.equals("servlet"))
        {
            return true;
        }
        else
        {
            return super.isSyncEnabled(protocol);
        }
    }

    public Server getServer(String uri)
    {
        return uriToServer.get(uri);
    }

    public boolean isInitializeStaticBusInstance()
    {
        return initializeStaticBusInstance;
    }

    public void setInitializeStaticBusInstance(boolean initializeStaticBusInstance)
    {
        this.initializeStaticBusInstance = initializeStaticBusInstance;
    }

}
