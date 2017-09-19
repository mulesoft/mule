/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.descriptor;

import static com.google.common.base.Optional.absent;

import org.mule.module.launcher.plugin.PluginDescriptor;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Optional;


public class ApplicationDescriptor extends ArtifactDescriptor
{
    public static final String DEFAULT_CONFIGURATION_RESOURCE = "mule-config.xml";
    public static final String DEFAULT_APP_PROPERTIES_RESOURCE = "mule-app.properties";

    /**
     * Required to support the '-config spring' shortcut. Don't use a class object so
     * the core doesn't depend on mule-module-spring.
     */
    public static final String CLASSNAME_SPRING_CONFIG_BUILDER = "org.mule.config.spring.SpringXmlConfigurationBuilder";

    private String encoding;
    private String configurationBuilder;
    private String domain;
    private String packagesToScan;
    private String[] configResources = new String[] {DEFAULT_CONFIGURATION_RESOURCE};
    private String[] absoluteResourcePaths;
    private File[] configResourcesFile;
    private Map<String, String> appProperties = new HashMap<String, String>();
    private Optional<Properties> deploymentProperties = absent();
    
    private File logConfigFile;

    private Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>(0);
    private URL[] sharedPluginLibs = new URL[0];

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

    public String[] getAbsoluteResourcePaths()
    {
        return absoluteResourcePaths;
    }

    public void setAbsoluteResourcePaths(String[] absoluteResourcePaths)
    {
        this.absoluteResourcePaths = absoluteResourcePaths;
    }

    public void setConfigResourcesFile(File[] configResourcesFile)
    {
        this.configResourcesFile = configResourcesFile;
    }

    public File[] getConfigResourcesFile()
    {
        return configResourcesFile;
    }

    public void setLogConfigFile(File logConfigFile)
    {
        this.logConfigFile = logConfigFile;
    }

    public File getLogConfigFile()
    {
        return logConfigFile;
    }

    public Set<PluginDescriptor> getPlugins()
    {
        return plugins;
    }

    public void setSharedPluginLibs(URL[] sharedPluginLibs)
    {
        this.sharedPluginLibs = sharedPluginLibs;
    }

    public URL[] getSharedPluginLibs()
    {
        return sharedPluginLibs;
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

    public Optional<Properties> getDeploymentProperties()
    {
        return deploymentProperties;
    }

    public void setDeploymentProperties(Optional<Properties> deploymentProperties)
    {
        this.deploymentProperties = deploymentProperties;
    }
}
