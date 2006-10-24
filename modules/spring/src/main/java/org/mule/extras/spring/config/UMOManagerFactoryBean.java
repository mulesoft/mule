/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOTransactionManagerFactory;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>UMOManagerFactoryBean</code> is a Spring FactoryBean used for creating a
 * MuleManager from a Spring context. The context must explicitly wire the beans
 * together. Users might want to try AutowireUMOManagerFactoryBean for a simpler and
 * cleaner spring configuration.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see AutowireUMOManagerFactoryBean
 */
public class UMOManagerFactoryBean implements FactoryBean, InitializingBean, DisposableBean
{
    UMOManager manager;

    public UMOManagerFactoryBean() throws Exception
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

    public void setMessageEndpoints(Map endpoints) throws InitialisationException
    {
        Map.Entry entry;
        for (Iterator iterator = endpoints.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            manager.registerEndpointIdentifier(entry.getKey().toString(), entry.getValue().toString());

        }
    }

    public void setProperties(Map props)
    {
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            manager.setProperty(entry.getKey(), entry.getValue());
        }
    }

    public void setConfiguration(MuleConfiguration config)
    {
        MuleManager.setConfiguration(config);
    }

    public void setTransactionManagerFactory(UMOTransactionManagerFactory factory) throws Exception
    {
        manager.setTransactionManager(factory.create());
    }

    public void setConnectors(List connectors) throws UMOException
    {
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();)
        {
            manager.registerConnector((UMOConnector)iterator.next());
        }
    }

    public void setTransformers(List transformers) throws InitialisationException
    {
        for (Iterator iterator = transformers.iterator(); iterator.hasNext();)
        {
            manager.registerTransformer((UMOTransformer)iterator.next());
        }
    }

    public void setProviders(List endpoints) throws InitialisationException
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            manager.registerEndpoint((UMOEndpoint)iterator.next());
        }
    }

    public void setInterceptorStacks(Map interceptors)
    {
        Map.Entry entry;
        for (Iterator iterator = interceptors.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            manager.registerInterceptorStack(entry.getKey().toString(), (UMOInterceptorStack)entry.getValue());
        }
    }

    public void setModel(UMOModel model) throws UMOException
    {
        manager.setModel(model);
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
