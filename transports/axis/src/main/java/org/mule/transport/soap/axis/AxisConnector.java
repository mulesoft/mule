/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.SecurityFilter;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.transport.MessageReceiver;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.endpoint.AbstractEndpointBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.model.seda.SedaService;
import org.mule.object.SingletonObjectFactory;
import org.mule.processor.SecurityFilterMessageProcessor;
import org.mule.routing.MessageFilter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.transport.AbstractConnector;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.servlet.ServletConnector;
import org.mule.transport.soap.axis.extensions.MuleConfigProvider;
import org.mule.transport.soap.axis.extensions.MuleTransport;
import org.mule.transport.soap.axis.extensions.WSDDFileProvider;
import org.mule.transport.soap.axis.extensions.WSDDJavaMuleProvider;
import org.mule.transport.soap.axis.i18n.AxisMessages;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.MuleUrlStreamHandlerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axis.AxisProperties;
import org.apache.axis.client.Call;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.deployment.wsdd.WSDDConstants;
import org.apache.axis.deployment.wsdd.WSDDProvider;
import org.apache.axis.encoding.TypeMappingRegistryImpl;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.server.AxisServer;
import org.apache.axis.wsdl.fromJava.Namespaces;
import org.apache.axis.wsdl.fromJava.Types;

/**
 * <code>AxisConnector</code> is used to maintain one or more Services for Axis
 * server instance.
 * <p/>
 * Some of the Axis specific service initialisation code was adapted from the Ivory
 * project (http://ivory.codehaus.org). Thanks guys :)
 */
public class AxisConnector extends AbstractConnector implements MuleContextNotificationListener<MuleContextNotification>
{
    /* Register the AxisFault Exception reader if this class gets loaded */
    static
    {
        ExceptionHelper.registerExceptionReader(new AxisFaultExceptionReader());
    }

    public static final QName QNAME_MULE_PROVIDER = new QName(WSDDConstants.URI_WSDD_JAVA, "Mule");
    public static final QName QNAME_MULE_TYPE_MAPPINGS = new QName("http://www.muleumo.org/ws/mappings",
            "Mule");
    public static final String DEFAULT_MULE_NAMESPACE_URI = "http://www.muleumo.org";

    public static final String DEFAULT_MULE_AXIS_SERVER_CONFIG = "mule-axis-server-config.wsdd";
    public static final String DEFAULT_MULE_AXIS_CLIENT_CONFIG = "mule-axis-client-config.wsdd";
    public static final String AXIS_SERVICE_COMPONENT_NAME = "_axisServiceComponent";
    public static final String AXIS_SERVICE_PROPERTY = "_axisService";
    public static final String AXIS_CLIENT_CONFIG_PROPERTY = "clientConfig";

    public static final String SERVICE_PROPERTY_COMPONENT_NAME = "componentName";
    public static final String SERVICE_PROPERTY_SERVCE_PATH = "servicePath";

    public static final String AXIS = "axis";

    // used by dispatcher and receiver
    public static final String SOAP_METHODS = "soapMethods";
    public static final String STYLE = "style";
    public static final String USE = "use";

    private String serverConfig = DEFAULT_MULE_AXIS_SERVER_CONFIG;

    private AxisServer axis = null;
    private SimpleProvider serverProvider = null;
    private String clientConfig = DEFAULT_MULE_AXIS_CLIENT_CONFIG;
    private SimpleProvider clientProvider = null;

    private List<?> beanTypes;
    private Service axisComponent;

    /**
     * These protocols will be set on client invocations. By default Mule uses it's
     * own transports rather that Axis's. This is only because it gives us more
     * flexibility inside Mule and simplifies the code
     */
    private Map<String, Class<?>> axisTransportProtocols = null;

    /**
     * A store of registered servlet services that need to have their endpoints
     * re-written with the 'real' http url instead of the servlet:// one. This is
     * only required to ensure wsdl is generated correctly. I would like a clearer
     * way of doing this so I can remove this workaround
     */
    private List<SOAPService> servletServices = new ArrayList<SOAPService>();

    private List<String> supportedSchemes = null;

    private boolean doAutoTypes = true;

    private boolean treatMapAsNamedParams = true;

    public AxisConnector(MuleContext context)
    {
        super(context);
        this.registerProtocols();
    }

    protected void registerProtocols()
    {
        if (supportedSchemes == null)
        {
            // Default supported schemes, these can be restricted
            // through configuration
            supportedSchemes = new ArrayList<String>();
            supportedSchemes.add("http");
            supportedSchemes.add("https");
            supportedSchemes.add("servlet");
            supportedSchemes.add("vm");
            supportedSchemes.add("jms");
            supportedSchemes.add("xmpp");
            supportedSchemes.add("ssl");
            supportedSchemes.add("tcp");
            supportedSchemes.add("smtp");
            supportedSchemes.add("smtps");
            supportedSchemes.add("pop3");
            supportedSchemes.add("pop3s");
            supportedSchemes.add("imap");
            supportedSchemes.add("imaps");
        }

        for (String scheme : supportedSchemes)
        {
            registerSupportedProtocol(scheme);
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        axisTransportProtocols = new HashMap<String, Class<?>>();
        try
        {
            for (String scheme : supportedSchemes)
            {
                axisTransportProtocols.put(scheme, MuleTransport.getTransportClass(scheme));
                registerSupportedProtocol(scheme);
            }
            muleContext.registerListener(this);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
        // TODO DO: call registerSupportedProtocol if axisTransportProtocols are set from external?

        if (clientProvider == null)
        {
            clientProvider = createAxisProvider(clientConfig);
        }
        else
        {
            if (!DEFAULT_MULE_AXIS_CLIENT_CONFIG.equals(clientConfig))
            {
                logger.warn(AxisMessages.clientProviderAndClientConfigConfigured());
            }
        }

        if (axis == null)
        {
            if (serverProvider == null)
            {
                serverProvider = this.createAxisProvider(serverConfig);
            }
            else
            {
                if (!DEFAULT_MULE_AXIS_SERVER_CONFIG.equals(serverConfig))
                {
                    logger.warn(AxisMessages.serverProviderAndServerConfigConfigured());
                }
            }

            // Create the AxisServer
            axis = new AxisServer(serverProvider);
            // principle of least surprise: doAutoTypes only has effect on our self-configured AxisServer
            axis.setOption("axis.doAutoTypes", Boolean.valueOf(doAutoTypes));
        }

        // Register the Mule service serverProvider
        WSDDProvider.registerProvider(QNAME_MULE_PROVIDER, new WSDDJavaMuleProvider(this));

        try
        {
            registerTransportTypes();
        }
        catch (ClassNotFoundException e)
        {
            throw new InitialisationException(
                    CoreMessages.cannotLoadFromClasspath(e.getMessage()), e, this);
        }

        // Register all our UrlStreamHandlers here so they can be resolved. This is necessary
        // to make Mule work in situations where modification of system properties at runtime
        // is not reliable, e.g. when running in maven's surefire test executor.
        MuleUrlStreamHandlerFactory.registerHandler("jms", new org.mule.transport.soap.axis.transport.jms.Handler());
        MuleUrlStreamHandlerFactory.registerHandler("pop3", new org.mule.transport.soap.axis.transport.pop3.Handler());
        MuleUrlStreamHandlerFactory.registerHandler("smtp", new org.mule.transport.soap.axis.transport.smtp.Handler());
        MuleUrlStreamHandlerFactory.registerHandler("vm", new org.mule.transport.soap.axis.transport.vm.Handler());

        try
        {
            registerTypes((TypeMappingRegistryImpl) axis.getTypeMappingRegistry(), beanTypes);
        }
        catch (ClassNotFoundException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected void registerTransportTypes() throws ClassNotFoundException
    {
        // Register Transport handlers
        // By default these will all be handled by Mule, however some companies may
        // have their own they wish to use
        for (String protocol : getAxisTransportProtocols().keySet())
        {
            Object temp = getAxisTransportProtocols().get(protocol);
            Class<?> clazz;
            if (temp instanceof String)
            {
                clazz = ClassUtils.loadClass(temp.toString(), getClass());
            }
            else
            {
                clazz = (Class<?>) temp;
            }
            Call.setTransportForProtocol(protocol, clazz);
        }
    }

    protected SimpleProvider createAxisProvider(String config) throws InitialisationException
    {
        // Use our custom file provider that does not require services to be declared
        // in the WSDD. This only affects the
        // client side as the client will fallback to the FileProvider when invoking
        // a service.
        WSDDFileProvider fileProvider = new WSDDFileProvider(config);
        fileProvider.setSearchClasspath(true);
        /*
         * Wrap the FileProvider with a SimpleProvider so we can programmatically
         * configure the Axis server (you can only use wsdd descriptors with the
         * FileProvider)
         */
        return new MuleConfigProvider(fileProvider);
    }

    @Override
    public String getProtocol()
    {
        return AXIS;
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param flowConstruct the component for which the endpoint is being registered
     * @param endpoint  the endpoint being registered for the component
     * @return the key to store the newly created receiver against. In this case it
     *         is the component name, which is equivalent to the Axis service name.
     */
    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        if (endpoint.getEndpointURI().getPort() == -1)
        {
            return flowConstruct.getName();
        }
        else
        {
            return endpoint.getEndpointURI().getAddress() + "/" + flowConstruct.getName();
        }
    }

    protected void unregisterReceiverWithMuleService(MessageReceiver receiver, EndpointURI ep)
            throws MuleException
    {
        String endpointKey = getCounterEndpointKey(receiver.getEndpointURI());

        for (Iterator iterator = ((ServiceCompositeMessageSource) axisComponent.getMessageSource()).getEndpoints().iterator(); iterator.hasNext();)
        {
            ImmutableEndpoint endpoint = (ImmutableEndpoint) iterator.next();
            if (endpointKey.startsWith(endpoint.getEndpointURI().getAddress()))
            {
                logger.info("Unregistering Axis endpoint: " + endpointKey + " for service: "
                        + ((AxisMessageReceiver) receiver).getSoapService().getName());
            }
            try
            {
                endpoint.getConnector()
                        .unregisterListener(receiver.getEndpoint(), axisComponent);
            }
            catch (Exception e)
            {
                logger.error("Failed to unregister Axis endpoint: " + endpointKey + " for service: "
                        + receiver.getFlowConstruct().getName() + ". Error is: "
                        + e.getMessage(), e);
            }
        }
    }

    protected void registerReceiverWithMuleService(MessageReceiver receiver, EndpointURI ep)
            throws MuleException
    {
        // If this is the first receiver we need to create the Axis service
        // component this will be registered with Mule when the Connector starts
        // See if the axis descriptor has already been added. This allows
        // developers to override the default configuration, say to increase
        // the threadpool
        if (axisComponent == null)
        {
            axisComponent = getOrCreateAxisComponent();
        }
        else
        {
            // Lets unregister the 'template' instance, configure it and
            // then register again later
            muleContext.getRegistry().unregisterService(AXIS_SERVICE_PROPERTY + getName());
        }

        String serviceName = ((AxisMessageReceiver) receiver).getSoapService().getName();
        // No determine if the endpointUri requires a new connector to be
        // registed in the case of http we only need to register the new endpointUri
        // if the port is different If we're using VM or Jms we just use the resource
        // info directly without appending a service name
        String endpoint;
        String scheme = ep.getScheme().toLowerCase();
        if (scheme.equals("jms") || scheme.equals("vm") || scheme.equals("servlet"))
        {
            endpoint = ep.toString();
        }
        else
        {
            endpoint = receiver.getEndpointURI().getAddress() + "/" + serviceName;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Modified endpoint with " + scheme + " scheme to " + endpoint);
        }

        EndpointBuilder serviceEndpointbuilder = new EndpointURIEndpointBuilder(endpoint, muleContext);
        serviceEndpointbuilder.setExchangePattern(receiver.getEndpoint().getExchangePattern());
        serviceEndpointbuilder.setName(ep.getScheme() + ":" + serviceName);
        // Set the transformers on the endpoint too
        serviceEndpointbuilder.setTransformers(receiver.getEndpoint().getTransformers().isEmpty() ? null
                                                                                                  : receiver.getEndpoint().getTransformers());
        serviceEndpointbuilder.setResponseTransformers(receiver.getEndpoint().getResponseTransformers().isEmpty() ? null
                                                                                                                 : receiver.getEndpoint().getResponseTransformers());
        // set the filter on the axis endpoint on the real receiver endpoint
        serviceEndpointbuilder.addMessageProcessor(new MessageFilter(receiver.getEndpoint().getFilter()));
        // set the Security filter on the axis endpoint on the real receiver
        // endpoint
        SecurityFilter securityFilter = receiver.getEndpoint().getSecurityFilter();
        if (securityFilter != null)
        {
            serviceEndpointbuilder.addMessageProcessor(new SecurityFilterMessageProcessor(securityFilter));
        }

        // TODO Do we really need to modify the existing receiver endpoint? What happens if we don't security,
        // filters and transformers will get invoked twice?
        AbstractEndpointBuilder receiverEndpointBuilder = new EndpointURIEndpointBuilder(receiver.getEndpoint());
        // Remove the Axis filter now

        List<MessageProcessor> procs = new ArrayList<MessageProcessor>(receiverEndpointBuilder.getMessageProcessors());
        CollectionUtils.removeType(procs, MessageFilter.class);
        CollectionUtils.removeType(procs, SecurityFilterMessageProcessor.class);
        receiverEndpointBuilder.setMessageProcessors(procs);

        InboundEndpoint serviceEndpoint = muleContext.getEndpointFactory()
            .getInboundEndpoint(serviceEndpointbuilder);

        InboundEndpoint receiverEndpoint = muleContext.getEndpointFactory()
            .getInboundEndpoint(receiverEndpointBuilder);

        receiver.setEndpoint(receiverEndpoint);

        ((CompositeMessageSource) axisComponent.getMessageSource()).addSource(serviceEndpoint);
    }

    private String getCounterEndpointKey(EndpointURI endpointURI)
    {
        StringBuilder endpointKey = new StringBuilder(64);

        endpointKey.append(endpointURI.getScheme());
        endpointKey.append("://");
        endpointKey.append(endpointURI.getHost());
        if (endpointURI.getPort() > -1)
        {
            endpointKey.append(":");
            endpointKey.append(endpointURI.getPort());
        }
        return endpointKey.toString();
    }

    // This initialization could be performed in the initialize() method.  Putting it here essentially makes
    // it a lazy-create/lazy-init
    // Another option would be to put it in the default-axis-config.xml (MULE-2102) with lazy-init="true"
    // but that makes us depend on Spring.
    // Another consideration is how/when this implicit component gets disposed.
    protected Service getOrCreateAxisComponent() throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService(AXIS_SERVICE_PROPERTY + getName());

        if (service == null)
        {
            // TODO MULE-2228 Simplify this API
            service = new SedaService(muleContext);
            service.setName(AXIS_SERVICE_PROPERTY + getName());
            service.setModel(muleContext.getRegistry().lookupSystemModel());

            Map props = new HashMap();
            props.put(AXIS, axis);
            SingletonObjectFactory of = new SingletonObjectFactory(AxisServiceComponent.class, props);
            final DefaultJavaComponent component = new DefaultJavaComponent(of);
            component.setMuleContext(muleContext);
            service.setComponent(component);
        }
        return service;
    }

    /**
     * Template method to perform any work when starting the connectoe
     *
     * @throws org.mule.api.MuleException if the method fails
     */
    @Override
    protected void doStart() throws MuleException
    {
        axis.start();
    }

    /**
     * Template method to perform any work when stopping the connectoe
     *
     * @throws org.mule.api.MuleException if the method fails
     */
    @Override
    protected void doStop() throws MuleException
    {
        axis.stop();
        // Model model = muleContext.getRegistry().lookupModel();
        // model.unregisterComponent(model.getDescriptor(AXIS_SERVICE_COMPONENT_NAME));
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDispose()
    {
        // clear static references to classloaders to prevent loading classes from
        // closed jars and avoid memory leaks on application redeploy.
        try
        {
            Field field = AxisProperties.class.getDeclaredField("loaders");
            field.setAccessible(true);
            field.set(null, null);

            field = AxisProperties.class.getDeclaredField("nameDiscoverer");
            field.setAccessible(true);
            field.set(null, null);
        }
        catch (Exception e)
        {
            logger.error(
                "Error disposing AxisConnector, this may cause a memory leak. Error is: " + e.getMessage(), e);
        }

    }

    public String getServerConfig()
    {
        return serverConfig;
    }

    public void setServerConfig(String serverConfig)
    {
        this.serverConfig = serverConfig;
    }

    public List<?> getBeanTypes()
    {
        return beanTypes;
    }

    public void setBeanTypes(List<?> beanTypes)
    {
        this.beanTypes = beanTypes;
    }

    public String getClientConfig()
    {
        return clientConfig;
    }

    public void setClientConfig(String clientConfig)
    {
        this.clientConfig = clientConfig;
    }

    public AxisServer getAxis()
    {
        return axis;
    }

    public void setAxis(AxisServer axisServer)
    {
        this.axis = axisServer;
    }

    public SimpleProvider getServerProvider()
    {
        return serverProvider;
    }

    public void setServerProvider(SimpleProvider serverProvider)
    {
        this.serverProvider = serverProvider;
    }

    public SimpleProvider getClientProvider()
    {
        return clientProvider;
    }

    public void setClientProvider(SimpleProvider clientProvider)
    {
        this.clientProvider = clientProvider;
    }

    public Map<String, Class<?>> getAxisTransportProtocols()
    {
        return axisTransportProtocols;
    }

    public void setAxisTransportProtocols(Map<String, Class<?>> axisTransportProtocols)
    {
        this.axisTransportProtocols.putAll(axisTransportProtocols);
    }

    void addServletService(SOAPService service)
    {
        servletServices.add(service);
    }

    public List<String> getSupportedSchemes()
    {
        return supportedSchemes;
    }

    public void setSupportedSchemes(List<String> supportedSchemes)
    {
        this.supportedSchemes = supportedSchemes;
    }

    public boolean isDoAutoTypes()
    {
        return doAutoTypes;
    }

    public void setDoAutoTypes(boolean doAutoTypes)
    {
        this.doAutoTypes = doAutoTypes;
    }

    void registerTypes(TypeMappingRegistryImpl registry, List types) throws ClassNotFoundException
    {
        if (types != null)
        {
            Class clazz;
            for (Iterator iterator = types.iterator(); iterator.hasNext();)
            {
                clazz = ClassUtils.loadClass(iterator.next().toString(), getClass());
                String localName = Types.getLocalNameFromFullName(clazz.getName());
                QName xmlType = new QName(Namespaces.makeNamespace(clazz.getName()), localName);

                registry.getDefaultTypeMapping().register(clazz, xmlType,
                        new BeanSerializerFactory(clazz, xmlType), new BeanDeserializerFactory(clazz, xmlType));
            }
        }
    }

    public boolean isTreatMapAsNamedParams()
    {
        return treatMapAsNamedParams;
    }

    public void setTreatMapAsNamedParams(boolean treatMapAsNamedParams)
    {
        this.treatMapAsNamedParams = treatMapAsNamedParams;
    }

    public void onNotification(MuleContextNotification notification)
    {
        try
        {
            if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
            {
                // We need to register the Axis service component once the muleContext
                // starts because when the model starts listeners on components are started, thus
                // all listener need to be registered for this connector before the Axis service
                // component is registered.
                // The implication of this is that to add a new service and a
                // different http port the model needs to be restarted before the listener is available
                if (muleContext.getRegistry().lookupService(AXIS_SERVICE_PROPERTY + getName()) == null)
                {
                    // Descriptor might be null if no inbound endpoints have been
                    // register for the Axis connector
                    if (axisComponent == null)
                    {
                        axisComponent = getOrCreateAxisComponent();
                    }
                    muleContext.getRegistry().registerService(axisComponent);

                    // We have to perform a small hack here to rewrite servlet://
                    // endpoints with the
                    // real http:// address
                    for (SOAPService service : servletServices)
                    {
                        ServletConnector servletConnector = (ServletConnector) new TransportFactory(muleContext).getConnectorByProtocol("servlet");
                        String url = servletConnector.getServletUrl();
                        if (url != null)
                        {
                            service.getServiceDescription().setEndpointURL(url + "/" + service.getName());
                        }
                        else
                        {
                            logger.error("The servletUrl property on the ServletConntector has not been set this means that wsdl generation for service '"
                                    + service.getName() + "' may be incorrect");
                        }
                    }
                    servletServices.clear();
                }
            }
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}
