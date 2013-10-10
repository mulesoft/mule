/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.descriptor;

import org.mule.module.launcher.plugin.PluginDescriptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ApplicationDescriptor
{
    public static final String DEFAULT_CONFIGURATION_RESOURCE = "mule-config.xml";
    public static final String DEFAULT_APP_PROPERTIES_RESOURCE = "mule-app.properties";

    /**
     * Required to support the '-config spring' shortcut. Don't use a class object so
     * the core doesn't depend on mule-module-spring.
     */
    public static final String CLASSNAME_SPRING_CONFIG_BUILDER = "org.mule.config.spring.SpringXmlConfigurationBuilder";


    private String appName;
    private String encoding;
    private String configurationBuilder;
    private String domain;
    private String packagesToScan;
    private String[] configResources = new String[] {DEFAULT_CONFIGURATION_RESOURCE};
    private Map<String, String> appProperties = new HashMap<String, String>();

    private boolean redeploymentEnabled = true;

    private boolean privileged;

    private Set<String> loaderOverride = new HashSet<String>();

    private Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>(0);

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public Map<String, String> getAppProperties()
    {
        return appProperties;
    }

    public void setAppProperties(Map<String, String> appProperties)
    {
        this.appProperties = appProperties;
    }

    /**
     * Config builder name. If the name not found among available builder shortcuts
     * (e.g. 'spring' for default xml-based Mule config), then a FQN of the class to
     * use.
     * @return null for defaults
     */
    public String getConfigurationBuilder()
    {
        return configurationBuilder;
    }

    public void setConfigurationBuilder(String configurationBuilder)
    {
        this.configurationBuilder = configurationBuilder;
    }


    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String[] getConfigResources()
    {
        return configResources;
    }

    public void setConfigResources(String[] configResources)
    {
        this.configResources = configResources;
    }

    public boolean isRedeploymentEnabled()
    {
        return redeploymentEnabled;
    }

    public void setRedeploymentEnabled(boolean redeploymentEnabled)
    {
        this.redeploymentEnabled = redeploymentEnabled;
    }

    /**
     * @deprecated use {@link #isPrivileged}
     */
    @Deprecated
    public boolean isPriviledged()
    {
        return privileged;
    }

    /**
     * @deprecated use @{link #setPrivileged}
     */
    @Deprecated
    public void setPriviledged(boolean priviledged)
    {
        this.privileged = priviledged;
    }

    public boolean isPrivileged()
    {
        return privileged;
    }

    public void setPrivileged(boolean privileged)
    {
        this.privileged = privileged;
    }

    public Set<String> getLoaderOverride()
    {
        return loaderOverride;
    }

    public void setLoaderOverride(Set<String> loaderOverride)
    {
        this.loaderOverride = loaderOverride;
    }

    public Set<PluginDescriptor> getPlugins()
    {
        return plugins;
    }

    public void setPlugins(Set<PluginDescriptor> plugins)
    {
        this.plugins = plugins;
    }

    public String getPackagesToScan()
    {
        return packagesToScan;
    }

    public void setPackagesToScan(String packages)
    {
        this.packagesToScan = packages;
    }
}
