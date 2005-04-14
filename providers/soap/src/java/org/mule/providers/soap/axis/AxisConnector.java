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
package org.mule.providers.soap.axis;

import org.apache.axis.AxisFault;
import org.apache.axis.AxisProperties;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.deployment.wsdd.WSDDConstants;
import org.apache.axis.deployment.wsdd.WSDDProvider;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.encoding.TypeMappingRegistryImpl;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.axis.server.AxisServer;
import org.apache.axis.wsdl.fromJava.Namespaces;
import org.apache.axis.wsdl.fromJava.Types;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.events.ModelEvent;
import org.mule.impl.internal.events.ModelEventListener;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.soap.ServiceProxy;
import org.mule.providers.soap.axis.extensions.MuleConfigProvider;
import org.mule.providers.soap.axis.extensions.MuleProvider;
import org.mule.providers.soap.axis.extensions.WSDDJavaMuleProvider;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerEvent;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.ClassHelper;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>AxisConnector</code> is used to maintain one or more Services for
 * Axis server instance.
 *
 * Some of the Axis specific service initialisation code was adapted from
 * the Ivory project (http://ivory.codehaus.org). Thanks :)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisConnector extends AbstractServiceEnabledConnector implements ModelEventListener
{
    public static final QName QNAME_MULERPC_PROVIDER = new QName(WSDDConstants.URI_WSDD_JAVA, "Mule");
    public static final QName QNAME_MULE_TYPE_MAPPINGS = new QName("http://www.muleumo.org/ws/mappings", "Mule");


    public static final String DEFAULT_MULE_AXIS_SERVER_CONFIG = "mule-axis-server-config.wsdd";
    public static final String DEFAULT_MULE_AXIS_CLIENT_CONFIG = "mule-axis-client-config.wsdd";
    public static final String AXIS_SERVICE_COMPONENT_NAME = "_axisServiceComponent";

    public static final String SERVICE_PROPERTY_COMPONENT_NAME = "componentName";
    public static final String SERVICE_PROPERTY_SERVCE_PATH = "servicePath";
    private String serverConfig;
    private AxisServer axisServer;
    private SimpleProvider serverProvider;
    //Client configuration currently not used but the endpoint should
    //probably support configuration of the client too
    private String clientConfig;
    private SimpleProvider clientProvider;

    private List beanTypes;
    private MuleDescriptor axisDescriptor;

    public AxisConnector()
    {
        super();
    }

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        MuleManager.getInstance().registerListener(this);
        
        if(serverConfig==null) serverConfig = DEFAULT_MULE_AXIS_SERVER_CONFIG;
        if(clientConfig==null) clientConfig = DEFAULT_MULE_AXIS_CLIENT_CONFIG;
        serverProvider = createAxisProvider(serverConfig);
        clientProvider = createAxisProvider(clientConfig);

        // Create the AxisServer
        axisServer = new AxisServer( serverProvider );

        //Register the Mule service serverProvider
        WSDDProvider.registerProvider(QNAME_MULERPC_PROVIDER,
            new WSDDJavaMuleProvider(this));
    }

    protected SimpleProvider createAxisProvider(String config) throws InitialisationException
    {
        InputStream is = null;
        File f = new File(config);
        if(f.exists()) {
            try
            {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e)
            {
                //ignore
            }
        } else {
            is = ClassHelper.getResourceAsStream(config, getClass());
        }
        FileProvider fileProvider = new FileProvider(config);
        if(is!=null) {
            fileProvider.setInputStream(is);
        } else {
            throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Axis Configuration: " + config), this);
        }
        /* Wrap the FileProvider with a SimpleProvider so we can prgrammatically
         * configure the Axis server (you can only use wsdd descriptors with the
         * FileProvider)
         */
         return new MuleConfigProvider( fileProvider );
    }

    public String getProtocol()
    {
        return "axis";
    }

    public AxisMessageReceiver getReceiver(String name) {
        return (AxisMessageReceiver)receivers.get(name);
    }

    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        return component.getDescriptor().getName();
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        //this is always initialisaed as synchronous as ws invocations should
        //always execute in a single thread unless the endpont has explicitly
        //been set to run asynchronously
        if(endpoint instanceof ImmutableMuleEndpoint  &&
                !((ImmutableMuleEndpoint)endpoint).isSynchronousExplicitlySet()) {
            if(!endpoint.isSynchronous()) {
                logger.debug("overriding endpoint synchronicity and setting it to true. Web service requests are executed in a single thread");
                endpoint.setSynchronous(true);
            }
        }

        UMOMessageReceiver receiver = super.createReceiver(component, endpoint);
        registerAxisComponent(receiver);
        return receiver;
    }

    protected void registerAxisComponent(UMOMessageReceiver receiver) throws AxisFault, UMOException, ClassNotFoundException, URISyntaxException
    {
        SOAPService service = new SOAPService( new MuleProvider(this));

        service.setEngine( axisServer );

        UMOEndpointURI uri = receiver.getEndpoint().getEndpointURI();
        String serviceName = receiver.getComponent().getDescriptor().getName();

        String servicePath = uri.getPath();
        service.setOption(serviceName, receiver);
        service.setOption(SERVICE_PROPERTY_SERVCE_PATH, servicePath);
        service.setOption(SERVICE_PROPERTY_COMPONENT_NAME, serviceName);
        service.setName(serviceName);

        //Add any custom options from the Descriptor config
        Map options = (Map)receiver.getComponent().getDescriptor().getProperties().get("axisOptions");
        if(options!=null) {
            Map.Entry entry;
            for (Iterator iterator = options.entrySet().iterator(); iterator.hasNext();)
            {
                entry =  (Map.Entry)iterator.next();
                service.setOption(entry.getKey().toString(), entry.getValue());
                logger.debug("Adding Axis option: " + entry);
            }
        }
        //set method names
        Class[] interfaces = ServiceProxy.getInterfacesForComponent(receiver.getComponent());
        //You must supply a class name if you want to restrict methods
        //or specify the 'allowedMethods' property in the axisOptions property
        String methodNames = "*";

        String[] methods = ServiceProxy.getMethodNames(interfaces);
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < methods.length; i++)
        {
            buf.append(methods[i]).append(",");
        }
        String className = interfaces[0].getName();
        methodNames = buf.toString();
        methodNames = methodNames.substring(0, methodNames.length() -1);

        // The namespace of the service.
        String namespace =  Namespaces.makeNamespace( className );

        /* Now we set up the various options for the SOAPService. We set:
         *
         * RPCProvider.OPTION_WSDL_SERVICEPORT
         * In essense, this is our service name
         *
         * RPCProvider.OPTION_CLASSNAME
         * This tells the serverProvider (whether it be an AvalonProvider or just
         * JavaProvider) what class to load via "makeNewServiceObject".
         *
         * RPCProvider.OPTION_SCOPE
         * How long the object loaded via "makeNewServiceObject" will persist -
         * either request, session, or application.  We use the default for now.
         *
         * RPCProvider.OPTION_WSDL_TARGETNAMESPACE
         * A namespace created from the package name of the service.
         *
         * RPCProvider.OPTION_ALLOWEDMETHODS
         * What methods the service can execute on our class.
         *
         * We don't set:
         * RPCProvider.OPTION_WSDL_PORTTYPE
         * RPCProvider.OPTION_WSDL_SERVICEELEMENT
         */
        setOptionIfNotset(service,  RPCProvider.OPTION_WSDL_SERVICEPORT, serviceName);
        setOptionIfNotset(service, RPCProvider.OPTION_CLASSNAME, className );
        setOptionIfNotset(service, RPCProvider.OPTION_SCOPE, "Request");
        setOptionIfNotset(service, RPCProvider.OPTION_WSDL_TARGETNAMESPACE, namespace  );

        // Set the allowed methods, allow all if there are none specified.
        if ( methodNames == null)
        {
            setOptionIfNotset(service, RPCProvider.OPTION_ALLOWEDMETHODS, "*" );
        }
        else
        {
            setOptionIfNotset(service, RPCProvider.OPTION_ALLOWEDMETHODS, methodNames );
        }

        /* Create a service description.  This tells Axis that this
         * service exists and also what it can execute on this service.  It is
         * created with all the options we set above.
         */
        ServiceDesc sd = service.getInitializedServiceDesc(null);
        sd.setName(serviceName);
        sd.setEndpointURL(uri.getAddress() + "/" + serviceName);

        String style = (String)receiver.getComponent().getDescriptor().getProperties().get("style");
        String use = (String)receiver.getComponent().getDescriptor().getProperties().get("use");
        String doc = (String)receiver.getComponent().getDescriptor().getProperties().get("documentation");

        //Note that Axis has specific rules to how these two variables are
        //combined.  This is handled for us
        //Set style: RPC/wrapped/Doc/Message
        if(style!=null) sd.setStyle(Style.getStyle(style));
        //Set use: Endcoded/Literal
        if(use!=null) sd.setUse(Use.getUse(use));
        sd.setDocumentation(doc);

        // Tell Axis to try and be intelligent about serialization.
        TypeMappingRegistryImpl registry = (TypeMappingRegistryImpl)service.getTypeMappingRegistry();
        //TypeMappingImpl tm = (TypeMappingImpl) registry.();
        //Handle complex bean type automatically
        //registry.setDoAutoTypes( true );
        //Axis 1.2 fix to handle autotypes properly
        AxisProperties.setProperty("axis.doAutoTypes", "true");

        //Load any explicitly defined bean types
        List types = (List)receiver.getComponent().getDescriptor().getProperties().get("beanTypes");
        registerTypes(registry, types);
        registerTypes(registry, beanTypes);
        service.setName(serviceName);

        // Tell the axis configuration about our new service.
        this.serverProvider.deployService( serviceName, service );

        //Add initialisation callback for the Axis service
        MuleDescriptor desc =(MuleDescriptor)receiver.getComponent().getDescriptor();
        desc.addInitialisationCallback(new AxisInitialisationCallback(service));

        registerReceiverWithMuleService(receiver, uri);
    }

    protected void registerReceiverWithMuleService(UMOMessageReceiver receiver, UMOEndpointURI ep) throws UMOException
    {
        //If this is the first receiver we need to create the Axis service component
        //this will be registered with Mule when the Connector starts
        if(axisDescriptor==null) {
            //See if the axis descriptor has already been added.  This allows
            //developers to override the default configuration, say to increase
            //the threadpool
            axisDescriptor = (MuleDescriptor)MuleManager.getInstance().getModel().getDescriptor(AXIS_SERVICE_COMPONENT_NAME);
            if(axisDescriptor==null) {
                axisDescriptor = new MuleDescriptor(AXIS_SERVICE_COMPONENT_NAME);
                axisDescriptor.setImplementation(AxisServiceComponent.class.getName());
            } else {
                //Lets unregister the 'template' instance, configure it and then register
                //again later
                MuleManager.getInstance().getModel().unregisterComponent(axisDescriptor);
            }
            //if the axis server hasn't been set, set it now
            if(axisDescriptor.getProperties().get("axisServer") == null) {
                axisDescriptor.getProperties().put("axisServer", axisServer);
            }
            axisDescriptor.setContainerManaged(false);
        }
        //No determine if the endpointUri requires a new connector to be registed
        // in the case of http we only need to register the new endpointUri if the
        //port is different
        String endpoint = receiver.getEndpointURI().getAddress();
        boolean startsWith = false;
        String scheme = ep.getScheme().toLowerCase();
        if(scheme.equals("http") || scheme.equals("tcp")) {
            endpoint = scheme + "://" + ep.getHost() + ":" + ep.getPort();
            startsWith = true;
            //if we are using a socket based endpointUri make sure it is running
            //synchronously by default
            String sync = "synchronous=" + receiver.getEndpoint().isSynchronous();
            if(endpoint.indexOf("?") > -1) {
                endpoint += "&" + sync;
            } else {
                endpoint += "?" + sync;
            }
        }
        boolean registered = false;
        for (Iterator iterator = axisDescriptor.getInboundRouter().getEndpoints().iterator(); iterator.hasNext();)
        {
            UMOEndpoint umoEndpoint = (UMOEndpoint)iterator.next();
            if((startsWith && endpoint.startsWith(umoEndpoint.getEndpointURI().getAddress())) ||
                    (!startsWith && endpoint.startsWith(umoEndpoint.getEndpointURI().getAddress()))) {
                registered = true;
                break;
            }
        }
        if(!registered) {
            UMOEndpoint serviceEndpoint = new MuleEndpoint(endpoint, true);
            serviceEndpoint.setName(ep.getScheme() + ":" + receiver.getComponent().getDescriptor().getName());
            //set the filter on the axis endpoint on the real receiver endpoint
            serviceEndpoint.setFilter(receiver.getEndpoint().getFilter());
            axisDescriptor.getInboundRouter().addEndpoint(serviceEndpoint);
        }
    }

    protected void setOptionIfNotset(SOAPService service, String option, Object value) {
        Object val = service.getOption(option);
        if(val==null) service.setOption(option, value);
    }

    protected void registerTypes(TypeMappingRegistryImpl registry, List types) throws ClassNotFoundException
    {
        if(types!=null) {
            Class clazz;
            for (Iterator iterator = types.iterator(); iterator.hasNext();)
            {
                clazz = ClassHelper.loadClass(iterator.next().toString(), getClass());
                QName xmlType = new QName(
                Namespaces.makeNamespace( clazz.getName() ),
                Types.getLocalNameFromFullName( clazz.getName() ) );

                registry.getDefaultTypeMapping().register(clazz,
                      xmlType,
                      new BeanSerializerFactory(clazz, xmlType),
                      new BeanDeserializerFactory(clazz, xmlType) );
            }
        }
    }
    /**
     * Template method to perform any work when starting the connectoe
     *
     * @throws org.mule.umo.UMOException if the method fails
     */
    protected void startConnector() throws UMOException
    {
        axisServer.start();
    }

    /**
     * Template method to perform any work when stopping the connectoe
     *
     * @throws org.mule.umo.UMOException if the method fails
     */
    protected void stopConnector() throws UMOException
    {
        axisServer.stop();
//        UMOModel model = MuleManager.getInstance().getModel();
//        model.unregisterComponent(model.getDescriptor(AXIS_SERVICE_COMPONENT_NAME));
    }


    public String getServerConfig()
    {
        return serverConfig;
    }

    public void setServerConfig(String serverConfig)
    {
        this.serverConfig = serverConfig;
    }

    public List getBeanTypes()
    {
        return beanTypes;
    }

    public void setBeanTypes(List beanTypes)
    {
        this.beanTypes = beanTypes;
    }

    public void onEvent(UMOServerEvent event)
    {
        if(event.getAction()==ModelEvent.MODEL_STARTED) {
            //We need to rgister the Axis service component once the model starts because
            //when the model starts listeners on components are started, thus all listener
            //need to be registered for this connector before the Axis service component
            //is registered.
            //The implication of this is that to add a new serive and a different http port the
            //model needs to be restarted before the listener is available
            if(!MuleManager.getInstance().getModel().isComponentRegistered(AXIS_SERVICE_COMPONENT_NAME)) {
                try
                {
                    MuleManager.getInstance().getModel().registerComponent(axisDescriptor);
                } catch (UMOException e)
                {
                    handleException(e);
                }
            }
        }
    }

    public String getClientConfig()
    {
        return clientConfig;
    }

    public void setClientConfig(String clientConfig)
    {
        this.clientConfig = clientConfig;
    }

    public AxisServer getAxisServer()
    {
        return axisServer;
    }

    public void setAxisServer(AxisServer axisServer)
    {
        this.axisServer = axisServer;
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
}
