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

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectCreateRule;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.SetNextRule;
import org.apache.commons.digester.SetPropertiesRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleDtdResolver;
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
import org.mule.impl.MuleComponentFactory;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleModel;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.interceptors.InterceptorStack;
import org.mule.model.DynamicEntryPointResolver;
import org.mule.providers.AbstractConnector;
import org.mule.providers.ConnectionStrategy;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.routing.response.ResponseMessageRouter;
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
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.PropertiesHelper;
import org.mule.util.queue.EventFilePersistenceStrategy;
import org.xml.sax.Attributes;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>MuleXmlConfigurationBuilder</code> is a configuration parser that
 * builds a MuleManager instance based on a mule xml configration file defined
 * in the mule-configuration.dtd.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleXmlConfigurationBuilder extends AbstractDigesterConfiguration implements ConfigurationBuilder
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleXmlConfigurationBuilder.class);

    public static final String DEFAULT_ENTRY_POINT_RESOLVER = DynamicEntryPointResolver.class.getName();
    public static final String DEFAULT_LIFECYCLE_ADAPTER = DefaultLifecycleAdapter.class.getName();
    public static final String DEFAULT_COMPONENT_FACTORY = MuleComponentFactory.class.getName();
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

    public static final String PERSISTENCE_STRATEGY_INTERFACE = EventFilePersistenceStrategy.class.getName();
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
    public static final String EXCEPTION_STRATEGY_INTERFACE = ExceptionListener.class.getName();
    public static final String CONNECTION_STRATEGY_INTERFACE = ConnectionStrategy.class.getName();

    protected UMOManager manager;

    private List transformerReferences = new ArrayList();
    private List endpointReferences = new ArrayList();

    public MuleXmlConfigurationBuilder() throws ConfigurationException
    {
        super(System.getProperty("org.mule.xml.validate", "true").equalsIgnoreCase("true"),
                System.getProperty("org.mule.xml.dtd", MuleDtdResolver.DEFAULT_MULE_DTD));

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
        // Threse rules allow for individual component configurations
        addMuleDescriptorRules(digester, path);
    }

    public String getRootName() {
        return "mule-configuration";
    }

    public UMOManager configure(String configResources) throws ConfigurationException
    {
        return configure(parseResources(configResources));
    }

    public UMOManager configure(ReaderResource[] configResources) throws ConfigurationException
    {
        manager = (MuleManager)process(configResources);
        try {
            setContainerProperties();
            setTransformers();
            setGlobalEndpoints();
            manager.start();
        } catch (Exception e) {
            throw new ConfigurationException(new Message(Messages.X_FAILED_TO_INITIALISE, "MuleManager"), e);
        }
        return manager;
    }

    /**
     * Indicate whether this ConfigurationBulder has been configured yet
     * 
     * @return <code>true</code> if this ConfigurationBulder has been
     *         configured
     */
    public boolean isConfigured()
    {
        return manager != null;
    }

    protected void setContainerProperties() throws ContainerException
    {
        UMOContainerContext ctx = manager.getContainerContext();
        try {
            for (Iterator iterator = containerReferences.iterator(); iterator.hasNext();) {
                ContainerReference reference = (ContainerReference) iterator.next();
                reference.resolveReference(ctx);
            }
        } finally {
            containerReferences.clear();
        }
    }

    protected void setTransformers() throws InitialisationException
    {
        try {
            for (Iterator iterator = transformerReferences.iterator(); iterator.hasNext();) {
                TransformerReference reference = (TransformerReference) iterator.next();
                reference.resolveTransformer();
            }
        } finally {
            transformerReferences.clear();
        }
    }

    protected void setGlobalEndpoints() throws InitialisationException
    {
        // because Mule Xml allows developers to overload global endpoints
        // we need a way to initialise Global endpoints after the Xml has
        // been processed but before the MuleManager is initialised. So we do
        // it here
        Map endpoints = MuleManager.getInstance().getEndpoints();
        UMOEndpoint ep;
        for (Iterator iterator = endpoints.values().iterator(); iterator.hasNext();) {
            ep = (UMOEndpoint) iterator.next();
            ep.initialise();
            MuleManager.getInstance().unregisterEndpoint(ep.getName());
            MuleManager.getInstance().registerEndpoint(ep);
        }

        try {
            for (Iterator iterator = endpointReferences.iterator(); iterator.hasNext();) {
                EndpointReference reference = (EndpointReference) iterator.next();
                reference.resolveEndpoint();
            }
        } finally {
            endpointReferences.clear();
        }
    }

    protected void addManagerRules(Digester digester, String path)
    {
        digester.addFactoryCreate(path, new AbstractObjectCreationFactory(){
            public Object createObject(Attributes attributes) throws Exception {
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
        threadingRule.addAlias("setPoolExhaustedAction", "setPoolExhaustedActionString");
        digester.addRule(path + "/threading-profile", threadingRule);
        digester.addRule(path + "/threading-profile", new Rule() {
            private String id;

            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                id = attributes.getValue("id");
            }

            public void end(String s, String s1) throws Exception
            {
                ThreadingProfile tp = (ThreadingProfile) digester.peek();
                MuleConfiguration cfg = (MuleConfiguration) digester.peek(1);

                if ("default".equals(id)) {
                    cfg.setDefaultThreadingProfile(tp);
                    cfg.setMessageDispatcherThreadingProfile(tp);
                    cfg.setMessageReceiverThreadingProfile(tp);
                    cfg.setComponentThreadingProfile(tp);
                } else if ("messageReceiver".equals(id) || "receiver".equals(id)) {
                    cfg.setMessageReceiverThreadingProfile(tp);
                } else if ("messageDispatcher".equals(id) || "dispatcher".equals(id)) {
                    cfg.setMessageDispatcherThreadingProfile(tp);
                } else if ("component".equals(id)) {
                    cfg.setComponentThreadingProfile(tp);
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
        //digester.addSetNext(path + "/connection-strategy", "setConnectionStrategy");
        digester.addRule(path + "/connection-strategy",new SetNextRule("setConnectionStrategy"){
            public void end(String s, String s1) throws Exception {
                super.end(s, s1);
            }
        });

        digester.addRule(path, new Rule() {
            public void end(String s, String s1) throws Exception
            {
                MuleManager.setConfiguration((MuleConfiguration) digester.peek());
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
        digester.addRule(path + "/encryption-strategy", new Rule() {
            private String name;

            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception
            {
                name = attributes.getValue("name");
            }

            public void end(String endpointName, String endpointName1) throws Exception
            {
                UMOEncryptionStrategy s = (UMOEncryptionStrategy) digester.peek();
                ((UMOSecurityManager) digester.peek(1)).addEncryptionStrategy(name, s);
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
        digester.addCallMethod(path, "registerEndpointIdentifier", 2);
        digester.addCallParam(path, 0, "name");
        digester.addCallParam(path, 1, "value");
    }

    protected void addTransactionManagerRules(Digester digester, String path) throws ConfigurationException
    {
        // Create transactionManager
        path += "/transaction-manager";
        addObjectCreateOrGetFromContainer(path, TRANSACTION_MANAGER_FACTORY_INTERFACE, "factory", "ref", true);
        addMulePropertiesRule(path, digester);

        digester.addSetRoot(path, "setTransactionManager");
        digester.addRule(path, new Rule() {
            public void end(String s, String s1) throws Exception
            {
                UMOTransactionManagerFactory txFactory = (UMOTransactionManagerFactory) digester.pop();
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

        digester.addRule(path + "/threading-profile", new Rule() {
            private String id;

            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // use the global tp as a template
                MuleConfiguration cfg = MuleManager.getConfiguration();
                id = attributes.getValue("id");
                if ("default".equals(id)) {
                    digester.push(cfg.getDefaultThreadingProfile());
                } else if ("receiver".equals(id)) {
                    digester.push(cfg.getMessageReceiverThreadingProfile());
                } else if ("dispatcher".equals(id)) {
                    digester.push(cfg.getMessageDispatcherThreadingProfile());
                }

            }

            public void end(String s, String s1) throws Exception
            {
                ThreadingProfile tp = (ThreadingProfile) digester.pop();
                AbstractConnector cnn = (AbstractConnector) digester.peek();

                if ("default".equals(id)) {
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
        digester.addRule(path + "/interceptor", new ObjectCreateRule(INTERCEPTOR_INTERFACE, "className") {
            public void end(String s, String s1) throws Exception
            {
                /* do not pop the result */
            }
        });

        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                digester.push(attributes.getValue("name"));
            }

            public void end(String s, String s1) throws Exception
            {
                List list = new ArrayList();
                Object obj = digester.peek();
                while (obj instanceof UMOInterceptor) {
                    list.add(0, digester.pop());
                    obj = digester.peek();
                }
                InterceptorStack stack = new InterceptorStack();
                stack.setInterceptors(list);
                manager.registerInterceptorStack(digester.pop().toString(), stack);
            }
        });
    }

    protected void addModelRules(Digester digester, String path) throws ConfigurationException
    {
        // Create Model
        path += "/model";
        addObjectCreateOrGetFromContainer(path, DEFAULT_MODEL, "className", "ref", false);

        addSetPropertiesRule(path, digester);

        digester.addSetRoot(path, "setModel");

        // Create endpointUri resolver
        digester.addObjectCreate(path + "/entry-point-resolver", DEFAULT_ENTRY_POINT_RESOLVER, "className");
        addSetPropertiesRule(path + "/entry-point-resolver", digester);

        digester.addSetNext(path + "/entry-point-resolver", "setEntryPointResolver");

        // Create lifecycle adapter
        digester.addObjectCreate(path + "/component-lifecycle-adapter-factory", DEFAULT_LIFECYCLE_ADAPTER, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path + "/component-lifecycle-adapter-factory", "setLifecycleAdapterFactory");

        // Create component factory
        digester.addObjectCreate(path + "/component-factory", DEFAULT_COMPONENT_FACTORY, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path + "/component-factory", "setComponentFactory");

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
        addObjectCreateOrGetFromContainer(path, DEFAULT_DESCRIPTOR, "className", "ref", false);

        addSetPropertiesRule(path, digester);

        // Create Message Routers
        addMessageRouterRules(digester, path, "inbound");
        addMessageRouterRules(digester, path, "outbound");
        addMessageRouterRules(digester, path, "response");

        // Add threading profile rules
        addThreadingProfileRules(digester, path, "component");

        // Add pooling profile rules
        addPoolingProfileRules(digester, path);

        // queue profile rules
        addQueueProfileRules(digester, path);

        // Create interceptors
        digester.addRule(path + "/interceptor", new Rule() {
            public void begin(String string, String string1, Attributes attributes) throws Exception
            {
                String value = attributes.getValue("name");
				if (value == null) {
					value = attributes.getValue("className");
				}
                UMOManager man = (UMOManager) digester.getRoot();
                UMOInterceptorStack interceptorStack = man.lookupInterceptorStack(value);
                MuleDescriptor temp = (MuleDescriptor) digester.peek();
                if (interceptorStack != null) {
                    temp.addInterceptor(interceptorStack);
                } else {
                    // Instantiate the new object and push it on the context
                    // stack
                    Class clazz = digester.getClassLoader().loadClass(value);
                    Object instance = clazz.newInstance();
                    temp.addInterceptor((UMOInterceptor) instance);
                    digester.push(instance);
                }
            }

            public void end(String s, String s1) throws Exception
            {
                if (digester.peek() instanceof UMOInterceptor) {
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
        digester.addRule(path, new Rule() {
            public void end(String s, String s1) throws Exception
            {
                UMODescriptor descriptor = (UMODescriptor) digester.peek();
                Object obj = digester.peek(1);
                final UMOModel model;
                if (obj instanceof UMOManager) {
                    model = ((UMOManager) obj).getModel();
                } else {
                    model = (UMOModel) obj;
                }
                model.registerComponent(descriptor);
            }
        });
    }

    protected void addThreadingProfileRules(Digester digester, String path, final String type)
    {
        // set threading profile
        digester.addRule(path + "/threading-profile", new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // use the default as a template
                MuleConfiguration cfg = MuleManager.getConfiguration();
                if ("component".equals(type)) {
                    digester.push(cfg.getComponentThreadingProfile());
                } else if ("messageReceiver".equals(type)) {
                    digester.push(cfg.getComponentThreadingProfile());
                } else if ("messageDispatcher".equals(type)) {
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
        // set threading profile
        SetPropertiesRule threadingRule = new SetPropertiesRule();
        threadingRule.addAlias("setPoolExhaustedAction", "setPoolExhaustedActionString");
        digester.addRule(path + "/threading-profile", threadingRule);
        digester.addSetNext(path + "/threading-profile", "setThreadingProfile");
    }

    protected void addPoolingProfileRules(Digester digester, String path)
    {
        // set pooling profile
        digester.addRule(path + "/pooling-profile", new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // use the default as a template
                MuleConfiguration cfg = MuleManager.getConfiguration();
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
        addSetPropertiesRule(path + "/queue-profile", digester);
        digester.addSetNext(path + "/queue-profile", "setQueueProfile");
    }

    protected void addMessageRouterRules(Digester digester, String path, String type) throws ConfigurationException
    {
        String defaultRouter = null;
        String setMethod = null;
        if ("inbound".equals(type)) {
            defaultRouter = DEFAULT_INBOUND_MESSAGE_ROUTER;
            setMethod = "setInboundRouter";
            path += "/inbound-router";
            // Add endpoints for multiple inbound endpoints
            addEndpointRules(digester, path, "addEndpoint");
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        } else if ("response".equals(type)) {
            defaultRouter = DEFAULT_RESPONSE_MESSAGE_ROUTER;
            setMethod = "setResponseRouter";
            path += "/response-router";
            // Add endpoints for multiple response endpoints i.e. replyTo
            // addresses
            addEndpointRules(digester, path, "addEndpoint");
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
        } else {
            defaultRouter = DEFAULT_OUTBOUND_MESSAGE_ROUTER;
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

    protected void addRouterRules(Digester digester, String path, final String type) throws ConfigurationException
    {
        path += "/router";
        if ("inbound".equals(type)) {
            digester.addObjectCreate(path, INBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        } else if ("response".equals(type)) {
            digester.addObjectCreate(path, RESPONSE_MESSAGE_ROUTER_INTERFACE, "className");
        } else {
            digester.addObjectCreate(path, OUTBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        }

        addSetPropertiesRule(path,
                             digester,
                             new String[] { "enableCorrelation" },
                             new String[] { "enableCorrelationAsString" });
        addMulePropertiesRule(path, digester);
        if ("outbound".equals(type)) {
            addEndpointRules(digester, path, "addEndpoint");
            addReplyToRules(digester, path);
            addGlobalReferenceEndpointRules(digester, path, "addEndpoint");
            addTransactionConfigRules(path, digester);
        }
        addFilterRules(digester, path);

        // Set the router on the to the message router
        digester.addSetNext(path, "addRouter");
    }

    protected void addReplyToRules(Digester digester, String path) throws ConfigurationException
    {
        // Set message endpoint
        path += "/reply-to";
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception {
                String replyTo = attributes.getValue("address");
                ((UMOOutboundRouter)digester.peek()).setReplyTo(replyTo);
            }
        });
    }

    protected void addEndpointRules(Digester digester, String path, String method) throws ConfigurationException
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
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                String name = attributes.getValue("name");
                String address = attributes.getValue("address");
                String trans = attributes.getValue("transformers");
                String createConnector = attributes.getValue("createConnector");
                EndpointReference ref = new EndpointReference(method,
                                                              name,
                                                              address,
                                                              trans,
                                                              createConnector,
                                                              digester.peek());
                digester.push(ref);
            }

            public void end(String endpointName, String endpointName1) throws Exception
            {
                endpointReferences.add(digester.pop());
            }
        });
        addCommonEndpointRules(digester, path, null);
    }

    protected void addCommonEndpointRules(Digester digester, String path, String method) throws ConfigurationException
    {
        addSetPropertiesRule(path,
                             digester,
                             new String[] { "address", "transformers", "createConnector" },
                             new String[] { "endpointURI", "transformer", "createConnectorAsString" });
        //todo test
        addMulePropertiesRule(path, digester, "setProperties");
        addTransactionConfigRules(path, digester);

        addFilterRules(digester, path);
        if (method != null) {
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
        addSetPropertiesRule(path + "/transaction",
                             digester,
                             new String[] { "action" },
                             new String[] { "actionAsString" });

        digester.addObjectCreate(path + "/transaction/constraint", TRANSACTION_CONSTRAINT_INTERFACE, "className");
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



    private void addContainerReference(String propName,
                                       String containerRef,
                                       Object object,
                                       boolean required,
                                       String container)
    {
        containerReferences.add(new ContainerReference(propName, containerRef, object, required, container));
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
        endpointReferences.add(new EndpointReference(propName, endpointName, null, null, null, object));
    }

    /**
     * this rule serves 2 functions - 1. Allows for late binding of certain
     * types of object, namely Transformers and endpoints that need to be set on
     * objects once the Manager configuration has been processed 2. Allows for
     * template parameters to be parse on the configuration file in the form of
     * ${param-name}. These will get resolved against properties set in the
     * mule-properites element
     */
    public class ExtendedMuleSetPropertiesRule extends MuleSetPropertiesRule
    {
        public ExtendedMuleSetPropertiesRule() {
        }

        public ExtendedMuleSetPropertiesRule(PlaceholderProcessor processor) {
            super(processor);
        }

        public ExtendedMuleSetPropertiesRule(String[] strings, String[] strings1) {
            super(strings, strings1);
        }

        public ExtendedMuleSetPropertiesRule(String[] strings, String[] strings1, PlaceholderProcessor processor) {
            super(strings, strings1, processor);
        }

        public void begin(String s1, String s2, Attributes attributes) throws Exception
        {
            attributes = processor.processAttributes(attributes, s2);
            // Add transformer references that will be bound to their objects
            // once
            // all configuration has bean read
            String transformerNames = attributes.getValue("transformer");
            if (transformerNames != null) {
                addTransformerReference("transformer", transformerNames, digester.peek());
            }
            transformerNames = attributes.getValue("transformers");
            if (transformerNames != null) {
                addTransformerReference("transformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("inboundTransformer");
            if (transformerNames != null) {
                addTransformerReference("inboundTransformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("outboundTransformer");
            if (transformerNames != null) {
                addTransformerReference("outboundTransformer", transformerNames, digester.peek());
            }

            transformerNames = attributes.getValue("responseTransformer");
            if (transformerNames != null) {
                addTransformerReference("responseTransformer", transformerNames, digester.peek());
            }

            // Special case handling of global endpoint refs on the
            // inboundEndpoint/
            // outboundendpoint attributes of the descriptor
            String endpoint = attributes.getValue("inboundEndpoint");
            if (endpoint != null) {
                Object o = PropertiesHelper.getProperty(manager.getEndpoints(), endpoint, null);
                if (o != null) {
                    addEndpointReference("setInboundEndpoint", endpoint, digester.peek());
                }
            }

            endpoint = attributes.getValue("outboundEndpoint");
            if (endpoint != null) {
                Object o = PropertiesHelper.getProperty(manager.getEndpoints(), endpoint, null);
                if (o != null) {
                    addEndpointReference("setOutboundEndpoint", endpoint, digester.peek());
                }
            }
            super.begin(attributes);
        }
    }

    protected void addObjectCreateOrGetFromContainer(final String path, String defaultImpl, final String classAttrib, final String refAttrib, final boolean classRefRequired) {
        digester.addRule(path, new ObjectGetOrCreateRule(
                defaultImpl, classAttrib, refAttrib, classAttrib,
                classRefRequired, "getContainerContext"));
    }
}
