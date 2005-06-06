package org.mule.extras.spring.config;

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

import java.beans.ExceptionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;
import org.mule.extras.spring.SpringContainerContext;
import org.mule.impl.MuleModel;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.UMOLifecycleAdapterFactory;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOTransactionManagerFactory;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.transformer.UMOTransformer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * <code>UMOManagerFactoryBean</code> is a MuleManager factory bean that is
 * used to configure the MuleManager from a spring context. This factory bean is
 * responsible for determining the instance type of UMOManager to create and
 * then delegates configuration calls to that instance depending on what is
 * available in the container. <p/> Apart from removing the need to explicitly
 * wire the MuleManager instance together there another advantage to using the
 * AutowireUMOManagerFactoryBean. There is no need to declare a UMOModel
 * instance in the configuration. If the factory doesn't find a UMOModel
 * implementation it creates a default one of type
 * <i>org.mule.impl.model.MuleModel</i>. The model is automatically initialised
 * with a SpringContainercontext using the current beanFactory and defaults are
 * used for the other Model properties. If you want to override the defaults,
 * such as define your own exception strategy, (which you will most likely want
 * to do) simply declare your exception strategy bean in the container and it
 * will automatically be set on the model. <p/> Most Mule objects have explicit
 * types and can be autowired, however some objects cannot be autowired, such as
 * a <i>java.util.Map</i> of endpoints for example. For these objects Mule
 * defines standard bean names that will be looked for in the container during
 * start up. <p/> muleEnvironmentProperties A map of properties to set on the
 * MuleManager. Accessible from your code using
 * AutowireUMOManagerFactoryBean.MULE_ENVIRONMENT_PROPERTIES_BEAN_NAME. <p/>
 * muleEndpointMappings A Map of logical endpointUri mappings accessible from
 * your code using
 * AutowireUMOManagerFactoryBean.MULE_ENDPOINT_MAPPINGS_BEAN_NAME. <p/>
 * muleInterceptorStacks A map of interceptor stacks, where the name of the
 * stack is the key and a list of interceptors is the value. Accessible using
 * from your code using
 * AutowireUMOManagerFactoryBean.MULE_INTERCEPTOR_STACK_BEAN_NAME.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AutowireUMOManagerFactoryBean implements FactoryBean, InitializingBean, DisposableBean,
        ApplicationContextAware
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(AutowireUMOManagerFactoryBean.class);

    public static final String MULE_ENVIRONMENT_PROPERTIES_BEAN_NAME = "muleEnvironmentProperties";
    public static final String MULE_ENDPOINT_IDENTIFIERS_BEAN_NAME = "muleEndpointIdentifiers";
    public static final String MULE_INTERCEPTOR_STACK_BEAN_NAME = "muleInterceptorStacks";
    public static final String MULE_MODEL_EXCEPTION_STRATEGY_BEAN_NAME = "muleModelExceptionStrategy";

    private UMOManager manager;

    private AbstractApplicationContext context;

    public AutowireUMOManagerFactoryBean() throws Exception
    {
        this.manager = MuleManager.getInstance();
    }

    public Object getObject() throws Exception
    {
        return manager;
    }

    public Class getObjectType()
    {
        return UMOManager.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        context = (AbstractApplicationContext) applicationContext;
        try {
            // set mule configuration
            Map temp = context.getBeansOfType(MuleConfiguration.class, true, true);
            if (temp.size() > 0)
                MuleManager.setConfiguration((MuleConfiguration) temp.values().iterator().next());

            // set environment properties
            setProperties((Map) getBean(MULE_ENVIRONMENT_PROPERTIES_BEAN_NAME, Map.class));

            // set Connectors
            Map connectors = context.getBeansOfType(UMOConnector.class, true, true);
            setConnectors(connectors.values());

            // set endpoint Identifiers
            setMessageEndpointIdentifiers((Map) getBean(MULE_ENDPOINT_IDENTIFIERS_BEAN_NAME, Map.class));

            // set mule transaction manager
            temp = context.getBeansOfType(UMOTransactionManagerFactory.class, true, true);
            if (temp.size() > 0)
                manager.setTransactionManager(((UMOTransactionManagerFactory) temp.values().iterator().next()).create());

            // set security manager
            temp = context.getBeansOfType(UMOSecurityManager.class, true, true);
            if (temp.size() > 0)
                manager.setSecurityManager((UMOSecurityManager) temp.values().iterator().next());

            // set Transformers
            Map transformers = context.getBeansOfType(UMOTransformer.class, true, true);
            setTransformers(transformers.values());

            // set Endpoints
            Map endpoints = context.getBeansOfType(UMOEndpoint.class, true, true);
            setEndpoints(endpoints.values());

            // set Agents
            Map agents = context.getBeansOfType(UMOAgent.class, true, true);
            setAgents(agents.values());

            // Set the container Context
            Map containers = context.getBeansOfType(UMOContainerContext.class, true, true);
            setContainerContext(containers);

            // interceptors
            Map interceptors = context.getBeansOfType(UMOInterceptorStack.class, true, true);
            setInterceptorStacks(interceptors);
            // create the model
            createModel();

            // set Components
            Map components = context.getBeansOfType(UMODescriptor.class, true, true);
            setComponents(components.values());
        } catch (Exception e) {
            throw new BeanInitializationException("Failed to wire MuleManager together: " + e.getMessage(), e);
        }
    }

    protected void createModel()
    {
        // set the model
        Map temp = context.getBeansOfType(UMOModel.class, true, true);
        UMOModel model;
        if (temp.size() > 0) {
            Map.Entry entry = (Map.Entry) temp.entrySet().iterator().next();
            model = (UMOModel) entry.getValue();
            model.setName(entry.getKey().toString());
        } else {
            // create a defaultModel
            model = new MuleModel();
        }

        // autowire the model so any ExceptionStrategy or PoolingStrategy beans
        // can be set
        // context.getBeanFactory().autowireBeanProperties(model,
        // AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        // RM we cant autowire the model by type as some list properties
        // conflict with each other

        // Entry point resolver
        Map epr = context.getBeansOfType(UMOEntryPointResolver.class, true, true);
        if (epr.size() > 0)
            model.setEntryPointResolver((UMOEntryPointResolver) epr.values().iterator().next());

        // Life cycle adapter factory
        Map lcaf = context.getBeansOfType(UMOLifecycleAdapterFactory.class, true, true);
        if (lcaf.size() > 0)
            model.setLifecycleAdapterFactory((UMOLifecycleAdapterFactory) lcaf.values().iterator().next());

        // Model exception strategy
        Object listener = getBean(MULE_MODEL_EXCEPTION_STRATEGY_BEAN_NAME, ExceptionListener.class);
        if (listener != null)
            model.setExceptionListener((ExceptionListener) listener);

        manager.setModel(model);

    }

    private Object getBean(String name, Class clazz)
    {
        try {
            return context.getBean(name, clazz);
        } catch (BeansException e) {
            return null;
        }

    }

    protected void setContainerContext(Map containers) throws UMOException
    {
        if (containers.size() == 0) {
            // Use this as the default container
            SpringContainerContext container = new SpringContainerContext();
            container.setBeanFactory(context);
            manager.setContainerContext(container);
        } else if (containers.size() == 1) {
            manager.setContainerContext((UMOContainerContext) containers.values().iterator().next());
        } else {
            UMOContainerContext ctx = (UMOContainerContext) containers.values().iterator().next();
            logger.warn("There are " + containers.size()
                    + " container contexts in the spring context. Using the first one: " + ctx.getClass().getName());
            manager.setContainerContext(ctx);
        }
    }

    protected void setMessageEndpointIdentifiers(Map endpoints) throws InitialisationException
    {
        if (endpoints == null)
            return;
        Map.Entry entry;
        for (Iterator iterator = endpoints.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            manager.registerEndpointIdentifier(entry.getKey().toString(), entry.getValue().toString());

        }
    }

    protected void setAgents(Collection agents) throws UMOException
    {
        for (Iterator iterator = agents.iterator(); iterator.hasNext();) {
            manager.registerAgent((UMOAgent) iterator.next());
        }
    }

    protected void setProperties(Map props)
    {
        if (props == null)
            return;
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            manager.setProperty(entry.getKey(), entry.getValue());
        }
    }

    protected void setConnectors(Collection connectors) throws UMOException
    {
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();) {
            manager.registerConnector((UMOConnector) iterator.next());
        }
    }

    protected void setTransformers(Collection transformers) throws InitialisationException
    {
        for (Iterator iterator = transformers.iterator(); iterator.hasNext();) {
            manager.registerTransformer((UMOTransformer) iterator.next());
        }
    }

    protected void setEndpoints(Collection endpoints) throws InitialisationException
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();) {
            manager.registerEndpoint((UMOEndpoint) iterator.next());
        }
    }

    protected void setComponents(Collection components) throws UMOException
    {
        UMODescriptor d;
        for (Iterator iterator = components.iterator(); iterator.hasNext();) {
            d = (UMODescriptor) iterator.next();
            if (!manager.getModel().isComponentRegistered(d.getName())) {
                manager.getModel().registerComponent(d);
            }
        }
    }

    protected void setInterceptorStacks(Map stacks)
    {
        if (stacks == null)
            return;
        for (Iterator iterator = stacks.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String name = entry.getKey().toString();
            manager.registerInterceptorStack(name, (UMOInterceptorStack) entry.getValue());
        }
    }

    public void afterPropertiesSet() throws Exception
    {
        manager.start();
    }

    public void destroy() throws Exception
    {
        manager.dispose();
    }
}
