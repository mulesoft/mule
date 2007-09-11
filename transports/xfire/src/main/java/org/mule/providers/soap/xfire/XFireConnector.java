/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.ManagerNotificationListener;
import org.mule.impl.internal.notifications.NotificationException;
import org.mule.impl.model.ModelHelper;
import org.mule.providers.AbstractConnector;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.soap.MethodFixInterceptor;
import org.mule.providers.soap.xfire.i18n.XFireMessages;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;
import org.mule.util.object.SingletonObjectFactory;

import java.util.List;

import org.codehaus.xfire.DefaultXFire;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.aegis.AegisBindingProvider;
import org.codehaus.xfire.aegis.type.TypeMappingRegistry;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.annotations.WebAnnotations;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceFactory;
import org.codehaus.xfire.service.binding.BindingProvider;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.soap.SoapConstants;
import org.codehaus.xfire.wsdl11.builder.WSDLBuilderFactory;

/**
 * Configures Xfire to provide STaX-based Web Servies support to Mule.
 */
public class XFireConnector extends AbstractConnector
    implements ManagerNotificationListener
{
    public static final String XFIRE_SERVICE_COMPONENT_NAME = "_xfireServiceComponent";
    public static final String DEFAULT_MULE_NAMESPACE_URI = "http://www.muleumo.org";
    public static final String XFIRE_PROPERTY = "xfire";
    public static final String XFIRE_TRANSPORT = "transportClass";

    public static final String CLASSNAME_ANNOTATIONS = "org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations";
    private static final String DEFAULT_BINDING_PROVIDER_CLASS = "org.codehaus.xfire.aegis.AegisBindingProvider";
    private static final String DEFAULT_TYPE_MAPPING_muleRegistry_CLASS = "org.codehaus.xfire.aegis.type.DefaultTypeMappingRegistry";

    protected MuleDescriptor xfireDescriptor;

    private XFire xfire;

    private ServiceFactory serviceFactory;

    private boolean enableJSR181Annotations = false;

    private List clientServices = null;
    private List clientInHandlers = null;
    private List clientOutHandlers = null;
    private String clientTransport = null;

    private String bindingProvider = null;
    private String typeMappingRegistry = null;
    private String serviceTransport = null;
    private List serverInHandlers = null;
    private List serverOutHandlers = null;

    public XFireConnector()
    {
        super();
        this.registerProtocols();
        // TODO RM: we should ditch the whole connector-knows-if-to-send-notifications thing
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
        return "xfire";
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

        if (xfire == null)
        {
            xfire = new DefaultXFire();
        }

        if (clientServices != null)
        {
            ObjectServiceFactory factory = new ObjectServiceFactory();
            configureBindingProvider(factory);

            for (int i = 0; i < clientServices.size(); i++)
            {
                try
                {
                    Class clazz = ClassUtils.loadClass(clientServices.get(i).toString(), this.getClass());
                    Service service = factory.create(clazz);
                    xfire.getServiceRegistry().register(service);
                }
                catch (ClassNotFoundException e)
                {
                    throw new InitialisationException(
                        XFireMessages.couldNotInitAnnotationProcessor(clientServices.get(i)), e, this);
                }
            }
        }

        if (serviceFactory == null)
        {
            if (enableJSR181Annotations)
            {
                // are we running under Java 5 (at least)?
                if (!SystemUtils.isJavaVersionAtLeast(150))
                {
                    throw new InitialisationException(
                        XFireMessages.annotationsRequireJava5(), this);
                }
                try
                {
                    WebAnnotations wa = (WebAnnotations)ClassUtils.instanciateClass(
                        CLASSNAME_ANNOTATIONS, null, this.getClass());
                    serviceFactory = new AnnotationServiceFactory(wa, xfire.getTransportManager());
                    configureBindingProvider((ObjectServiceFactory)serviceFactory);
                }
                catch (Exception ex)
                {
                    throw new InitialisationException(
                        XFireMessages.couldNotInitAnnotationProcessor(CLASSNAME_ANNOTATIONS), ex, this);
                }
            }
            else
            {
                serviceFactory = new MuleObjectServiceFactory(xfire.getTransportManager());
                configureBindingProvider((ObjectServiceFactory)serviceFactory);
            }
        }

        if (serviceFactory instanceof ObjectServiceFactory)
        {
            ObjectServiceFactory osf = (ObjectServiceFactory)serviceFactory;
            if (osf.getTransportManager() == null)
            {
                osf.setTransportManager(xfire.getTransportManager());
            }

        }
    }

    protected void configureBindingProvider(ObjectServiceFactory factory) throws InitialisationException
    {
        if (StringUtils.isBlank(bindingProvider))
        {
            bindingProvider = DEFAULT_BINDING_PROVIDER_CLASS;
        }

        if (StringUtils.isBlank(typeMappingRegistry))
        {
            typeMappingRegistry = DEFAULT_TYPE_MAPPING_muleRegistry_CLASS;
        }

        try
        {
            Class clazz = ClassUtils.loadClass(bindingProvider, this.getClass());
            BindingProvider provider = (BindingProvider)ClassUtils.instanciateClass(clazz, new Object[] {} );

            // Create the argument of TypeMappingRegistry ONLY if the binding 
            // provider is aegis and the type mapping registry is not the default
            if (bindingProvider.equals(DEFAULT_BINDING_PROVIDER_CLASS) && !typeMappingRegistry.equals(DEFAULT_TYPE_MAPPING_muleRegistry_CLASS))
            {
                Class registryClazz = ClassUtils.loadClass(typeMappingRegistry, this.getClass());

                // No constructor arguments for the mapping registry
                //
                // Note that if we had to create the DefaultTypeMappingRegistry here
                // we would need to pass in a boolean argument of true to the
                // constructor. Currently, it appears that all other registries
                // can be created with zero argument constructors
                TypeMappingRegistry registry = (TypeMappingRegistry)ClassUtils.instanciateClass(registryClazz, new Object[] { } );
                ((AegisBindingProvider)provider).setTypeMappingRegistry(registry);
            }

            factory.setBindingProvider(provider);

            String wsdlBuilderFactoryClass = null;

            // Special handling for MessageBindingProvider
            if (bindingProvider.equals("org.codehaus.xfire.service.binding.MessageBindingProvider"))
            {
                factory.setStyle(SoapConstants.STYLE_MESSAGE);
            }

            // See MULE-1871
//            // Special handling for XmlBeansBindingProvider
//            if (bindingProvider.equals("org.codehaus.xfire.service.binding.MessageBindingProvider"))
//            {
//                factory.setStyle(SoapConstants.STYLE_DOCUMENT);
//                wsdlBuilderFactoryClass = "org.codehaus.xfire.xmlbeans.XmlBeansWSDLBuilderFactory";
//            }

            // If required, create the WSDL builder factory (only XML beans needs
            // this)
            if (wsdlBuilderFactoryClass != null)
            {
                Class wsdlBuilderFactoryClazz = ClassUtils.loadClass(wsdlBuilderFactoryClass, this.getClass());
                WSDLBuilderFactory wsdlBuilderFactory = (WSDLBuilderFactory)ClassUtils.instanciateClass(wsdlBuilderFactoryClazz, new Object[] { } );
                factory.setWsdlBuilderFactory(wsdlBuilderFactory);
            }
        }
        catch (Exception ex)
        {
            throw new InitialisationException(
                XFireMessages.unableToInitBindingProvider(bindingProvider), ex, this);
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

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public XFire getXfire()
    {
        return xfire;
    }

    public void setXfire(XFire xfire)
    {
        this.xfire = xfire;
    }

    protected void registerReceiverWithMuleService(UMOMessageReceiver receiver, UMOEndpointURI ep)
        throws UMOException
    {
        // If this is the first receiver we need to create the Axis service
        // component
        // this will be registered with Mule when the Connector starts
        if (xfireDescriptor == null)
        {
            // See if the xfire descriptor has already been added. This allows
            // developers to override the default configuration, say to increase
            // the threadpool
            xfireDescriptor = (MuleDescriptor)managementContext.getRegistry().lookupService(
                XFIRE_SERVICE_COMPONENT_NAME + getName());
            if (xfireDescriptor == null)
            {
                xfireDescriptor = createxfireDescriptor();
            }
            else
            {
                // Lets unregister the 'template' instance, configure it and
                // then register
                // again later
                managementContext.getRegistry().lookupModel(ModelHelper.SYSTEM_MODEL).unregisterComponent(xfireDescriptor);
            }
            // if the axis server hasn't been set, set it now. The Axis server
            // may be set externally
            if (xfireDescriptor.getProperties().get(XFIRE_PROPERTY) == null)
            {
                xfireDescriptor.getProperties().put(XFIRE_PROPERTY, xfire);
            }
            if (serviceTransport != null
                && xfireDescriptor.getProperties().get(XFIRE_TRANSPORT) == null)
            {
                xfireDescriptor.getProperties().put(XFIRE_TRANSPORT, serviceTransport);
            }
        }
        String serviceName = receiver.getComponent().getDescriptor().getName();

        // No determine if the endpointUri requires a new connector to be
        // registed in the case of http we only need to register the new
        // endpointUri if the port is different
        String endpoint = receiver.getEndpointURI().getAddress();
        String scheme = ep.getScheme().toLowerCase();


        boolean sync = receiver.getEndpoint().isSynchronous();

        // If we are using sockets then we need to set the endpoint name appropiately
        // and if using http/https
        // we need to default to POST and set the Content-Type
        if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ssl")
            || scheme.equals("tcp") || scheme.equals("servlet"))
        {
            endpoint += "/" + serviceName;
            receiver.getEndpoint().getProperties().put(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
            receiver.getEndpoint().getProperties().put(HttpConstants.HEADER_CONTENT_TYPE,
                "text/xml");

            // Default to using synchronous for socket based protocols unless the
            // synchronous property has been set explicitly
            if (!receiver.getEndpoint().isSynchronousSet())
            {
                sync = true;
            }
        }
       

        UMOEndpoint serviceEndpoint = new MuleEndpoint(endpoint, true);
        serviceEndpoint.setSynchronous(sync);
        serviceEndpoint.setName(ep.getScheme() + ":" + serviceName);

        // Set the transformers on the endpoint too
        serviceEndpoint.setTransformer(receiver.getEndpoint().getTransformer());
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setTransformer(null);

        serviceEndpoint.setResponseTransformer(receiver.getEndpoint().getResponseTransformer());
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setResponseTransformer(null);

        // set the filter on the axis endpoint on the real receiver endpoint
        serviceEndpoint.setFilter(receiver.getEndpoint().getFilter());
        // Remove the Axis filter now
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setFilter(null);

        // set the Security filter on the axis endpoint on the real receiver
        // endpoint
        serviceEndpoint.setSecurityFilter(receiver.getEndpoint().getSecurityFilter());
        // Remove the Axis Receiver Security filter now
        // TODO DF: MULE-2291 Resolve pending endpoint mutability issues
        ((MuleEndpoint) receiver.getEndpoint()).setSecurityFilter(null);
        xfireDescriptor.getInboundRouter().addEndpoint(serviceEndpoint);
    }

    protected MuleDescriptor createxfireDescriptor()
    {
        MuleDescriptor xfireDescriptor = (MuleDescriptor)managementContext.getRegistry().lookupService(XFIRE_SERVICE_COMPONENT_NAME + getName());
        if (xfireDescriptor == null)
        {
            xfireDescriptor = new MuleDescriptor(XFIRE_SERVICE_COMPONENT_NAME + getName());
            xfireDescriptor.setServiceFactory(new SingletonObjectFactory(new XFireServiceComponent()));
        }
        return xfireDescriptor;
    }

    public ServiceFactory getServiceFactory()
    {
        return serviceFactory;
    }

    public void setServiceFactory(ServiceFactory serviceFactory)
    {
        this.serviceFactory = serviceFactory;
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param component the component for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the component
     * @return the key to store the newly created receiver against. In this case it
     *         is the component name, which is equivilent to the Axis service name.
     */
    protected Object getReceiverKey(UMOComponent component, UMOImmutableEndpoint endpoint)
    {
        if (endpoint.getEndpointURI().getPort() == -1)
        {
            return component.getDescriptor().getName();
        }
        else
        {
            return endpoint.getEndpointURI().getAddress() + "/"
                   + component.getDescriptor().getName();
        }
    }

    public boolean isEnableJSR181Annotations()
    {
        return enableJSR181Annotations;
    }

    public void setEnableJSR181Annotations(boolean enableJSR181Annotations)
    {
        this.enableJSR181Annotations = enableJSR181Annotations;
    }

    public List getClientServices()
    {
        return clientServices;
    }

    public void setClientServices(List clientServices)
    {
        this.clientServices = clientServices;
    }

    public List getClientInHandlers()
    {
        return clientInHandlers;
    }

    public void setClientInHandlers(List handlers)
    {
        clientInHandlers = handlers;
    }

    public List getClientOutHandlers()
    {
        return clientOutHandlers;
    }

    public void setClientOutHandlers(List handlers)
    {
        clientOutHandlers = handlers;
    }

    public String getClientTransport()
    {
        return clientTransport;
    }

    public void setClientTransport(String transportClass)
    {
        clientTransport = transportClass;
    }

    public String getServiceTransport()
    {
        return serviceTransport;
    }

    public void setServiceTransport(String transportClass)
    {
        serviceTransport = transportClass;
    }

    public String getBindingProvider()
    {
        return bindingProvider;
    }

    public void setBindingProvider(String bindingProvider)
    {
        this.bindingProvider = bindingProvider;
    }

    public String getTypeMappingRegistry()
    {
        return typeMappingRegistry;
    }

    public void setTypeMappingRegistry(String typeMappingRegistry)
    {
        this.typeMappingRegistry = typeMappingRegistry;
    }

    public void onNotification(UMOServerNotification event)
    {
        if (event.getAction() == ManagerNotification.MANAGER_STARTED_MODELS)
        {
            // We need to register the xfire service component once the model
            // starts because
            // when the model starts listeners on components are started, thus
            // all listener
            // need to be registered for this connector before the xfire service
            // component is registered. The implication of this is that to add a
            // new service and a
            // different http port the model needs to be restarted before the
            // listener is available
            if (!managementContext.getRegistry().lookupModel(ModelHelper.SYSTEM_MODEL).isComponentRegistered(
                XFIRE_SERVICE_COMPONENT_NAME + getName()))
            {
                try
                {
                    // Descriptor might be null if no inbound endpoints have
                    // been register for the xfire connector
                    if (xfireDescriptor == null)
                    {
                        xfireDescriptor = createxfireDescriptor();
                    }
                    xfireDescriptor.addInterceptor(new MethodFixInterceptor());

                    if (xfireDescriptor.getProperties().get("xfire") == null)
                    {
                        xfireDescriptor.getProperties().put("xfire", xfire);
                    }
                    xfireDescriptor.setModelName(MuleProperties.OBJECT_SYSTEM_MODEL);
                    managementContext.getRegistry().registerService(xfireDescriptor);
                }
                catch (UMOException e)
                {
                    handleException(e);
                }
            }
        }
    }

    public List getServerInHandlers()
    {
        return serverInHandlers;
    }

    public void setServerInHandlers(List serverInHandlers)
    {
        this.serverInHandlers = serverInHandlers;
    }

    public List getServerOutHandlers()
    {
        return serverOutHandlers;
    }

    public void setServerOutHandlers(List serverOutHandlers)
    {
        this.serverOutHandlers = serverOutHandlers;
    }
}
