/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import org.mule.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.filefilter.SuffixFileFilter;

public class ApplicationPluginDescriptorFactory implements ArtifactDescriptorFactory<ApplicationPluginDescriptor>
{

    public static final String PROPERTY_LOADER_OVERRIDE = "loader.override";
    public static final String PROPERTY_LOADER_EXPORTED = "loader.export";
    public static final String PLUGIN_PROPERTIES = "plugin.properties";

    @Override
    public ApplicationPluginDescriptor create(File pluginFolder) throws ArtifactDescriptorCreateException
    {
        final String pluginName = pluginFolder.getName();
        final ApplicationPluginDescriptor descriptor = new ApplicationPluginDescriptor();
        descriptor.setRootFolder(pluginFolder);
        descriptor.setName(pluginName);

        final File pluginPropsFile = new File(pluginFolder, PLUGIN_PROPERTIES);
        if (pluginPropsFile.exists())
        {
            Properties props = new Properties();
            try
            {
                props.load(new FileReader(pluginPropsFile));
            }
            catch (IOException e)
            {
                throw new ArtifactDescriptorCreateException("Cannot read plugin.properties file", e);
            }

            final String overrideString = props.getProperty(PROPERTY_LOADER_OVERRIDE);
            if (StringUtils.isNotBlank(overrideString))
            {
                Set<String> values = new HashSet<>();
                final String[] overrides = overrideString.split(",");
                Collections.addAll(values, overrides);
                descriptor.setLoaderOverride(values);
            }

            String exportedClasses = props.getProperty(PROPERTY_LOADER_EXPORTED);
            if (StringUtils.isNotBlank(exportedClasses))
            {
                Set<String> values = new HashSet<>();
                final String[] exports = exportedClasses.split(",");
                Collections.addAll(values, exports);
                descriptor.setExportedPrefixNames(values);
            }
        }

        try
        {
            descriptor.setRuntimeClassesDir(new File(pluginFolder, "classes").toURI().toURL());
            final File libDir = new File(pluginFolder, "lib");
            URL[] urls = new URL[0];
            if (libDir.exists())
            {
                final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(".jar"));
                urls = new URL[jars.length];
                for (int i = 0; i < jars.length; i++)
                {
                    urls[i] = jars[i].toURI().toURL();
                }
            }
            descriptor.setRuntimeLibs(urls);
        }
        catch (MalformedURLException e)
        {
            throw new ArtifactDescriptorCreateException("Failed to create plugin descriptor " + pluginFolder);
        }

        return descriptor;
    }
}
