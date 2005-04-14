/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.config.builders;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.*;
import org.mule.config.converters.*;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.config.pool.CommonsPoolFactory;
import org.mule.impl.*;
import org.mule.impl.container.MuleContainerContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.model.DynamicEntryPointResolver;
import org.mule.providers.AbstractConnector;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.routing.response.ResponseMessageRouter;
import org.mule.transaction.constraints.BatchConstraint;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.*;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassHelper;
import org.mule.util.PropertiesHelper;
import org.mule.util.Utility;
import org.mule.util.queue.PersistenceStrategy;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.beans.ExceptionListener;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * <code>MuleXmlConfigurationBuilder</code> is a configuration parser that builds a
 * MuleManager instance based on a mule xml configration file defined in the
 * mule-configuration.dtd.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleXmlConfigurationBuilder implements ConfigurationBuilder
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleXmlConfigurationBuilder.class);

    public static final String DEFAULT_ENTRY_POINT_RESOLVER = DynamicEntryPointResolver.class.getName();
    public static final String DEFAULT_LIFECYCLE_ADAPTER = DefaultLifecycleAdapter.class.getName();
    public static final String DEFAULT_COMPONENT_FACTORY = MuleComponentFactory.class.getName();
    public static final String DEFAULT_CONTAINER_CONTEXT = MuleContainerContext.class.getName();
    public static final String DEFAULT_ENDPOINT = MuleEndpoint.class.getName();
    public static final String DEFAULT_TRANSACTION_CONFIG = MuleTransactionConfig.class.getName();
    public static final String DEFAULT_DESCRIPTOR = MuleDescriptor.class.getName();
    public static final String DEFAULT_SECURITY_MANAGER = MuleSecurityManager.class.getName();
    public static final String DEFAULT_OUTBOUND_MESSAGE_ROUTER = OutboundMessageRouter.class.getName();
    public static final String DEFAULT_INBOUND_MESSAGE_ROUTER = InboundMessageRouter.class.getName();
    public static final String DEFAULT_RESPONSE_MESSAGE_ROUTER = ResponseMessageRouter.class.getName();
    public static final String DEFAULT_CATCH_ALL_STRATEGY = LoggingCatchAllStrategy.class.getName();
    public static final String DEFAULT_POOL_FACTORY = CommonsPoolFactory.class.getName();
    public static final String DEFAULT_MODEL = MuleModel.class.getName();
    public static final String THREADING_PROFILE = ThreadingProfile.class.getName();
    public static final String POOLING_PROFILE = PoolingProfile.class.getName();
    public static final String QUEUE_PROFILE = QueueProfile.class.getName();

    public static final String PERSISTENCE_STRATEGY_INTERFACE = PersistenceStrategy.class.getName();
    public static final String INBOUND_MESSAGE_ROUTER_INTERFACE = UMOInboundMessageRouter.class.getName();
    public static final String RESPONSE_MESSAGE_ROUTER_INTERFACE = UMOResponseMessageRouter.class.getName();
    public static final String OUTBOUND_MESSAGE_ROUTER_INTERFACE = UMOOutboundMessageRouter.class.getName();
    public static final String TRANSFORMER_INTERFACE = UMOTransformer.class.getName();
    public static final String TRANSACTION_MANAGER_FACTORY_INTERFACE = UMOTransactionManagerFactory.class.getName();
    public static final String SECURITY_PROVIDER_INTERFACE = UMOSecurityProvider.class.getName();
    public static final String ENCRYPTION_STRATEGY_INTERFACE = UMOEncryptionStrategy.class.getName();
    public static final String ENDPOINT_SECURITY_FILTER_INTERFACE = UMOEndpointSecurityFilter.class.getName();
    public static final String AGENT_INTERFACE = UMOAgent.class.getName();
    public static final String TRANSACTION_FACTORY_INTERFACE = UMOTransactionFactory.class.getName();
    public static final String TRANSACTION_CONSTRAINT_INTERFACE = BatchConstraint.class.getName();
    public static final String CONNECTOR_INTERFACE = UMOConnector.class.getName();
    public static final String INTERCEPTOR_INTERFACE = UMOInterceptor.class.getName();
    public static final String ROUTER_INTERFACE = UMOOutboundRouter.class.getName();
    public static final String FILTER_INTERFACE = UMOFilter.class.getName();
    public static final String EXCEPTION_STRATEGY_INTERFACE = ExceptionListener.class.getName();

    protected UMOManager manager;
    protected Digester digester;
    private List containerReferences = new ArrayList();
    private List transformerReferences = new ArrayList();
    private List endpointReferences = new ArrayList();

    public MuleXmlConfigurationBuilder() throws ConfigurationException {
        ConvertUtils.register(new EndpointConverter(), UMOEndpoint.class);
        ConvertUtils.register(new TransformerConverter(), UMOTransformer.class);
        ConvertUtils.register(new ConnectorConverter(), UMOConnector.class);
        ConvertUtils.register(new TransactionFactoryConverter(), UMOTransactionFactory.class);
        ConvertUtils.register(new EndpointURIConverter(), UMOEndpointURI.class);

        //This is a hack to stop Digester spitting out unnecessary warnings where there is
        // a customer error handler registered
        digester = new Digester() {
            public void warning(SAXParseException e) throws SAXException {
                if(errorHandler!=null) {
                    errorHandler.warning(e);
                }
            }
        };
        digester.setEntityResolver(new MuleDtdResolver());

        String temp = System.getProperty("org.mule.xml.validate", "true");
        digester.setValidating((temp.equalsIgnoreCase("true")));

        digester.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException exception) throws SAXException
            {
                logger.error(exception.getMessage(), exception);
                throw new SAXException(exception);
            }

            public void fatalError(SAXParseException exception) throws SAXException
            {
                logger.fatal(exception.getMessage(), exception);
                throw new SAXException(exception);
            }

            public void warning(SAXParseException exception) throws SAXException
            {
                logger.warn(exception.getMessage());
            }
        });

        String path = "mule-configuration";
        addMuleConfigurationRules(digester, path);
        addContainerContextRules(digester, path);
        addTransformerRules(digester, path);
        addMuleEnvironmentPropertiesRules(digester, path);
        addSecurityManagerRules(digester, path);
        addTransactionManagerRules(digester, path);
        addGlobalEndpointRules(digester, path);
        addEndpointIdentfierRules(digester, path);
        addInterceptorStackRules(digester, path);
        addConnectorRules(digester, path);
        addAgentRules(digester, path);

        addModelRules(digester, path);
        //Threse rules allow for individual component configurations
        addMuleDescriptorRules(digester, path);
    }

    /**
     * ConfigResource can be a url, a path on the local file system or a resource name on
     * the classpath
     * Finds and loads the configuration resource by doing the following -
     * 1. load it form the classpath
     * 2. load it from from the local file system
     * 3. load it as a url
     *
     * @param configResource
     * @return an inputstream to the resource
     * @throws ConfigurationException
     */
    protected InputStream loadConfig(String configResource) throws ConfigurationException
    {
        InputStream is = ClassHelper.getResourceAsStream(configResource, getClass());
        if (is == null) {
            File file = new File(configResource);
            if(file.exists()) {
                try
                {
                    is = new FileInputStream(file);
                }
                catch (FileNotFoundException e)
                {
                    throw new ConfigurationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE, configResource), e);

                }
            } else {
                try
                {
                    URL url = new URL(configResource);
                    is = url.openStream();
                } catch (Exception e)
                {
                    throw new ConfigurationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE, configResource));
                }
            }
        }
        return is;
    }

    public UMOManager configure(String configResources) throws ConfigurationException
    {
        String[] resources = Utility.split(configResources, ",");
        MuleManager.getConfiguration().setConfigResources(resources);
        ReaderResource[] readers = new ReaderResource[resources.length];
        for (int i = 0; i < resources.length; i++)
        {
            try
            {
                readers[i] = new ReaderResource(resources[i].trim(),
                        new InputStreamReader(loadConfig(resources[i].trim()), "UTF-8"));
            } catch (UnsupportedEncodingException e)
            {
                throw new ConfigurationException(e);
            }
        }
        return configure(readers);
    }

    public UMOManager configure(ReaderResource[] configResources) throws ConfigurationException
    {
        manager = MuleManager.getInstance();

        Reader configResource = null;
        for (int i = 0; i < configResources.length; i++)
        {
            try {
                configResource = configResources[i].getReader();
                digester.push(manager);
                manager = (UMOManager)digester.parse(configResource);
            } catch (Exception e) {
                throw new ConfigurationException(new Message(Messages.FAILED_TO_PARSE_CONFIG_RESOURCE_X, configResources[i].getDescription()), e);
            }
        }
        try
        {
            setContainerProperties();
            setTransformers();
            setGlobalEndpoints();
            manager.start();
        }
        catch (Exception e)
        {
            throw new ConfigurationException(new Message(Messages.X_FAILED_TO_INITIALISE, "MuleManager"), e);
        }
        return manager;
    }

    /**
     * Indicate whether this ConfigurationBulder has been configured yet
     * @return <code>true</code> if this ConfigurationBulder has been configured
     */
    public boolean isConfigured() {
        return manager != null;
    }

    protected void setContainerProperties() throws ContainerException
    {
        UMOContainerContext ctx = manager.getContainerContext();
        try
        {
            for (Iterator iterator = containerReferences.iterator(); iterator.hasNext();)
            {
                ContainerReference reference = (ContainerReference) iterator.next();
                reference.resolveReference(ctx);
            }
        } finally
        {
            containerReferences.clear();
        }
    }

    protected void setTransformers() throws InitialisationException
    {
        try
        {
            for (Iterator iterator = transformerReferences.iterator(); iterator.hasNext();)
            {
                TransformerReference reference = (TransformerReference) iterator.next();
                reference.resolveTransformer();
            }
        } finally
        {
            transformerReferences.clear();
        }
    }

    protected void setGlobalEndpoints() throws InitialisationException
    {
        //because Mule Xml allows developers to overload global endpoints
        //we need a way to initialise Global endpoints after the Xml has
        //been processed but before the MuleManager is initialised.  So we do
        //it here
        Map endpoints = MuleManager.getInstance().getEndpoints();
        UMOEndpoint ep;
        for (Iterator iterator = endpoints.values().iterator(); iterator.hasNext();) {
            ep = (UMOEndpoint) iterator.next();
            ep.initialise();
            MuleManager.getInstance().unregisterEndpoint(ep.getName());
            MuleManager.getInstance().registerEndpoint(ep);
        }

        try
        {
            for (Iterator iterator = endpointReferences.iterator(); iterator.hasNext();)
            {
                EndpointReference reference = (EndpointReference) iterator.next();
                reference.resolveEndpoint();
            }
        } finally
        {
            endpointReferences.clear();
        }
    }

    protected void addMuleConfigurationRules(Digester digester, String path)
    {
        digester.addSetProperties(path);
        //Create mule system properties and defaults
        path += "/mule-environment-properties";
        digester.addObjectCreate(path, MuleConfiguration.class);
        addSetPropertiesRule(path, digester);

        //Add pooling profile rules
        addPoolingProfileRules(digester, path);

        //Add Queue Profile rules
        addQueueProfileRules(digester, path);

        //set threading profile
        digester.addObjectCreate(path + "/threading-profile", THREADING_PROFILE);
        SetPropertiesRule threadingRule = new SetPropertiesRule();
        threadingRule.addAlias("setPoolExhaustedAction", "setPoolExhaustedActionString");
        digester.addRule(path + "/threading-profile", threadingRule);
        digester.addRule(path + "/threading-profile", new Rule()
        {
            private String id;
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                id = attributes.getValue("id");
            }

            public void end(String s, String s1) throws Exception
            {
                ThreadingProfile tp = (ThreadingProfile)digester.peek();
                MuleConfiguration cfg = (MuleConfiguration)digester.peek(1);

                if("default".equals(id)) {
                    cfg.setDefaultThreadingProfile(tp);
                    cfg.setMessageDispatcherThreadingProfile(tp);
                    cfg.setMessageReceiverThreadingProfile(tp);
                    cfg.setComponentThreadingProfile(tp);
                } else if ("messageReceiver".equals(id)) {
                    cfg.setMessageReceiverThreadingProfile(tp);
                } else if ("messageDispatcher".equals(id)) {
                    cfg.setMessageDispatcherThreadingProfile(tp);
                }  else if ("component".equals(id)) {
                    cfg.setComponentThreadingProfile(tp);
                }
            }
        });

        digester.addRule(path, new Rule(){
            public void end(String s, String s1) throws Exception
            {
                MuleManager.setConfiguration((MuleConfiguration)digester.peek());
            }
        });
    }

    protected void addMuleEnvironmentPropertiesRules(Digester digester, String path)
    {
        //Set environment properties
        path += "/environment-properties";
        addMulePropertiesRule(path, digester, false);
        digester.addRule( path, new Rule() {
            public void end(String s, String s1) throws Exception
            {
                Map prop = (Map)digester.peek();
                Map.Entry entry;
                for (Iterator iterator = prop.entrySet().iterator(); iterator.hasNext();)
                {
                    entry = (Map.Entry)iterator.next();
                    MuleManager.getInstance().setProperty(entry.getKey(), entry.getValue());

                }
                super.end(s, s1);
            }
        });
    }

    protected void addSecurityManagerRules(Digester digester, String path) throws ConfigurationException
    {
        //Create container Context
        path += "/security-manager";
        digester.addObjectCreate(path, DEFAULT_SECURITY_MANAGER, "className");

        //Add propviders
        digester.addObjectCreate(path + "/security-provider", SECURITY_PROVIDER_INTERFACE, "className");
        addSetPropertiesRule(path + "/security-provider", digester);
        addMulePropertiesRule(path + "/security-provider", digester, true);
        digester.addSetNext(path + "/security-provider", "addProvider");

        //Add encryption strategies
        digester.addObjectCreate(path + "/encryption-strategy", ENCRYPTION_STRATEGY_INTERFACE, "className");
        addSetPropertiesRule(path + "/encryption-strategy", digester);
        addMulePropertiesRule(path + "/encryption-strategy", digester, true);
        digester.addRule(path  + "/encryption-strategy", new Rule(){
            private String name;

            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception
            {
                name = attributes.getValue("name");
            }

            public void end(String endpointName, String endpointName1) throws Exception
            {
                UMOEncryptionStrategy s = (UMOEncryptionStrategy)digester.peek();
                ((UMOSecurityManager)digester.peek(1)).addEncryptionStrategy(name, s);
            }
        });
        digester.addSetNext(path, "setSecurityManager");

    }

    protected void addContainerContextRules(Digester digester, String path) throws ConfigurationException
    {
        //Create container Context
        path += "/container-context";
        digester.addObjectCreate(path, DEFAULT_CONTAINER_CONTEXT, "className");
        addMulePropertiesRule(path, digester, true);


        NodeCreateRule nodeCreateRule = null;
        try {
            nodeCreateRule = new NodeCreateRule(Node.DOCUMENT_FRAGMENT_NODE) {
                private String encoding;
                private String doctype;
                public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception {
                    encoding = attributes.getValue("encoding");
                    doctype = attributes.getValue("doctype");
                    super.begin(endpointName, endpointName1, attributes);
                }

                public void end(String endpointName, String endpointName1) throws Exception {
                    super.end(endpointName, endpointName1);

                    DocumentFragment config = (DocumentFragment)digester.pop();
                    StringWriter s = new StringWriter();
                    StreamResult streamResult = new StreamResult(s);
                    TransformerFactory tFactory = TransformerFactory.newInstance();
                    try {
                        Transformer transformer = tFactory.newTransformer();
                        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                        transformer.transform(new DOMSource(config), streamResult);
                    } catch (TransformerException e) {
                        throw new ContainerException(new Message(Messages.COULD_NOT_RECOVER_CONTIANER_CONFIG), e);
                    }
                    Reader reader = new StringReader(s.toString());
                    UMOContainerContext ctx = (UMOContainerContext) digester.peek();
                    ctx.configure(reader, doctype, encoding);
                    }
            };
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException(e);
        }
        digester.addRule(path + "/configuration", nodeCreateRule);
        digester.addSetRoot(path, "setContainerContext");
   }

    protected void addTransformerRules(Digester digester, String path) throws ConfigurationException
    {
        //Create Transformers
        path += "/transformers/transformer";
        digester.addObjectCreate(path, TRANSFORMER_INTERFACE, "className");
        addSetPropertiesRule(path, digester);

        addMulePropertiesRule(path, digester, true);
        digester.addSetRoot(path, "registerTransformer");
    }

    protected void addGlobalEndpointRules(Digester digester, String path) throws ConfigurationException
    {
        //Create global message endpoints
        path += "/global-endpoints";
        addEndpointRules(digester, path, "registerEndpoint");
    }

    protected void addEndpointIdentfierRules(Digester digester, String path) throws ConfigurationException
    {
        //Create and reqister endpoints
        path += "/endpoint-identifiers/endpoint-identifier";
        digester.addCallMethod(path, "registerEndpointIdentifier", 2);
        digester.addCallParam(path, 0, "name");
        digester.addCallParam(path, 1, "value");
    }

    protected void addTransactionManagerRules(Digester digester, String path) throws ConfigurationException
    {
        //Create transactionManager
        path +="/transaction-manager";
        digester.addObjectCreate(path, TRANSACTION_MANAGER_FACTORY_INTERFACE, "factory");
        addMulePropertiesRule(path, digester, true);

        digester.addSetRoot(path, "setTransactionManager");
        digester.addRule(path, new Rule()
        {
            public void end(String s, String s1) throws Exception
            {
                UMOTransactionManagerFactory txFactory = (UMOTransactionManagerFactory) digester.pop();
                digester.push(txFactory.create());
            }
        });
    }

    protected void addAgentRules(Digester digester, String path) throws ConfigurationException
    {
        //Create Agents
        path += "/agents/agent";
        digester.addObjectCreate(path, AGENT_INTERFACE, "className");
        addSetPropertiesRule(path, digester);

        addMulePropertiesRule(path, digester, true);

        digester.addSetRoot(path, "registerAgent");
    }

    protected void addConnectorRules(Digester digester, String path) throws ConfigurationException
    {
        //Create connectors
        path += "/connector";
        digester.addObjectCreate(path, CONNECTOR_INTERFACE, "className");
        addSetPropertiesRule(path, digester);

        addMulePropertiesRule(path, digester, true);

        digester.addRule(path + "/threading-profile", new Rule()
        {
            private String id;
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                //use the global tp as a template
                MuleConfiguration cfg = ((MuleManager)digester.getRoot()).getConfiguration();
                id = attributes.getValue("id");
                if("default".equals(id)) {
                    digester.push(cfg.getDefaultThreadingProfile());
                } else if ("receiver".equals(id)) {
                    digester.push(cfg.getMessageReceiverThreadingProfile());
                } else if ("dispatcher".equals(id)) {
                    digester.push(cfg.getMessageDispatcherThreadingProfile());
                }

            }

            public void end(String s, String s1) throws Exception
            {
                ThreadingProfile tp = (ThreadingProfile)digester.pop();
                AbstractConnector cnn = (AbstractConnector)digester.peek();

                if("default".equals(id)) {
                    cnn.setReceiverThreadingProfile(tp);
                    cnn.setDispatcherThreadingProfile(tp);
                } else if ("receiver".equals(id)) {
                    cnn.setReceiverThreadingProfile(tp);
                } else if ("dispatcher".equals(id)) {
                    cnn.setDispatcherThreadingProfile(tp);
                }
            }
        });

        SetPropertiesRule threadingRule = new SetPropertiesRule();
        threadingRule.addAlias("setPoolExhaustedAction", "setPoolExhaustedActionString");
        digester.addRule(path + "/threading-profile", threadingRule);

        addExceptionStrategyRules(digester, path);
        //initialise the connector
        //digester.addCallMethod(path, "initialise");
        //register conntector
        digester.addSetRoot(path, "registerConnector");
    }

    protected void addInterceptorStackRules(Digester digester, String path) throws ConfigurationException
    {
        //Create Inteceptor stacks
        path += "/interceptor-stack";
        digester.addRule(path + "/interceptor", new ObjectCreateRule(INTERCEPTOR_INTERFACE, "className")
        {
            public void end(String s, String s1) throws Exception
            {
                /*do not pop the result*/
            } });

        digester.addRule(path, new Rule()
        {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                digester.push(attributes.getValue("name"));
            }

            public void end(String s, String s1) throws Exception
            {
                List list = new ArrayList();
                Object obj = digester.peek();
                while (obj instanceof UMOInterceptor)
                {
                    list.add(0, digester.pop());
                    obj = digester.peek();
                }
                manager.registerInterceptorStack(digester.pop().toString(), list);
            }
        });
    }

    protected void addModelRules(Digester digester, String path) throws ConfigurationException
    {
        //Create Model
        path += "/model";
        digester.addObjectCreate(path, DEFAULT_MODEL, "className");
        addSetPropertiesRule(path, digester);

        digester.addSetRoot(path, "setModel");

        //Create endpointUri resolver
        digester.addObjectCreate(path + "/entry-point-resolver", DEFAULT_ENTRY_POINT_RESOLVER, "className");
        addSetPropertiesRule(path + "/entry-point-resolver", digester);

        digester.addSetNext(path + "/entry-point-resolver", "setEntryPointResolver");

        //Create lifecycle adapter
        digester.addObjectCreate(path + "/component-lifecycle-adapter-factory", DEFAULT_LIFECYCLE_ADAPTER, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path + "/component-lifecycle-adapter-factory", "setLifecycleAdapterFactory");

        //Create component factory
        digester.addObjectCreate(path + "/component-factory", DEFAULT_COMPONENT_FACTORY, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path + "/component-factory", "setComponentFactory");

        //Pool factory
        addPoolingProfileRules(digester, path);

        //Exception strategy
        addExceptionStrategyRules(digester, path);

        //Add Components
        addMuleDescriptorRules(digester, path);
    }

    protected void addMuleDescriptorRules(Digester digester, String path) throws ConfigurationException
    {
        //Create Mule UMOs
        path += "/mule-descriptor";
        digester.addObjectCreate(path, DEFAULT_DESCRIPTOR, "className");
        addSetPropertiesRule(path, digester);

        //Create Message Routers
        addMessageRouterRules(digester, path, "inbound");
        addMessageRouterRules(digester, path, "outbound");
        addMessageRouterRules(digester, path, "response");

        //Add threading profile rules
        addThreadingProfileRules(digester, path, "component");

        //Add pooling profile rules
        addPoolingProfileRules(digester, path);

        //queue profile rules
        addQueueProfileRules(digester, path);

        //Create interceptors
        digester.addRule(path + "/interceptor", new Rule() {
            public void begin(String string, String string1, Attributes attributes) throws Exception
            {
                String value = attributes.getValue("className");
                UMOManager man = (UMOManager) digester.getRoot();
                List interceptorStack = man.lookupInterceptorStack(value);
                MuleDescriptor temp = (MuleDescriptor) digester.peek();
                if (interceptorStack != null)
                {
                    Iterator iter = interceptorStack.iterator();
                    while (iter.hasNext())
                    {
                        temp.addInterceptor((UMOInterceptor) iter.next());
                    }
                }
                else
                {
                    //Instantiate the new object and push it on the context stack
                    Class clazz = digester.getClassLoader().loadClass(value);
                    Object instance = clazz.newInstance();
                    temp.addInterceptor((UMOInterceptor) instance);
                    digester.push(instance);
                }
            }

            public void end(String s, String s1) throws Exception
            {
                if(digester.peek() instanceof UMOInterceptor) {
                    digester.pop();
                }
            }
        });

        addMulePropertiesRule(path + "/interceptor", digester, true);

        //Set exception strategy
        addExceptionStrategyRules(digester, path);

        addSetPropertiesRule(path, digester);
        addMulePropertiesRule(path, digester, false);
        digester.addSetNext(path + "/properties", "setProperties");

        //register the component
        digester.addRule(path, new Rule(){
            public void end(String s, String s1) throws Exception
            {
                UMODescriptor descriptor = (UMODescriptor)digester.peek();
                Object obj = digester.peek(1);
                if(obj instanceof UMOManager) {
                    ((UMOManager)obj).getModel().registerComponent(descriptor);
                } else {
                    ((UMOModel)obj).registerComponent(descriptor);
                }
            }
        });
    }

    protected void addThreadingProfileRules(Digester digester, String path, final String type)
    {
        //set threading profile
        digester.addRule(path + "/threading-profile", new Rule(){
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                //use the default as a template
                MuleConfiguration cfg = ((MuleManager)digester.getRoot()).getConfiguration();
                if("component".equals(type)) {
                    digester.push(cfg.getComponentThreadingProfile());
                } else  if("messageReceiver".equals(type)) {
                    digester.push(cfg.getComponentThreadingProfile());
                } else if("messageDispatcher".equals(type)) {
                    digester.push(cfg.getComponentThreadingProfile());
                } else {
                    digester.push(cfg.getDefaultThreadingProfile());
                }
            }

            public void end(String s, String s1) throws Exception
            {
                digester.pop();
            }
        });
        //set threading profile
        SetPropertiesRule threadingRule = new SetPropertiesRule();
        threadingRule.addAlias("setPoolExhaustedAction", "setPoolExhaustedActionString");
        digester.addRule(path + "/threading-profile", threadingRule);
        digester.addSetNext(path + "/threading-profile", "setThreadingProfile");
    }

    protected void addPoolingProfileRules(Digester digester, String path)
    {
        //set pooling profile
        digester.addRule(path + "/pooling-profile", new Rule(){
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                //use the default as a template
                MuleConfiguration cfg = ((MuleManager)digester.getRoot()).getConfiguration();
                digester.push(cfg.getPoolingProfile());
            }

            public void end(String s, String s1) throws Exception
            {
                digester.pop();
            }
        });

        SetPropertiesRule rule = new SetPropertiesRule();
        rule.addAlias("exhaustedAction", "exhaustedActionString");
        rule.addAlias("initialisationPolicy", "initialisationPolicyString");
        digester.addRule(path + "/pooling-profile", rule);
        digester.addSetNext(path + "/pooling-profile", "setPoolingProfile");
    }

    protected void addQueueProfileRules(Digester digester, String path)
    {
        digester.addObjectCreate(path + "/queue-profile", QUEUE_PROFILE);
        addSetPropertiesRule(path+ "/queue-profile", digester);

        digester.addObjectCreate(path + "/queue-profile/persistence-strategy", PERSISTENCE_STRATEGY_INTERFACE , "className");
        addMulePropertiesRule(path + "/queue-profile/persistence-strategy", digester, true);
        digester.addSetNext(path + "/queue-profile/persistence-strategy", "setPersistenceStrategy");

        digester.addSetNext(path + "/queue-profile", "setQueueProfile");
    }

    protected void addMessageRouterRules(Digester digester, String path, String type) throws ConfigurationException
    {
        String defaultRouter = null;
        String setMethod = null;
        if("inbound".equals(type)) {
            defaultRouter = DEFAULT_INBOUND_MESSAGE_ROUTER;
            setMethod = "setInboundRouter";
            path += "/inbound-router";
            //Add endpoints for multiple inbound endpoints
            addEndpointRules(digester, path, "addEndpoint");
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        } else if("response".equals(type)) {
            defaultRouter = DEFAULT_RESPONSE_MESSAGE_ROUTER;
            setMethod = "setResponseRouter";
            path += "/response-router";
            //Add endpoints for multiple response endpoints i.e. replyTo addresses
            addEndpointRules(digester, path, "addEndpoint");
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        } else {
            defaultRouter = DEFAULT_OUTBOUND_MESSAGE_ROUTER;
            setMethod = "setOutboundRouter";
            path += "/outbound-router";
        }
        digester.addObjectCreate(path, defaultRouter, "className");
        addSetPropertiesRule(path, digester);

        //Add Catch All strategy
        digester.addObjectCreate(path + "/catch-all-strategy", DEFAULT_CATCH_ALL_STRATEGY, "className");
        addSetPropertiesRule(path + "/catch-all-strategy", digester);

        //Add endpointUri for catch-all strategy
        addEndpointRules(digester, path + "/catch-all-strategy", "setEndpoint");
        addGlobalReferenceEndpointRules(digester, path + "/catch-all-strategy", "setEndpoint");

        addMulePropertiesRule(path + "/catch-all-strategy", digester, true);
        digester.addSetNext(path + "/catch-all-strategy", "setCatchAllStrategy");

        //Add router rules
        addRouterRules(digester, path, type);

        //add the router to the descriptor
        digester.addSetNext(path, setMethod);
    }

    protected void addRouterRules(Digester digester, String path, final String type) throws ConfigurationException
    {
        path += "/router";
        if("inbound".equals(type)) {
            digester.addObjectCreate(path, INBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        } else if("response".equals(type)) {
            digester.addObjectCreate(path, RESPONSE_MESSAGE_ROUTER_INTERFACE, "className");
        } else {
            digester.addObjectCreate(path, OUTBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        }

        addSetPropertiesRule(path, digester, new String[]{"enableCorrelation"}, new String[]{"enableCorrelationAsString"});
        addMulePropertiesRule(path, digester, true);
        if("outbound".equals(type)) {
            addEndpointRules(digester, path, "addEndpoint");
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
            addTransactionConfigRules(path, digester);
        }
        addFilterRules(digester, path);

        //Set the router on the to the message router
        digester.addSetNext(path, "addRouter");
    }

    protected void addFilterRules(Digester digester, String path) throws ConfigurationException
    {
        //three levels
        addSingleFilterRule(digester, path);
        path += "/filter";
        addFilterGroupRule(digester, path);

        addFilterGroupRule(digester, path + "/left-filter");
        addFilterGroupRule(digester, path + "/right-filter");
        addFilterGroupRule(digester, path + "/filter");

        addFilterGroupRule(digester, path + "/left-filter/left-filter");
        addFilterGroupRule(digester, path + "/left-filter/right-filter");
        addFilterGroupRule(digester, path + "/left-filter/filter");

        addFilterGroupRule(digester, path + "/right-filter/left-filter");
        addFilterGroupRule(digester, path + "/right-filter/right-filter");
        addFilterGroupRule(digester, path + "/right-filter/filter");

        addFilterGroupRule(digester, path + "/filter/left-filter");
        addFilterGroupRule(digester, path + "/filter/right-filter");
        addFilterGroupRule(digester, path + "/filter/filter");

        //digester.addSetNext(path, "setFilter");
    }

    protected void addFilterGroupRule(Digester digester, String path) throws ConfigurationException
    {
        addLeftFilterRule(digester, path);
        addRightFilterRule(digester, path);
        addSingleFilterRule(digester, path);
    }

    protected void addLeftFilterRule(Digester digester, String path) throws ConfigurationException
    {
        path += "/left-filter";
        digester.addObjectCreate(path, FILTER_INTERFACE, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path, "setLeftFilter");
    }

    protected void addRightFilterRule(Digester digester, String path) throws ConfigurationException
    {
        path += "/right-filter";
        digester.addObjectCreate(path, FILTER_INTERFACE, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path, "setRightFilter");
    }

    protected void addSingleFilterRule(Digester digester, String path) throws ConfigurationException
    {
        path += "/filter";
        digester.addObjectCreate(path, FILTER_INTERFACE, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path, "setFilter");
    }

    protected void addEndpointRules(Digester digester, String path, String method) throws ConfigurationException {
        //Set message endpoint
        path += "/endpoint";
        digester.addObjectCreate(path, DEFAULT_ENDPOINT);
        addCommonEndpointRules(digester, path, method);
    }

    protected void addGlobalReferenceEndpointRules(Digester digester, String path, final String method) throws ConfigurationException {
        //Set message endpoint
        path += "/global-endpoint";
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                String name = attributes.getValue("name");
                String address = attributes.getValue("address");
                String trans = attributes.getValue("transformers");
                String createConnector = attributes.getValue("createConnector");
                EndpointReference ref = new EndpointReference(method, name, address, trans, createConnector, digester.peek());
                digester.push(ref);
            }

            public void end(String endpointName, String endpointName1) throws Exception
            {
                endpointReferences.add((EndpointReference)digester.pop());
            }
        }
        );
        addCommonEndpointRules(digester, path, null);
    }

    protected void addCommonEndpointRules(Digester digester, String path, String method) throws ConfigurationException {
        addSetPropertiesRule(path, digester, new String[]{"address", "transformers", "createConnector"}, new String[]{"endpointURI", "transformer", "createConnectorAsString"} );
        addMulePropertiesRule(path, digester, false);
        addTransactionConfigRules(path, digester);

        addFilterRules(digester, path);
        if(method!=null) {
            digester.addSetNext(path, method);
        }

        //Add security filter rules
        digester.addObjectCreate(path + "/security-filter", ENDPOINT_SECURITY_FILTER_INTERFACE, "className");

        addMulePropertiesRule(path + "/security-filter", digester, true);
        digester.addSetNext(path + "/security-filter", "setSecurityFilter");
    }

    protected void addTransactionConfigRules(String path, Digester digester)
    {
        digester.addObjectCreate(path + "/transaction", DEFAULT_TRANSACTION_CONFIG);
        addSetPropertiesRule(path + "/transaction", digester, new String[] {"action"},  new String[] {"actionAsString"});

        digester.addObjectCreate(path + "/transaction/constraint", TRANSACTION_CONSTRAINT_INTERFACE, "className");
        addSetPropertiesRule(path + "/transaction/constraint", digester);

        digester.addSetNext(path + "/transaction/constraint", "setConstraint");
        digester.addSetNext(path + "/transaction", "setTransactionConfig");
    }

    protected void addExceptionStrategyRules(Digester digester, String path) throws ConfigurationException {
        path += "/exception-strategy";
        digester.addObjectCreate(path , EXCEPTION_STRATEGY_INTERFACE, "className");
        addMulePropertiesRule(path, digester, true);

        //Add endpoint rules
        addEndpointRules(digester, path, "addEndpoint");
        addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        digester.addSetNext(path, "setExceptionListener");
    }

    protected void addSetPropertiesRule(String path, Digester digester) {
        digester.addRule(path, new MuleSetPropertiesRule());
    }

    protected void addSetPropertiesRule(String path, Digester digester, String[] s1, String[] s2) {
        digester.addRule(path, new MuleSetPropertiesRule(s1, s2));
    }

    protected void addMulePropertiesRule(String path, Digester digester, final boolean setAsBeanProperties)
    {
        //small hack to allow the same property rules to be used but the environment-properties elements
        if(!path.endsWith("environment-properties")) {
            path += "/properties";
        }
        digester.addRule(path, new ObjectCreateRule(path , HashMap.class){

            //This will set the properties on the top object as bean setters if the flag is set
            public void end(String string, String string1) throws Exception
            {

                Map props = (Map)digester.peek();
                if(props.containsKey(MuleConfiguration.USE_MANAGER_PROPERTIES))
                {
                    props.putAll(MuleManager.getInstance().getProperties());
                    props.remove(MuleConfiguration.USE_MANAGER_PROPERTIES);
                }
                super.end(string, string1);

                //support for setting transformers as properties
                String trans = (String)props.remove("transformer");
                if(setAsBeanProperties)
                {
                    org.mule.util.BeanUtils.populateWithoutFail(digester.peek(), props, true);
                } else {
                    BeanUtils.setProperty(digester.peek(), string1, props);
                }
                if(trans!=null) {
                    addTransformerReference("transformer", trans, digester.peek());
                }
            }
        });
        digester.addCallMethod(path + "/property","put", 2);
        digester.addRule(path + "/property", new CallParamRule(0, "name") {
            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception {
                //Process template tokens
                attributes = processAttributes(attributes, endpointName1);
                super.begin(endpointName, endpointName1, attributes);
            }
        });

        digester.addRule(path + "/property", new CallParamRule(1, "value") {
            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception {
                //Process template tokens
                attributes = processAttributes(attributes, endpointName1);
                super.begin(endpointName, endpointName1, attributes);
            }
        });
//        digester.addCallParam(path + "/property", 0, "name");
//        digester.addCallParam(path + "/property", 1, "value");

        addPropertyFactoryRule(digester, path + "/factory-property");
        addSystemPropertyRule(digester, path + "/system-property");
        addFilePropertiesRule(digester, path + "/file-properties");
        addContainerPropertyRule(digester, path + "/container-property", setAsBeanProperties);

        digester.addObjectCreate(path + "/map", HashMap.class);
        digester.addCallMethod(path + "/map/property", "put", 2);

        digester.addRule(path + "/map/property", new CallParamRule(0, "name") {
            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception {
                //Process template tokens
                attributes = processAttributes(attributes, endpointName1);
                super.begin(endpointName, endpointName1, attributes);
            }
        });

        digester.addRule(path + "/map/property", new CallParamRule(1, "value") {
            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception {
                //Process template tokens
                attributes = processAttributes(attributes, endpointName1);
                super.begin(endpointName, endpointName1, attributes);
            }
        });
//        digester.addCallParam(path + "/map/property", 0, "name");
//        digester.addCallParam(path + "/map/property", 1, "value");

        addPropertyFactoryRule(digester, path + "/map/factory-property");
        addSystemPropertyRule(digester, path + "/map/system-property");
        addFilePropertiesRule(digester, path + "/map/file-properties");
        addContainerPropertyRule(digester, path + "/map/container-property", false);

        //A small hack to call a method on top -1
        digester.addRule(path + "/map", new CallMethodRule("put", 2){
            public void end(String string, String string1) throws Exception
            {
                Map props = (Map)digester.peek();
                if(props.containsKey(MuleConfiguration.USE_MANAGER_PROPERTIES))
                {
                    props.putAll(MuleManager.getInstance().getProperties());
                    props.remove(MuleConfiguration.USE_MANAGER_PROPERTIES);
                }
                Object o = digester.peek(1);
                digester.push(o);
                super.end(string, string1);
                o = digester.pop();
            }
        });
        digester.addCallParam(path + "/map", 0, "name");
        digester.addCallParam(path + "/map", 1, true);

        digester.addObjectCreate(path + "/list", ArrayList.class);

        //digester.addCallMethod(path + "/list/entry", "add", 1);
        digester.addRule(path + "/list/entry", new CallMethodRule("add", 1) {
            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception {
                //Process template tokens
                attributes = processAttributes(attributes, endpointName1);
                super.begin(endpointName, endpointName1, attributes);
            }
        });
        digester.addRule(path + "/list/entry", new CallParamRule(0, "value"){
            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception {
                //Process template tokens
                attributes = processAttributes(attributes, endpointName1);
                super.begin(endpointName, endpointName1, attributes);
            }
        });

        addPropertyFactoryRule(digester, path + "/list/factory-entry");
        addSystemPropertyRule(digester, path + "/list/system-entry");
        addContainerPropertyRule(digester, path + "/list/container-entry", false);

        //A small hack to call a method on top -1
        digester.addRule(path + "/list", new CallMethodRule("put", 2){
            public void end(String string, String string1) throws Exception
            {
                Object o = digester.peek(1);
                digester.push(o);
                super.end(string, string1);
                o = digester.pop();
            }
        });
        digester.addCallParam(path + "/list", 0, "name");
        digester.addCallParam(path + "/list", 1, true);
    }

    protected void addPropertyFactoryRule(Digester digester, String path) {
        digester.addRule(path, new Rule() {

            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                //Process template tokens
                attributes = processAttributes(attributes, s1);

                String clazz = attributes.getValue("factory");
                String name = attributes.getValue("name");
                Object props = digester.peek();
                Object obj = ClassHelper.instanciateClass(clazz, ClassHelper.NO_ARGS);
                if(obj instanceof PropertyFactory) {
                    if(props instanceof Map) {
                        obj= ((PropertyFactory)obj).create((Map)props);
                    } else {
                        //this must be a list so we'll get the containing properties map
                        obj= ((PropertyFactory)obj).create((Map)digester.peek(1));
                    }
                }
                if(obj !=null) {
                    if(props instanceof Map) {
                        ((Map)props).put(name, obj);
                    }else {
                        ((List)props).add(obj);
                    }
                }
            }
        });
    }

    protected void addSystemPropertyRule(Digester digester, String path) {
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                //Process template tokens
                attributes = processAttributes(attributes, s1);

                String name = attributes.getValue("name");
                String key = attributes.getValue("key");
                String defaultValue = attributes.getValue("defaultValue");
                String value = System.getProperty(key, defaultValue);
                if(value!=null) {
                    Object props = digester.peek();
                    if(props instanceof Map) {
                        ((Map)props).put(name, value);
                    } else {
                        ((List)props).add(value);
                    }
                }
            }
        });
    }

    protected void addFilePropertiesRule(Digester digester, String path) {
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                //Process template tokens
                attributes = processAttributes(attributes, s1);

                String location = attributes.getValue("location");
                String temp = attributes.getValue("override");
                boolean override = "true".equalsIgnoreCase(temp);
                InputStream is = Utility.loadResource(location, getClass());
                if(is==null) {
                    throw new FileNotFoundException(location);
                }
                Properties p = new Properties();
                p.load(is);
                Map props = (Map)digester.peek();
                if(override) {
                    props.putAll(p);
                } else {
                    String key;
                    for (Iterator iterator = p.keySet().iterator(); iterator.hasNext();) {
                        key = (String) iterator.next();
                        if(!props.containsKey(key)) {
                            props.put(key, p.getProperty(key));
                        }
                    }
                }
            }
        });
    }

    protected void addContainerPropertyRule(Digester digester, String path, final boolean setAsBeanProperties) {
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                attributes = processAttributes(attributes, s1);

                String name = attributes.getValue("name");
                String value = attributes.getValue("reference");
                String required = attributes.getValue("required");
                String container = attributes.getValue("container");
                if(required==null) required = "true";
                boolean req = Boolean.valueOf(required).booleanValue();
                //if we're not setting as bean properties we need get the topmost object
                //which will be a list or Map
                Object obj = null;
                if(setAsBeanProperties) {
                    obj = digester.peek(1);
                } else {
                    obj = digester.peek();
                }
                addContainerReference(name, value, obj, req, container);
            }
        });
    }

    private void addContainerReference(String propName, String containerRef, Object object, boolean required, String container) {
        containerReferences.add(new ContainerReference(propName, containerRef, object, required, container));
    }

    private void addTransformerReference(String propName, String transName, Object object) {
        transformerReferences.add(new TransformerReference(propName, transName, object));
    }

    private void addEndpointReference(String propName, String endpointName, Object object) {
        endpointReferences.add(new EndpointReference(propName, endpointName, null, null, null, object));
    }

    /**
     * this rule serves 2 functions -
     * 1. Allows for late binding of certain types of object, namely Transformers and endpoints
     * that need to be set on objects once the Manager configuration has been processed
     * 2. Allows for template parameters to be parse on the configuration file in the form of
     * ${param-name}.  These will get resolved against properties set in the mule-properites element
     */
    private class MuleSetPropertiesRule extends SetPropertiesRule
    {

        public MuleSetPropertiesRule()
        {
        }

        public MuleSetPropertiesRule(String s, String s1)
        {
            super(s, s1);
        }

        public MuleSetPropertiesRule(String[] strings, String[] strings1)
        {
            super(strings, strings1);
        }

        public void begin(String s1, String s2, Attributes attributes) throws Exception
        {
            attributes = processAttributes(attributes, s2);
            //Add transformer references that will be bound to their objects once
            //all configuration has bean read
            String transformerNames = attributes.getValue("transformer");
            if(transformerNames!=null) {
                addTransformerReference("transformer", transformerNames, digester.peek());
            }
            transformerNames = attributes.getValue("transformers");
            if(transformerNames!=null) {
                addTransformerReference("transformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("inboundTransformer");
            if(transformerNames!=null) {
                addTransformerReference("inboundTransformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("outboundTransformer");
            if(transformerNames!=null) {
                addTransformerReference("outboundTransformer", transformerNames, digester.peek());
            }

            //Special case handling of global endpoint refs on the inboundEndpoint/
            //outboundendpoint attributes of the descriptor
            String endpoint = attributes.getValue("inboundEndpoint");
            if(endpoint!=null) {
                Object o = PropertiesHelper.getProperty(manager.getEndpoints(), endpoint, null);
                if(o!=null)
                addEndpointReference("setInboundEndpoint", endpoint, digester.peek());
            }

            endpoint = attributes.getValue("outboundEndpoint");
            if(endpoint!=null) {
                Object o = PropertiesHelper.getProperty(manager.getEndpoints(), endpoint, null);
                if(o!=null)
                addEndpointReference("setOutboundEndpoint", endpoint, digester.peek());
            }

            super.begin(attributes);
        }
    }

    private static Attributes processAttributes(Attributes attributes, String elementName) throws ConfigurationException {
        AttributesImpl attribs = new AttributesImpl(attributes);
        String value = null;
        String realValue = null;
        String key = null;
        if(elementName.equals("property")) {
            System.out.println("");
        }
        UMOManager manager = MuleManager.getInstance();
        for(int i = 0; i < attribs.getLength(); i++) {
            value = attribs.getValue(i);
            int x = value.indexOf("${");
            while(x > -1) {
                int y = value.indexOf("}", x +1);
                if(y==-1) {
                    throw new ConfigurationException(new Message(Messages.PROPERTY_TEMPLATE_MALFORMED_X,
                            "<" + elementName + attribs.getLocalName(i) + "='" + value + "' ...>"));
                }
                key = value.substring(x+2, y);
                realValue = (String)manager.getProperty(key);
                if(logger.isDebugEnabled()) {
                    logger.debug("Param is '" + value + "', Property key is '" + key + "', Property value is '" + realValue + "'");
                }
                if(realValue!=null) {
                    value = value.substring(0, x) +  realValue + value.substring(y+1);
                } else {
                    logger.info("Property for placeholder: '" + key + "' was not found.  Leaving place holder as is");
                }
                x = value.indexOf("${", y);
            }
            attribs.setValue(i, value);
        }
        return attribs;
    }
}