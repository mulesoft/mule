/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import static org.mule.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_RESOURCE_PACKAGES_PROPERTY;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.module.artifact.classloader.ClassLoaderFilter;
import org.mule.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicyFactory;
import org.mule.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.module.artifact.descriptor.ArtifactDescriptorFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.filefilter.SuffixFileFilter;

public class ApplicationPluginDescriptorFactory implements ArtifactDescriptorFactory<ApplicationPluginDescriptor>
{

    public static final String PROPERTY_LOADER_OVERRIDE = "loader.override";
    public static final String PLUGIN_PROPERTIES = "plugin.properties";

    private final ClassLoaderLookupPolicyFactory classLoaderLookupPolicyFactory;
    private final ClassLoaderFilterFactory classLoaderFilterFactory;

    /**
     * Creates a new instance
     *
     * @param classLoaderLookupPolicyFactory creates classloader lookup policies for the created descriptors. Not null.
     * @param classLoaderFilterFactory creates classloader filters for the created descriptors. Not null.
     */
    public ApplicationPluginDescriptorFactory(ClassLoaderLookupPolicyFactory classLoaderLookupPolicyFactory, ClassLoaderFilterFactory classLoaderFilterFactory)
    {
        checkArgument(classLoaderLookupPolicyFactory != null, "ClassLoaderLookupPolicyFactory cannot be null");
        checkArgument(classLoaderFilterFactory != null, "ClassLoaderFilterFactory cannot be null");

        this.classLoaderLookupPolicyFactory = classLoaderLookupPolicyFactory;
        this.classLoaderFilterFactory = classLoaderFilterFactory;
    }

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

            final ClassLoaderLookupPolicy classLoaderLookupPolicy = classLoaderLookupPolicyFactory.create(props.getProperty(PROPERTY_LOADER_OVERRIDE));
            descriptor.setClassLoaderLookupPolicy(classLoaderLookupPolicy);

            String exportedClasses = props.getProperty(EXPORTED_CLASS_PACKAGES_PROPERTY);
            String exportedResources = props.getProperty(EXPORTED_RESOURCE_PACKAGES_PROPERTY);

            final ClassLoaderFilter classLoaderFilter = classLoaderFilterFactory.create(exportedClasses, exportedResources);
            descriptor.setClassLoaderFilter(classLoaderFilter);
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
