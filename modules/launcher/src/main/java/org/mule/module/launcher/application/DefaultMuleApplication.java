/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.config.builders.AutoConfigurationBuilder;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.NotificationException;
import org.mule.module.launcher.AbstractFileWatcher;
import org.mule.module.launcher.AppBloodhound;
import org.mule.module.launcher.DefaultAppBloodhound;
import org.mule.module.launcher.DefaultMuleSharedDomainClassLoader;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class DefaultMuleApplication extends EmbeddedMuleApplication
{
    protected static final String ANCHOR_FILE_BLURB = "Delete this file while Mule is running to undeploy this app in a clean way.";

    private String appName;
    protected String[] absoluteResourcePaths;

    protected DefaultMuleApplication(String appName)
    {
        this.appName = appName;
    }

    @Override
    protected ApplicationDescriptor createApplicationDescriptor()
    {
        AppBloodhound bh = new DefaultAppBloodhound();
        ApplicationDescriptor descriptor;
        try
        {
            descriptor = bh.fetch(getAppName());
        }
        catch (IOException e)
        {
            throw new InstallException(MessageFactory.createStaticMessage("Failed to parse the application deployment descriptor"), e);
        }

        // convert to absolute paths
        final String[] configResources = descriptor.getConfigResources();
        absoluteResourcePaths = new String[configResources.length];
        for (int i = 0; i < configResources.length; i++)
        {
            String resource = configResources[i];
            final File file = toAbsoluteFile(resource);
            if (!file.exists())
            {
                throw new InstallException(
                        MessageFactory.createStaticMessage(String.format("Config for app '%s' not found: %s", getAppName(), file))
                );
            }

            absoluteResourcePaths[i] = file.getAbsolutePath();
        }
        return descriptor;
    }


    protected ConfigurationBuilder createConfigurationBuilder()
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
        InvocationTargetException
    {
        // Configuration builder
        // Provide a shortcut for Spring: "-builder spring"
        String configBuilderClassName = null;
        final String builderFromDesc = getDescriptor().getConfigurationBuilder();
        if ("spring".equalsIgnoreCase(builderFromDesc))
        {
            configBuilderClassName = ApplicationDescriptor.CLASSNAME_SPRING_CONFIG_BUILDER;
        }
        else if (builderFromDesc == null)
        {
            configBuilderClassName = AutoConfigurationBuilder.class.getName();
        }
        else
        {
            configBuilderClassName = builderFromDesc;
        }

        ConfigurationBuilder cfgBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(
            configBuilderClassName, new Object[] {absoluteResourcePaths}, getDeploymentClassLoader());
        return cfgBuilder;
    }

    protected ClassLoader createDeploymentClassLoader()
    {
        final String domain = getDescriptor().getDomain();
        ClassLoader parent;

        if (StringUtils.isBlank(domain) || DefaultMuleSharedDomainClassLoader.DEFAULT_DOMAIN_NAME.equals(domain))
        {
            parent = new DefaultMuleSharedDomainClassLoader(getClass().getClassLoader());
        }
        else
        {
            // TODO handle non-existing domains with an exception
            parent = new MuleSharedDomainClassLoader(domain, getClass().getClassLoader());
        }

        return new MuleApplicationClassLoader(getAppName(), parent);
    }
    
    protected AbstractFileWatcher createRedeployMonitor() throws NotificationException
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Monitoring for hot-deployment: " + new File(absoluteResourcePaths [0]));
        }

        return new ConfigFileWatcher(new File(absoluteResourcePaths [0]));
    }
    
    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public void start()
    {
        super.start();

        try
        {
            // save app's state in the marker file
            File marker = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), String.format("%s-anchor.txt", getAppName()));
            FileUtils.writeStringToFile(marker, ANCHOR_FILE_BLURB);
        }
        catch (IOException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStartException(MessageFactory.createStaticMessage(appName), e);
        }
    }

    protected String getAppHome()
    {
        return new File(MuleContainerBootstrapUtils.getMuleAppsDir(), getAppName()).getAbsolutePath();
    }
    
    /**
     * Resolve a resource relative to an application root.
     * @param path the relative path to resolve
     * @return absolute path, may not actually exist (check with File.exists())
     */
    protected File toAbsoluteFile(String path)
    {
        final String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
        String configPath = String.format("%s/apps/%s/%s", muleHome, getAppName(), path);
        return new File(configPath);
    }

    protected class ConfigFileWatcher extends AbstractFileWatcher
    {
        public ConfigFileWatcher(File watchedResource)
        {
            super(watchedResource);
        }

        @Override
        protected synchronized void onChange(File file)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("================== Reloading " + file);
            }

            // grab the proper classloader for our context
            final ClassLoader cl = getDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            redeploy();
        }
    }
}
