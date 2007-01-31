/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleDtdResolver;
import org.mule.config.MuleProperties;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ReaderResource;
import org.mule.config.ThreadingProfile;
import org.mule.config.converters.ConnectorConverter;
import org.mule.config.converters.EndpointConverter;
import org.mule.config.converters.EndpointURIConverter;
import org.mule.config.converters.TransactionFactoryConverter;
import org.mule.config.converters.TransformerConverter;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.config.pool.CommonsPoolFactory;
import org.mule.impl.DefaultLifecycleAdapter;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.model.ModelFactory;
import org.mule.impl.model.resolvers.DynamicEntryPointResolver;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.interceptors.InterceptorStack;
import org.mule.providers.AbstractConnector;
import org.mule.providers.ConnectionStrategy;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.nested.NestedRouter;
import org.mule.routing.nested.NestedRouterCollection;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.routing.response.ResponseRouterCollection;
import org.mule.transaction.constraints.BatchConstraint;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOTransactionManagerFactory;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;
import org.mule.util.queue.EventFilePersistenceStrategy;

import java.beans.ExceptionListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectCreateRule;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.SetNextRule;
import org.apache.commons.digester.SetPropertiesRule;
import org.xml.sax.Attributes;

/**
 * <code>MuleXmlConfigurationBuilder</code> is a configuration parser that builds a
 * MuleManager instance based on a mule xml configration file defined in the
 * mule-configuration.dtd.
 */
public class MuleXmlConfigurationBuilder extends AbstractDigesterConfiguration
    implements ConfigurationBuilder
{
    public static final String DEFAULT_ENTRY_POINT_RESOLVER = DynamicEntryPointResolver.class.getName();
    public static final String DEFAULT_LIFECYCLE_ADAPTER = DefaultLifecycleAdapter.class.getName();
    public static final String DEFAULT_ENDPOINT = MuleEndpoint.class.getName();
    public static final String DEFAULT_TRANSACTION_CONFIG = MuleTransactionConfig.class.getName();
    public static final String DEFAULT_DESCRIPTOR = MuleDescriptor.class.getName();
    public static final String DEFAULT_SECURITY_MANAGER = MuleSecurityManager.class.getName();
    public static final String DEFAULT_OUTBOUND_ROUTER_COLLECTION = OutboundRouterCollection.class.getName();
    public static final String DEFAULT_INBOUND_ROUTER_COLLECTION = InboundRouterCollection.class.getName();
    public static final String DEFAULT_NESTED_ROUTER_COLLECTION = NestedRouterCollection.class.getName();
    public static final String DEFAULT_RESPONSE_ROUTER_COLLECTION = ResponseRouterCollection.class.getName();
    public static final String DEFAULT_NESTED_ROUTER = NestedRouter.class.getName();
    public static final String DEFAULT_CATCH_ALL_STRATEGY = LoggingCatchAllStrategy.class.getName();
    public static final String DEFAULT_POOL_FACTORY = CommonsPoolFactory.class.getName();
    public static final String THREADING_PROFILE = ThreadingProfile.class.getName();
    public static final String POOLING_PROFILE = PoolingProfile.class.getName();
    public static final String QUEUE_PROFILE = QueueProfile.class.getName();

    public static final String PERSISTENCE_STRATEGY_INTERFACE = EventFilePersistenceStrategy.class.getName();
    public static final String INBOUND_MESSAGE_ROUTER_INTERFACE = UMOInboundRouterCollection.class.getName();
    public static final String NESTED_MESSAGE_ROUTER_INTERFACE = UMONestedRouterCollection.class.getName();
    public static final String RESPONSE_MESSAGE_ROUTER_INTERFACE = UMOResponseRouterCollection.class.getName();
    public static final String OUTBOUND_MESSAGE_ROUTER_INTERFACE = UMOOutboundRouterCollection.class.getName();
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
    public static final String EXCEPTION_STRATEGY_INTERFACE = ExceptionListener.class.getName();
    public static final String CONNECTION_STRATEGY_INTERFACE = ConnectionStrategy.class.getName();

    protected UMOManager manager;

    private final List transformerReferences = new ArrayList();
    private final List endpointReferences = new ArrayList();

    public MuleXmlConfigurationBuilder() throws ConfigurationException
    {
        super(System.getProperty(MuleProperties.XML_VALIDATE_SYSTEM_PROPERTY, "true")
            .equalsIgnoreCase("true"), System.getProperty(MuleProperties.XML_DTD_SYSTEM_PROPERTY,
            MuleDtdResolver.DEFAULT_MULE_DTD));

        ConvertUtils.register(new EndpointConverter(), UMOEndpoint.class);
        ConvertUtils.register(new TransformerConverter(), UMOTransformer.class);
        ConvertUtils.register(new ConnectorConverter(), UMOConnector.class);
        ConvertUtils.register(new TransactionFactoryConverter(), UMOTransactionFactory.class);
        ConvertUtils.register(new EndpointURIConverter(), UMOEndpointURI.class);

        String path = getRootName();
        addManagerRules(digester, path);
        addServerPropertiesRules(path + "/environment-properties", "addProperties", 0);
        addContainerContextRules(path + "/container-context", "setContainerContext", 0);

        addMuleConfigurationRules(digester, path);
        addTransformerRules(digester, path);
        addSecurityManagerRules(digester, path);
        addTransactionManagerRules(digester, path);
        addGlobalEndpointRules(digester, path);
        addEndpointIdentifierRules(digester, path);
        addInterceptorStackRules(digester, path);
        addConnectorRules(digester, path);
        addAgentRules(digester, path);

        addModelRules(digester, path);
        // These rules allow for individual component configurations
        //RM* this is no longer required since Mule supports Multiple models and can inherit existing models.
        // This means that every mule-descriptor should be wrapped with a model element if its a stand alone
        //component config
        //addMuleDescriptorRules(digester, path);
    }

    public String getRootName()
    {
        return "mule-configuration";
    }

    public UMOManager configure(String configResources) throws ConfigurationException
    {
        return configure(configResources, null);
    }

    public UMOManager configure(String configResources, String startupPropertiesFile)
        throws ConfigurationException
    {
        try
        {
            String[] resources = StringUtils.splitAndTrim(configResources, ",");
            MuleManager.getConfiguration().setConfigResources(resources);
            ReaderResource[] readers = new ReaderResource[resources.length];
            for (int i = 0; i < resources.length; i++)
            {
                InputStream is = loadConfig(resources[i].trim());
                readers[i] = new ReaderResource(resources[i].trim(),
                    new InputStreamReader(is, configEncoding));
            }

            // Load startup properties if any.
            if (startupPropertiesFile != null)
            {
                return configure(readers, PropertiesUtils.loadProperties(startupPropertiesFile, getClass()));
            }
            else
                return configure(readers, null);

        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }

    /**
     * @deprecated Please use configure(ReaderResource[] configResources, Properties
     *             startupProperties) instead.
     */
    public UMOManager configure(ReaderResource[] configResources) throws ConfigurationException
    {
        return configure(configResources, null);
    }

    public UMOManager configure(ReaderResource[] configResources, Properties startupProperties)
        throws ConfigurationException
    {
        if (startupProperties != null)
        {
            ((MuleManager)MuleManager.getInstance()).addProperties(startupProperties);
        }
        manager = (MuleManager)process(configResources);
        if (manager == null)
        {
            throw new ConfigurationException(new Message(Messages.FAILED_TO_CREATE_MANAGER_INSTANCE_X,
                "Are you using a correct configuration builder?"));
        }
        try
        {
            setContainerProperties();
            setTransformers();
            setGlobalEndpoints();
            if (System.getProperty(MuleProperties.MULE_START_AFTER_CONFIG_SYSTEM_PROPERTY, "true")
                .equalsIgnoreCase("true"))
            {
                manager.start();
            }
        }
        catch (Exception e)
        {
            throw new ConfigurationException(new Message(Messages.X_FAILED_TO_INITIALISE, "MuleManager"), e);
        }
        return manager;
    }

    /**
     * Indicate whether this ConfigurationBulder has been configured yet
     *
     * @return <code>true</code> if this ConfigurationBulder has been configured
     */
    public boolean isConfigured()
    {
        return manager != null;
    }

    protected void setContainerProperties() throws ContainerException
    {
        UMOContainerContext ctx = manager.getContainerContext();
        try
        {
            for (Iterator iterator = containerReferences.iterator(); iterator.hasNext();)
            {
                ContainerReference reference = (ContainerReference)iterator.next();
                reference.resolveReference(ctx);
            }
        }
        finally
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
                TransformerReference reference = (TransformerReference)iterator.next();
                reference.resolveTransformer();
            }
        }
        finally
        {
            transformerReferences.clear();
        }
    }

    protected void setGlobalEndpoints() throws InitialisationException
    {
        // because Mule Xml allows developers to overload global endpoints
        // we need a way to initialise Global endpoints after the Xml has
        // been processed but before the MuleManager is initialised. So we do
        // it here.
        UMOManager manager = MuleManager.getInstance();

        // we need to take a copy of the endpoints since we're going to modify them
        // while iterating
        Map endpoints = new HashMap(manager.getEndpoints());
        for (Iterator iterator = endpoints.values().iterator(); iterator.hasNext();)
        {
            UMOEndpoint ep = (UMOEndpoint)iterator.next();
            ep.initialise();
            manager.unregisterEndpoint(ep.getName());
            manager.registerEndpoint(ep);
        }

        try
        {
            for (Iterator iterator = endpointReferences.iterator(); iterator.hasNext();)
            {
                EndpointReference reference = (EndpointReference)iterator.next();
                reference.resolveEndpoint();
            }
        }
        finally
        {
            endpointReferences.clear();
        }
    }

    protected void addManagerRules(Digester digester, String path)
    {
        digester.addFactoryCreate(path, new AbstractObjectCreationFactory()
        {
            public Object createObject(Attributes attributes) throws Exception
            {
                manager = MuleManager.getInstance();
                return manager;
            }
        });
        digester.addSetProperties(path);
    }

    protected void addMuleConfigurationRules(Digester digester, String path)
    {
        digester.addSetProperties(path);
        // Create mule system properties and defaults
        path += "/mule-environment-properties";
        digester.addObjectCreate(path, MuleConfiguration.class);
        addSetPropertiesRule(path, digester);

        // Add pooling profile rules
        addPoolingProfileRules(digester, path);

        // Add Queue Profile rules
        addQueueProfileRules(digester, path);

        // set threading profile
        digester.addObjectCreate(path + "/threading-profile", THREADING_PROFILE);
        SetPropertiesRule threadingRule = new SetPropertiesRule();
        threadingRule.addAlias("poolExhaustedAction", "poolExhaustedActionString");
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

                if ("default".equals(id))
                {
                    cfg.setDefaultThreadingProfile(tp);
                    cfg.setDefaultMessageDispatcherThreadingProfile(tp);
                    cfg.setDefaultMessageReceiverThreadingProfile(tp);
                    cfg.setDefaultComponentThreadingProfile(tp);
                }
                else if ("messageReceiver".equals(id) || "receiver".equals(id))
                {
                    cfg.setDefaultMessageReceiverThreadingProfile(tp);
                }
                else if ("messageDispatcher".equals(id) || "dispatcher".equals(id))
                {
                    cfg.setDefaultMessageDispatcherThreadingProfile(tp);
                }
                else if ("component".equals(id))
                {
                    cfg.setDefaultComponentThreadingProfile(tp);
                }
            }
        });

        // add persistence strategy
        digester.addObjectCreate(path + "/persistence-strategy", PERSISTENCE_STRATEGY_INTERFACE, "className");
        addMulePropertiesRule(path + "/persistence-strategy", digester);
        digester.addSetNext(path + "/persistence-strategy", "setPersistenceStrategy");

        // Connection strategy
        digester.addObjectCreate(path + "/connection-strategy", CONNECTION_STRATEGY_INTERFACE, "className");
        addMulePropertiesRule(path + "/connection-strategy", digester);
        // digester.addSetNext(path + "/connection-strategy",
        // "setConnectionStrategy");
        digester.addRule(path + "/connection-strategy", new SetNextRule("setConnectionStrategy")
        {
            public void end(String s, String s1) throws Exception
            {
                super.end(s, s1);
            }
        });

        digester.addRule(path, new Rule()
        {
            public void end(String s, String s1) throws Exception
            {
                MuleManager.setConfiguration((MuleConfiguration)digester.peek());
            }
        });
    }

    protected void addSecurityManagerRules(Digester digester, String path) throws ConfigurationException
    {
        // Create container Context
        path += "/security-manager";
        addObjectCreateOrGetFromContainer(path, DEFAULT_SECURITY_MANAGER, "className", "ref", false);

        // Add propviders
        digester.addObjectCreate(path + "/security-provider", SECURITY_PROVIDER_INTERFACE, "className");
        addSetPropertiesRule(path + "/security-provider", digester);
        addMulePropertiesRule(path + "/security-provider", digester);
        digester.addSetNext(path + "/security-provider", "addProvider");

        // Add encryption strategies
        digester.addObjectCreate(path + "/encryption-strategy", ENCRYPTION_STRATEGY_INTERFACE, "className");
        addSetPropertiesRule(path + "/encryption-strategy", digester);
        addMulePropertiesRule(path + "/encryption-strategy", digester);
        digester.addRule(path + "/encryption-strategy", new Rule()
        {
            private String name;

            public void begin(String endpointName, String endpointName1, Attributes attributes)
                throws Exception
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

    protected void addTransformerRules(Digester digester, String path) throws ConfigurationException
    {
        // Create Transformers
        path += "/transformers/transformer";
        addObjectCreateOrGetFromContainer(path, TRANSFORMER_INTERFACE, "className", "ref", true);

        addSetPropertiesRule(path, digester);

        addMulePropertiesRule(path, digester);
        digester.addSetRoot(path, "registerTransformer");
    }

    protected void addGlobalEndpointRules(Digester digester, String path) throws ConfigurationException
    {
        // Create global message endpoints
        path += "/global-endpoints";
        addEndpointRules(digester, path, "registerEndpoint");
    }

    protected void addEndpointIdentifierRules(Digester digester, String path) throws ConfigurationException
    {
        // Create and reqister endpoints
        path += "/endpoint-identifiers/endpoint-identifier";
        digester.addRule(path, new Rule()
        {
            private PlaceholderProcessor processor = new PlaceholderProcessor();

            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                attributes = processor.processAttributes(attributes, s1);
                String name = attributes.getValue("name");
                String value = attributes.getValue("value");
                ((UMOManager)digester.getRoot()).registerEndpointIdentifier(name, value);
            }
        });
    }

    protected void addTransactionManagerRules(Digester digester, String path) throws ConfigurationException
    {
        // Create transactionManager
        path += "/transaction-manager";
        addObjectCreateOrGetFromContainer(path, TRANSACTION_MANAGER_FACTORY_INTERFACE, "factory", "ref", true);
        addMulePropertiesRule(path, digester);

        digester.addSetRoot(path, "setTransactionManager");
        digester.addRule(path, new Rule()
        {
            public void end(String s, String s1) throws Exception
            {
                UMOTransactionManagerFactory txFactory = (UMOTransactionManagerFactory)digester.pop();
                digester.push(txFactory.create());
            }
        });
    }

    protected void addAgentRules(Digester digester, String path) throws ConfigurationException
    {
        // Create Agents
        path += "/agents/agent";
        addObjectCreateOrGetFromContainer(path, AGENT_INTERFACE, "className", "ref", true);
        addSetPropertiesRule(path, digester);

        addMulePropertiesRule(path, digester);

        digester.addSetRoot(path, "registerAgent");
    }

    protected void addConnectorRules(Digester digester, String path) throws ConfigurationException
    {
        // Create connectors
        path += "/connector";
        addObjectCreateOrGetFromContainer(path, CONNECTOR_INTERFACE, "className", "ref", true);

        addSetPropertiesRule(path, digester);

        addMulePropertiesRule(path, digester);

        digester.addRule(path + "/threading-profile", new Rule()
        {
            private String id;

            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // use the global tp as a template
                MuleConfiguration cfg = MuleManager.getConfiguration();
                id = attributes.getValue("id");
                if ("default".equals(id))
                {
                    digester.push(cfg.getDefaultThreadingProfile());
                }
                else if ("receiver".equals(id))
                {
                    digester.push(cfg.getDefaultMessageReceiverThreadingProfile());
                }
                else if ("dispatcher".equals(id))
                {
                    digester.push(cfg.getDefaultMessageDispatcherThreadingProfile());
                }

            }

            public void end(String s, String s1) throws Exception
            {
                ThreadingProfile tp = (ThreadingProfile)digester.pop();
                AbstractConnector cnn = (AbstractConnector)digester.peek();

                if ("default".equals(id))
                {
                    cnn.setReceiverThreadingProfile(tp);
                    cnn.setDispatcherThreadingProfile(tp);
                }
                else if ("receiver".equals(id))
                {
                    cnn.setReceiverThreadingProfile(tp);
                }
                else if ("dispatcher".equals(id))
                {
                    cnn.setDispatcherThreadingProfile(tp);
                }
            }
        });

        SetPropertiesRule threadingRule = new SetPropertiesRule();
        threadingRule.addAlias("setPoolExhaustedAction", "setPoolExhaustedActionString");
        digester.addRule(path + "/threading-profile", threadingRule);

        // Connection strategy
        digester.addObjectCreate(path + "/connection-strategy", CONNECTION_STRATEGY_INTERFACE, "className");
        addMulePropertiesRule(path + "/connection-strategy", digester);
        digester.addSetNext(path + "/connection-strategy", "setConnectionStrategy");

        addExceptionStrategyRules(digester, path);

        // register conntector
        digester.addSetRoot(path, "registerConnector");
    }

    protected void addInterceptorStackRules(Digester digester, String path) throws ConfigurationException
    {
        // Create Inteceptor stacks
        path += "/interceptor-stack";
        digester.addRule(path + "/interceptor", new ObjectCreateRule(INTERCEPTOR_INTERFACE, "className")
        {
            public void end(String s, String s1) throws Exception
            {
                /* do not pop the result */
            }
        });

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
                InterceptorStack stack = new InterceptorStack();
                stack.setInterceptors(list);
                manager.registerInterceptorStack(digester.pop().toString(), stack);
            }
        });

        addMulePropertiesRule(path + "/interceptor", digester);
    }

    protected void addModelRules(Digester digester, String path) throws ConfigurationException
    {
        // Create Model
        path += "/model";

        digester.addRule(path, new Rule()
        {
            public void begin(String string, String string1, Attributes attributes) throws Exception
            {
                UMOModel model;
                String modelType = attributes.getValue("type");
                String modelName = attributes.getValue("name");
                if (modelType == null)
                {
                    logger.debug("Model type not set, defaulting to SEDA");
                    modelType = "seda";
                }
                if (modelType.equalsIgnoreCase("custom"))
                {
                    String className = attributes.getValue("className");
                    if (className == null)
                    {
                        throw new IllegalArgumentException(
                            "Cannot use 'custom' model type without setting the 'className' for the model");
                    }
                    else
                    {
                        model = (UMOModel)ClassUtils.instanciateClass(className, ClassUtils.NO_ARGS,
                            getClass());
                    }
                }
                else if (modelType.equalsIgnoreCase("inherited"))
                {
                    Map models = MuleManager.getInstance().getModels();
                    if(models.size()==0)
                    {
                        throw new IllegalArgumentException("When using model inheritance there must be one model registered with Mule");
                    }
                    model = (UMOModel)models.get(modelName);
                    if(model == null)
                    {
                        throw new IllegalArgumentException("Cannot inherit from model '" + modelName + "'. No such model registered");
                    }
                }
                else
                {
                    model = ModelFactory.createModel(modelType);
                }

                digester.push(model);
            }
        });

        addSetPropertiesRule(path, digester);

        digester.addSetRoot(path, "registerModel");

        // Create endpointUri resolver
        digester.addObjectCreate(path + "/entry-point-resolver", DEFAULT_ENTRY_POINT_RESOLVER, "className");
        addSetPropertiesRule(path + "/entry-point-resolver", digester);

        digester.addSetNext(path + "/entry-point-resolver", "setEntryPointResolver");

        // Create lifecycle adapter
        digester.addObjectCreate(path + "/component-lifecycle-adapter-factory", DEFAULT_LIFECYCLE_ADAPTER,
            "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path + "/component-lifecycle-adapter-factory", "setLifecycleAdapterFactory");

        // Pool factory
        addPoolingProfileRules(digester, path);

        // Exception strategy
        addExceptionStrategyRules(digester, path);

        // Add Components
        addMuleDescriptorRules(digester, path);
    }

    protected void addMuleDescriptorRules(Digester digester, String path) throws ConfigurationException
    {
        // Create Mule UMOs
        path += "/mule-descriptor";
        addObjectCreateOrGetFromContainer(path, DEFAULT_DESCRIPTOR, "className", "ref", "container", false);

        addSetPropertiesRule(path, digester);

        // Create Message Routers
        addMessageRouterRules(digester, path, "inbound");
        addMessageRouterRules(digester, path, "outbound");
        addMessageRouterRules(digester, path, "nested");
        addMessageRouterRules(digester, path, "response");

        // Add threading profile rules
        addThreadingProfileRules(digester, path, "component");

        // Add pooling profile rules
        addPoolingProfileRules(digester, path);

        // queue profile rules
        addQueueProfileRules(digester, path);

        // Create interceptors
        digester.addRule(path + "/interceptor", new Rule()
        {
            public void begin(String string, String string1, Attributes attributes) throws Exception
            {
                String value = attributes.getValue("name");
                if (value == null)
                {
                    value = attributes.getValue("className");
                }
                UMOManager man = (UMOManager)digester.getRoot();
                UMOInterceptorStack interceptorStack = man.lookupInterceptorStack(value);
                MuleDescriptor temp = (MuleDescriptor)digester.peek();
                if (interceptorStack != null)
                {
                    temp.addInterceptor(interceptorStack);
                }
                else
                {
                    // Instantiate the new object and push it on the context
                    // stack
                    Class clazz = digester.getClassLoader().loadClass(value);
                    Object instance = clazz.newInstance();
                    temp.addInterceptor((UMOInterceptor)instance);
                    digester.push(instance);
                }
            }

            public void end(String s, String s1) throws Exception
            {
                if (digester.peek() instanceof UMOInterceptor)
                {
                    digester.pop();
                }
            }
        });

        addMulePropertiesRule(path + "/interceptor", digester);

        // Set exception strategy
        addExceptionStrategyRules(digester, path);

        addMulePropertiesRule(path, digester, "setProperties");
        digester.addSetNext(path + "/properties", "setProperties");

        // register the component
        digester.addRule(path, new Rule()
        {
            public void end(String s, String s1) throws Exception
            {
                UMODescriptor descriptor = (UMODescriptor)digester.peek();
                Object obj = digester.peek(1);
                final UMOModel model = (UMOModel)obj;
                descriptor.setModelName(model.getName());
                model.registerComponent(descriptor);
            }
        });
    }

    protected void addThreadingProfileRules(Digester digester, String path, final String type)
    {
        // set threading profile
        digester.addRule(path + "/threading-profile", new Rule()
        {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // use the default as a template
                MuleConfiguration cfg = MuleManager.getConfiguration();
                if ("component".equals(type))
                {
                    digester.push(cfg.getDefaultComponentThreadingProfile());
                }
                else if ("messageReceiver".equals(type))
                {
                    digester.push(cfg.getDefaultComponentThreadingProfile());
                }
                else if ("messageDispatcher".equals(type))
                {
                    digester.push(cfg.getDefaultComponentThreadingProfile());
                }
                else
                {
                    digester.push(cfg.getDefaultThreadingProfile());
                }
            }

            public void end(String s, String s1) throws Exception
            {
                digester.pop();
            }
        });
        // set threading profile
        SetPropertiesRule threadingRule = new SetPropertiesRule();
        threadingRule.addAlias("setPoolExhaustedAction", "setPoolExhaustedActionString");
        digester.addRule(path + "/threading-profile", threadingRule);
        digester.addSetNext(path + "/threading-profile", "setThreadingProfile");
    }

    protected void addPoolingProfileRules(Digester digester, String path)
    {
        // set pooling profile
        digester.addRule(path + "/pooling-profile", new Rule()
        {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // use the default as a template
                MuleConfiguration cfg = MuleManager.getConfiguration();
                //RM* digester.push(cfg.getPoolingProfile());
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
        addSetPropertiesRule(path + "/queue-profile", digester);
        digester.addSetNext(path + "/queue-profile", "setQueueProfile");
    }

    protected void addMessageRouterRules(Digester digester, String path, String type)
        throws ConfigurationException
    {
        String defaultRouter;
        String setMethod;
        if ("inbound".equals(type))
        {
            defaultRouter = DEFAULT_INBOUND_ROUTER_COLLECTION;
            setMethod = "setInboundRouter";
            path += "/inbound-router";
            // Add endpoints for multiple inbound endpoints
            addEndpointRules(digester, path, "addEndpoint");
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        }
        else if ("response".equals(type))
        {
            defaultRouter = DEFAULT_RESPONSE_ROUTER_COLLECTION;
            setMethod = "setResponseRouter";
            path += "/response-router";
            // Add endpoints for multiple response endpoints i.e. replyTo
            // addresses
            addEndpointRules(digester, path, "addEndpoint");
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        }
        else if ("nested".equals(type)) {
			defaultRouter = DEFAULT_NESTED_ROUTER_COLLECTION;
			setMethod = "setNestedRouter";
			path += "/nested-router";

    	}
        else
        {
            defaultRouter = DEFAULT_OUTBOUND_ROUTER_COLLECTION;
            setMethod = "setOutboundRouter";
            path += "/outbound-router";
        }
        digester.addObjectCreate(path, defaultRouter, "className");
        addSetPropertiesRule(path, digester);

        // Add Catch All strategy
        digester.addObjectCreate(path + "/catch-all-strategy", DEFAULT_CATCH_ALL_STRATEGY, "className");
        addSetPropertiesRule(path + "/catch-all-strategy", digester);

        // Add endpointUri for catch-all strategy
        addEndpointRules(digester, path + "/catch-all-strategy", "setEndpoint");
        addGlobalReferenceEndpointRules(digester, path + "/catch-all-strategy", "setEndpoint");

        addMulePropertiesRule(path + "/catch-all-strategy", digester);
        digester.addSetNext(path + "/catch-all-strategy", "setCatchAllStrategy");

        // Add router rules
        addRouterRules(digester, path, type);

        // add the router to the descriptor
        digester.addSetNext(path, setMethod);
    }

    protected void addRouterRules(Digester digester, String path, final String type)
        throws ConfigurationException
    {
        if("nested".equals(type))
        {
            path += "/binding";
            digester.addObjectCreate(path, DEFAULT_NESTED_ROUTER);


        } else if ("inbound".equals(type))
        {
            path += "/router";
            digester.addObjectCreate(path, INBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        }
        else if ("response".equals(type))
        {
            path += "/router";
            digester.addObjectCreate(path, RESPONSE_MESSAGE_ROUTER_INTERFACE, "className");
        }
        else
        {
            path += "/router";
            digester.addObjectCreate(path, OUTBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        }

        addSetPropertiesRule(path, digester, new String[]{"enableCorrelation", "propertyExtractor"},
            new String[]{"enableCorrelationAsString", "propertyExtractorAsString"});
        addMulePropertiesRule(path, digester);
        if ("outbound".equals(type))
        {
            addEndpointRules(digester, path, "addEndpoint");
            addReplyToRules(digester, path);
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
            addTransactionConfigRules(path, digester);
        }
        else if("nested".equals(type))
        {
            //Right now only one endpoint can be set on a nested Router so call
            //setEndpoint not addEndpoint
            addEndpointRules(digester, path, "setEndpoint");
			addGlobalReferenceEndpointRules(digester, path, "setEndpoint");
        }
        addFilterRules(digester, path);

        // Set the router on the to the message router
        digester.addSetNext(path, "addRouter");
    }

    protected void addReplyToRules(Digester digester, String path) throws ConfigurationException
    {
        // Set message endpoint
        path += "/reply-to";
        digester.addRule(path, new Rule()
        {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                String replyTo = attributes.getValue("address");
                ((UMOOutboundRouter)digester.peek()).setReplyTo(replyTo);
            }
        });
    }

    protected void addEndpointRules(Digester digester, String path, String method)
        throws ConfigurationException
    {
        // Set message endpoint
        path += "/endpoint";
        addObjectCreateOrGetFromContainer(path, DEFAULT_ENDPOINT, "className", "ref", false);
        addCommonEndpointRules(digester, path, method);
    }

    protected void addGlobalReferenceEndpointRules(Digester digester, String path, final String method)
        throws ConfigurationException
    {
        // Set message endpoint
        path += "/global-endpoint";
        digester.addRule(path, new Rule()
        {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                String name = attributes.getValue("name");
                String address = attributes.getValue("address");
                String trans = attributes.getValue("transformers");
                String responseTrans = attributes.getValue("responseTransformers");
                String createConnector = attributes.getValue("createConnector");
                EndpointReference ref = new EndpointReference(method, name, address, trans, responseTrans,
                    createConnector, digester.peek());
                // TODO encoding
                // String encoding = attributes.getValue("encoding");
                digester.push(ref);
            }

            public void end(String endpointName, String endpointName1) throws Exception
            {
                endpointReferences.add(digester.pop());
            }
        });
        addCommonEndpointRules(digester, path, null);
    }

    protected void addCommonEndpointRules(Digester digester, String path, String method)
        throws ConfigurationException
    {
        addSetPropertiesRule(path, digester, new String[]{"address", "transformers", "responseTransformers",
            "createConnector"}, new String[]{"endpointURI", "transformer", "responseTransformer",
            "createConnectorAsString"});
        // TODO test
        addMulePropertiesRule(path, digester, "setProperties");
        addTransactionConfigRules(path, digester);

        addFilterRules(digester, path);
        if (method != null)
        {
            digester.addSetNext(path, method);
        }

        // Add security filter rules
        digester.addObjectCreate(path + "/security-filter", ENDPOINT_SECURITY_FILTER_INTERFACE, "className");

        addMulePropertiesRule(path + "/security-filter", digester);
        digester.addSetNext(path + "/security-filter", "setSecurityFilter");
    }

    protected void addTransactionConfigRules(String path, Digester digester)
    {
        digester.addObjectCreate(path + "/transaction", DEFAULT_TRANSACTION_CONFIG);
        addSetPropertiesRule(path + "/transaction", digester, new String[]{"action"},
            new String[]{"actionAsString"});

        digester.addObjectCreate(path + "/transaction/constraint", TRANSACTION_CONSTRAINT_INTERFACE,
            "className");
        addSetPropertiesRule(path + "/transaction/constraint", digester);

        digester.addSetNext(path + "/transaction/constraint", "setConstraint");
        digester.addSetNext(path + "/transaction", "setTransactionConfig");
    }

    protected void addExceptionStrategyRules(Digester digester, String path) throws ConfigurationException
    {
        path += "/exception-strategy";
        digester.addObjectCreate(path, EXCEPTION_STRATEGY_INTERFACE, "className");
        addMulePropertiesRule(path, digester);

        // Add endpoint rules
        addEndpointRules(digester, path, "addEndpoint");
        addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        digester.addSetNext(path, "setExceptionListener");
    }

    protected void addSetPropertiesRule(String path, Digester digester, String[] s1, String[] s2)
    {
        digester.addRule(path, new ExtendedMuleSetPropertiesRule(s1, s2));
    }

    protected void addSetPropertiesRule(String path, Digester digester)
    {
        digester.addRule(path, new ExtendedMuleSetPropertiesRule());
    }

    private void addTransformerReference(String propName, String transName, Object object)
    {
        transformerReferences.add(new TransformerReference(propName, transName, object));
    }

    private void addEndpointReference(String propName, String endpointName, Object object)
    {
        endpointReferences.add(new EndpointReference(propName, endpointName, null, null, null, null, object));
    }

    /**
     * this rule serves 2 functions - 1. Allows for late binding of certain types of
     * object, namely Transformers and endpoints that need to be set on objects once
     * the Manager configuration has been processed 2. Allows for template parameters
     * to be parse on the configuration file in the form of ${param-name}. These will
     * get resolved against properties set in the mule-properites element
     */
    public class ExtendedMuleSetPropertiesRule extends MuleSetPropertiesRule
    {
        public ExtendedMuleSetPropertiesRule()
        {
            super();
        }

        public ExtendedMuleSetPropertiesRule(PlaceholderProcessor processor)
        {
            super(processor);
        }

        public ExtendedMuleSetPropertiesRule(String[] strings, String[] strings1)
        {
            super(strings, strings1);
        }

        public ExtendedMuleSetPropertiesRule(String[] strings,
                                             String[] strings1,
                                             PlaceholderProcessor processor)
        {
            super(strings, strings1, processor);
        }

        public void begin(String s1, String s2, Attributes attributes) throws Exception
        {
            attributes = processor.processAttributes(attributes, s2);
            // Add transformer references that will be bound to their objects
            // once all configuration has bean read
            String transformerNames = attributes.getValue("transformer");
            if (transformerNames != null)
            {
                addTransformerReference("transformer", transformerNames, digester.peek());
            }
            transformerNames = attributes.getValue("transformers");
            if (transformerNames != null)
            {
                addTransformerReference("transformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("responseTransformers");
            if (transformerNames != null)
            {
                addTransformerReference("responseTransformer", transformerNames, digester.peek());
            }

            // transformerNames = attributes.getValue("responseTransformer");
            // if (transformerNames != null) {
            // addTransformerReference("responseTransformer", transformerNames,
            // digester.peek());
            // }

            transformerNames = attributes.getValue("inboundTransformer");
            if (transformerNames != null)
            {
                addTransformerReference("inboundTransformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("outboundTransformer");
            if (transformerNames != null)
            {
                addTransformerReference("outboundTransformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("responseTransformer");
            if (transformerNames != null)
            {
                addTransformerReference("responseTransformer", transformerNames, digester.peek());
            }

            // Special case handling of global endpoint refs on the
            // inboundEndpoint/
            // outboundendpoint attributes of the descriptor
            String endpoint = attributes.getValue("inboundEndpoint");
            if (endpoint != null)
            {
                Object o = manager.getEndpoints().get(endpoint);
                if (o != null)
                {
                    addEndpointReference("setInboundEndpoint", endpoint, digester.peek());
                }
            }

            endpoint = attributes.getValue("outboundEndpoint");
            if (endpoint != null)
            {
                Object o = manager.getEndpoints().get(endpoint);
                if (o != null)
                {
                    addEndpointReference("setOutboundEndpoint", endpoint, digester.peek());
                }
            }
            super.begin(attributes);
        }
    }

    protected void addObjectCreateOrGetFromContainer(final String path,
                                                     String defaultImpl,
                                                     final String classAttrib,
                                                     final String refAttrib,
                                                     final boolean classRefRequired)
    {
        digester.addRule(path, new ObjectGetOrCreateRule(defaultImpl, classAttrib, refAttrib, classAttrib,
            classRefRequired, "getContainerContext"));
    }

    protected void addObjectCreateOrGetFromContainer(final String path,
                                                     String defaultImpl,
                                                     final String classAttrib,
                                                     final String refAttrib,
                                                     final String containerAttrib,
                                                     final boolean classRefRequired)
    {
        digester.addRule(path, new ObjectGetOrCreateRule(defaultImpl, classAttrib, refAttrib,
            containerAttrib, classAttrib, classRefRequired, "getContainerContext"));
    }
}
