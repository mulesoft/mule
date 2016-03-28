/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.descriptor;

import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;

public class PropertiesDescriptorParser implements DescriptorParser
{
    protected static final String PROPERTY_ENCODING = "encoding";
    protected static final String PROPERTY_CONFIG_BUILDER = "config.builder";
    protected static final String PROPERTY_DOMAIN = "domain";
    // support not yet implemented for CL reversal
    protected static final String PROPERTY_CONFIG_RESOURCES = "config.resources";
    protected static final String PROPERTY_REDEPLOYMENT_ENABLED = "redeployment.enabled";
    protected static final String PROPERTY_LOG_CONFIG_FILE = "log.configFile";
    protected static final String PROPERTY_LOADER_OVERRIDE = "loader.override";
    protected static final String PROPERTY_SCAN_PACKAGES = "scan.packages";

    @Override
    public ApplicationDescriptor parse(File descriptor, String applicationName) throws IOException
    {
        final Properties p = PropertiesUtils.loadProperties(new FileInputStream(descriptor));

        ApplicationDescriptor d = new ApplicationDescriptor();
        d.setEncoding(p.getProperty(PROPERTY_ENCODING));
        d.setConfigurationBuilder(p.getProperty(PROPERTY_CONFIG_BUILDER));
        d.setDomain(p.getProperty(PROPERTY_DOMAIN));
        d.setPackagesToScan(p.getProperty(PROPERTY_SCAN_PACKAGES));

        final String resProps = p.getProperty(PROPERTY_CONFIG_RESOURCES);
        String[] urls;
        if (StringUtils.isBlank(resProps))
        {
            urls = new String[] {ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE};
        }
        else
        {
            urls = resProps.split(",");
        }
        d.setConfigResources(urls);

        String[] absoluteResourcePaths = convertConfigResourcesToAbsolutePatch(urls, applicationName);
        d.setAbsoluteResourcePaths(absoluteResourcePaths);
        d.setConfigResourcesFile(convertConfigResourcesToFile(absoluteResourcePaths));

        // supports true (case insensitive), yes, on as positive values
        d.setRedeploymentEnabled(BooleanUtils.toBoolean(p.getProperty(PROPERTY_REDEPLOYMENT_ENABLED, Boolean.TRUE.toString())));

        if (p.containsKey(PROPERTY_LOG_CONFIG_FILE))
        {
            d.setLogConfigFile(new File(p.getProperty(PROPERTY_LOG_CONFIG_FILE)));
        }

        final String overrideString = p.getProperty(PROPERTY_LOADER_OVERRIDE);
        if (StringUtils.isNotBlank(overrideString))
        {
            Set<String> values = new HashSet<String>();
            final String[] overrides = overrideString.split(",");
            Collections.addAll(values, overrides);
            d.setLoaderOverride(values);
        }

        return d;
    }

    private File[] convertConfigResourcesToFile(String[] absoluteResourcePaths)
    {
        File[] configResourcesFile = new File[absoluteResourcePaths.length];
        for (int i = 0; i < absoluteResourcePaths.length; i++)
        {
            configResourcesFile[i] = new File(absoluteResourcePaths[i]);
        }
        return configResourcesFile;
    }

    private String[] convertConfigResourcesToAbsolutePatch(final String[] configResources, String appName)
    {
        String[] absoluteResourcePaths = new String[configResources.length];
        for (int i = 0; i < configResources.length; i++)
        {
            String resource = configResources[i];
            absoluteResourcePaths[i] = toAbsoluteFile(resource, appName);
        }
        return absoluteResourcePaths;
    }

    /**
     * Resolve a resource relative to an application root.
     *
     * @param path the relative path to resolve
     * @return absolute path, may not actually exist (check with File.exists())
     */
    protected String toAbsoluteFile(String path, String appName)
    {
        return MuleContainerBootstrapUtils.getMuleAppDir(appName).getAbsolutePath() + File.separator + path;
    }

    @Override
    public String getSupportedFormat()
    {
        return "properties";
    }
}
