/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.RegistryContext;
import org.mule.impl.ManagementContext;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.model.ModelHelper;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOTransactionManagerFactory;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <code>UMOManagerFactoryBean</code> is a MuleManager factory bean that is used to
 * configure the MuleManager from a spring context. This factory bean is responsible
 * for determining the instance type of UMOManager to create and then delegates
 * configuration calls to that instance depending on what is available in the
 * container. <p/> Apart from removing the need to explicitly wire the MuleManager
 * instance together there another advantage to using the
 * AutowireUMOManagerFactoryBean. There is no need to declare a UMOModel instance in
 * the configuration. If the factory doesn't find a UMOModel implementation it
 * creates a default one of type <i>org.mule.impl.model.seda.SedaModel</i>. The
 * model is automatically initialised with a SpringContainercontext using the current
 * beanFactory and defaults are used for the other Model properties. If you want to
 * override the defaults, such as define your own exception strategy, (which you will
 * most likely want to do) simply declare your exception strategy bean in the
 * container and it will automatically be set on the model. <p/> Most Mule objects
 * have explicit types and can be autowired, however some objects cannot be
 * autowired, such as a <i>java.util.Map</i> of endpoints for example. For these
 * objects Mule defines standard bean names that will be looked for in the container
 * during start up. <p/> muleEnvironmentProperties A map of properties to set on the
 * MuleManager. Accessible from your code using
 * AutowireUMOManagerFactoryBean.MULE_ENVIRONMENT_PROPERTIES_BEAN_NAME. <p/>
 * muleEndpointMappings A Map of logical endpointUri mappings accessible from your
 * code using AutowireUMOManagerFactoryBean.MULE_ENDPOINT_MAPPINGS_BEAN_NAME. <p/>
 * muleInterceptorStacks A map of interceptor stacks, where the name of the stack is
 * the key and a list of interceptors is the value. Accessible using from your code
 * using AutowireUMOManagerFactoryBean.MULE_INTERCEPTOR_STACK_BEAN_NAME.
 */
public class ManagementContextFactoryBean extends AbstractFactoryBean
        implements ApplicationContextAware
{
    /**
     * logger used by this class
     */
    protected static Log logger = LogFactory.getLog(ManagementContextFactoryBean.class);

    protected UMOManagementContext managementContext = new ManagementContext();

    //TODO LM: Replace
    protected RegistryFacade registry;

    private ApplicationContext context;

    protected Object createInstance() throws Exception
    {
        if(managementContext==null)
        {
            this.managementContext = new ManagementContext();
        }
        return managementContext;
    }

    public Class getObjectType()
    {
        return UMOManagementContext.class;
    }

    protected UMOManagementContext getManagementContext()
    {
        return managementContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.context = applicationContext;
        //TODO where do I get this from
        //registry = new SpringRegistry();
        //Add the Spring Container context by default
        //SpringContainerContext container = new SpringContainerContext();
        //container.setBeanFactory(context);
        //container.setName("spring-registry");
//        if(registry==null)
//        {
//            Map m = applicationContext.getBeansOfType(RegistryFacade.class);
//            if(m.size() > 0)
//            {
//                registry = (RegistryFacade)m.values().iterator().next();
//            }
//            else
//            {
//                registry = createDefaultRegistry();
//
//                logger.debug("no registry has been defined in context. Created default Registry: " + registry.getClass().getName());
//            }
//        }
//        managementContext.setRegistry(registry);
        registry = RegistryContext.getRegistry();

//        try
//        {
//            registry.registerContainerContext(container);
//        }
//        catch (UMOException e)
//        {
//            throw new BeanCreationException("failed to register default spring container", e);
//        }

    }


    public void afterPropertiesSet() throws Exception
    {
        super.afterPropertiesSet();
        managementContext.initialise();        
        initialise();
    }

    protected RegistryFacade createDefaultRegistry()
    {
        return new SpringRegistry();
    }

    protected void initialise()
    {
        try
        {
            boolean legacy = false;

            // set mule configuration
//            Map temp = context.getBeansOfType(MuleConfiguration.class, true, false);
//            if (temp.size() > 0)
//            {
//               registry.setConfiguration((MuleConfiguration)temp.values().iterator().next());
//            }

            //Legacy handling.  If the context contains an AutowireUMOManagerFactoryBean, then we're dealing
            //with an old Mule config file and we change the way we deal with some of the components
            Map temp = context.getBeansOfType(AutowireUMOManagerFactoryBean.LegacyManager.class);
            if(temp.size() > 0)
            {
                legacy = true;
                registry.getConfiguration().setId(((AutowireUMOManagerFactoryBean.LegacyManager)temp.values().iterator().next()).getManagerId());
                // set environment properties
                setLegacyProperties((Map)context.getBean("muleEnvironmentProperties", Map.class));

            }


            // Set the container Context
            Map containers = context.getBeansOfType(UMOContainerContext.class, true, false);
            setContainerContext(containers);

            // set Connectors
//            Map connectors = context.getBeansOfType(UMOConnector.class, true, false);
//            setConnectors(connectors.values());

            // set mule transaction manager
            temp = context.getBeansOfType(UMOTransactionManagerFactory.class, true, false);
            if (temp.size() > 0)
            {
                managementContext.setTransactionManager(((UMOTransactionManagerFactory)temp.values().iterator().next()).create());
            }
            else
            {
                temp = context.getBeansOfType(TransactionManager.class, true, false);
                if (temp.size() > 0)
                {
                    managementContext.setTransactionManager(((TransactionManager)temp.values().iterator().next()));
                }
            }

            // set security manager
            temp = context.getBeansOfType(UMOSecurityManager.class, true, false);
            if (temp.size() > 0)
            {
               managementContext.setSecurityManager((UMOSecurityManager)temp.values().iterator().next());
            }

            // set Transformers
//            Map transformers = context.getBeansOfType(UMOTransformer.class, true, false);
//            setTransformers(transformers.values());

            // set Endpoints
//            Map endpoints = context.getBeansOfType(UMOEndpoint.class, true, false);
//
//            if(legacy)
//            {
//                setLegacyEndpoints(endpoints.values());
//            }
//            else
//            {
//                setEndpoints(endpoints.values());
//            }

            // set Agents
//            Map agents = context.getBeansOfType(UMOAgent.class, true, false);
//            setAgents(agents.values());
//
//            // add the models
//            Map models = context.getBeansOfType(UMOModel.class, true, false);
//            if(legacy)
//            {
//                setLegacyModels(models);
//            }
//            else
//            {
//                setModels(models);
//            }
        }
        catch (Exception e)
        {
            throw new BeanInitializationException("Failed to wire MuleManager together: " + e.getMessage(), e);
        }
    }

    public void destroy() throws Exception
    {
        super.destroy();
        managementContext.dispose();
    }

    public void setManagerId(String managerId)
    {
       managementContext.setId(managerId);
    }

    protected void setContainerContext(Map containers) throws UMOException
    {

        for (Iterator iter = containers.values().iterator(); iter.hasNext();)
        {
            UMOContainerContext context =  (UMOContainerContext)iter.next();
            if(!(context instanceof MultiContainerContext))
            {
                registry.registerContainerContext(context);
            }

        }
    }

    protected void setModels(Map models) throws UMOException
    {
        if (models == null)
        {
            return;
        }
        Map.Entry entry;
        for (Iterator iterator = models.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            UMOModel model = (UMOModel)entry.getValue();
            model.setName(entry.getKey().toString());
            //TODO LM: Registry Lookup
            registry.registerModel(model);
        }
    }

    protected void setAgents(Collection agents) throws UMOException
    {
        for (Iterator iterator = agents.iterator(); iterator.hasNext();)
        {
            //TODO registry.registerMuleObject(null, (UMOAgent)iterator.next());
        registry.registerAgent((UMOAgent)iterator.next());

        }
    }

    protected void setConnectors(Collection connectors) throws UMOException
    {
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();)
        {
            //TODO LM: Registry Lookup
            //manager.registerConnector((UMOConnector)iterator.next());
            registry.registerConnector((UMOConnector)iterator.next());
        }
    }

    protected void setTransformers(Collection transformers) throws UMOException
    {
        for (Iterator iterator = transformers.iterator(); iterator.hasNext();)
        {
            //TODO LM: Registry Lookup
            //manager.registerTransformer((UMOTransformer)iterator.next());
            registry.registerTransformer((UMOTransformer)iterator.next());

        }
    }

    protected void setEndpoints(Collection endpoints) throws UMOException
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            UMOEndpoint ep  = (UMOEndpoint)iterator.next();
            if(UMOImmutableEndpoint.ENDPOINT_TYPE_GLOBAL.equals(ep.getType()))
            {
                //TODO LM: Registry Lookup

                //manager.registerEndpoint(ep);
                registry.registerEndpoint((UMOEndpoint)iterator.next());
            }
        }
    }



    /**
     * In Mule 1.x all endpoints in the the context should be registered with the manager
     * @param endpoints
     * @throws org.mule.umo.lifecycle.InitialisationException
     */
    //@Override
    protected void setLegacyEndpoints(Collection endpoints) throws UMOException
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            UMOEndpoint ep  = (UMOEndpoint)iterator.next();
            //TODO LM: Replace
            registry.registerEndpoint(ep);
        }
    }

    /**
     * In Mule 1.x Inherited model types need to be handled differently to the other model types.  this
     * method has the logic to discover the correct model and copy service descriptors to the correct model
     * @param models
     * @throws UMOException
     */
    //@Override
    protected void setLegacyModels(Map models) throws UMOException
    {
        if (models == null)
        {
            return;
        }

        UMOModel model;

        if(models.size() > 1)
        {
            throw new IllegalArgumentException("Legacy configuration could only support onr model");
        }
        else if (models.size() == 0)
        {
            model = ModelHelper.getFirstUserModel();
            if(model==null)
            {
                throw new IllegalArgumentException("There is no component model set");
            }
        }
        else
        {
            model = (UMOModel)models.values().iterator().next();
            registry.registerModel(model);
        }

        Map components = context.getBeansOfType(UMODescriptor.class, true, false);
        UMODescriptor d;

        for (Iterator iterator = components.values().iterator(); iterator.hasNext();)
        {
            d = (UMODescriptor)iterator.next();
            model.registerComponent(d);
        }

    }

    protected void setLegacyProperties(Map props)
    {
        if(props!=null)
        {
            registry.addProperties(props);
        }
    }
}
