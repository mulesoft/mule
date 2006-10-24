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
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.ConfigurationBuilder;
import org.mule.extras.spring.SpringContainerContext;
import org.mule.umo.manager.UMOManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * This Bean can e used to bootstrap a MuleManager instance in a Spring context. This
 * is different to the <code>AutoWireUMOManagerFactoryBean</code> in that the
 * Manager is not initialised using beans from the ApplicationContext. Instead, a
 * list of Mule Configuration resources can be passed in. The Configuration builder
 * can be overloaded so that other types of configuration resources, such as
 * BeanShell or Groovy scripts cn be used to actually configure the server. For
 * example to pick up all Mule confuration resources from the classpath, use
 * something like - <beans> <bean id="muleManager" class="eg.mule.MuleManagerBean"
 * depends-on="jms.broker"> <property name="configResources"
 * value="classpath*:META-INF/services/*.mule.xml"/> </bean> .... </beans>
 */
public class MuleManagerBean
    implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener
{

    private Resource[] configResources;
    private SpringContainerContext containerContext;
    private UMOManager muleManager;
    private ConfigurationBuilder configurationBuilder;

    public void afterPropertiesSet() throws Exception
    {
        if (configurationBuilder == null)
        {
            configurationBuilder = new MuleXmlConfigurationBuilder();
        }
    }

    public void setConfigResources(Resource[] configResources)
    {
        this.configResources = configResources;
    }

    public void destroy() throws Exception
    {
        if (muleManager != null)
        {
            muleManager.dispose();
            muleManager = null;
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        containerContext = new SpringContainerContext();
        containerContext.setBeanFactory(applicationContext);
    }

    private UMOManager createMuleManager() throws Exception
    {
        UMOManager muleManager = MuleManager.getInstance();
        muleManager.setContainerContext(containerContext);

        String configFilenames = getConfigFilenames();
        configurationBuilder.configure(configFilenames);

        return muleManager;
    }

    private String getConfigFilenames()
    {
        String[] result = new String[configResources.length];
        for (int i = 0; i < result.length; i++)
        {
            try
            {
                result[i] = configResources[i].getURL().getPath();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return StringUtils.arrayToCommaDelimitedString(result);
    }

    public void onApplicationEvent(ApplicationEvent event)
    {
        if (muleManager == null)
        {
            try
            {
                muleManager = createMuleManager();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public ConfigurationBuilder getConfigurationBuilder()
    {
        return configurationBuilder;
    }

    public void setConfigurationBuilder(ConfigurationBuilder configurationBuilder)
    {
        this.configurationBuilder = configurationBuilder;
    }
}
