/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.lang.String.format;
import static org.mule.runtime.module.launcher.artifact.ArtifactFactoryUtils.getDeploymentFile;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.descriptor.EmptyApplicationDescriptor;
import org.mule.runtime.module.launcher.descriptor.PropertiesDescriptorParser;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Creates artifact descriptor for application
 */
public class ApplicationDescriptorFactory implements ArtifactDescriptorFactory<ApplicationDescriptor>
{

    public static final String SYSTEM_PROPERTY_OVERRIDE = "-O";

    private final ApplicationPluginDescriptorFactory pluginDescriptorFactory;

    public ApplicationDescriptorFactory(ApplicationPluginDescriptorFactory applicationPluginDescriptorFactory)
    {
        checkArgument(applicationPluginDescriptorFactory != null, "ApplicationPluginDescriptorFactory cannot be null");

        this.pluginDescriptorFactory = applicationPluginDescriptorFactory;
    }

    public ApplicationDescriptor create(File artifactFolder) throws ArtifactDescriptorCreateException
    {
        if (!artifactFolder.exists())
        {
            throw new IllegalArgumentException(format("Application directory does not exist: '%s'", artifactFolder));
        }

        final String appName = artifactFolder.getName();
        ApplicationDescriptor desc;

        try
        {
            final File deployPropertiesFile = getDeploymentFile(artifactFolder);
            if (deployPropertiesFile != null)
            {
                // lookup the implementation by extension
                final PropertiesDescriptorParser descriptorParser = new PropertiesDescriptorParser();
                desc = descriptorParser.parse(deployPropertiesFile, appName);
            }
            else
            {
                desc = new EmptyApplicationDescriptor(appName);
            }

            // get a ref to an optional app props file (right next to the descriptor)
            final File appPropsFile = new File(artifactFolder, ApplicationDescriptor.DEFAULT_APP_PROPERTIES_RESOURCE);
            setApplicationProperties(desc, appPropsFile);

            final Set<ApplicationPluginDescriptor> plugins = parsePluginDescriptors(artifactFolder, desc);
            desc.setPlugins(plugins);

            desc.setSharedPluginLibs(findSharedPluginLibs(appName));
        }
        catch (IOException e)
        {
            throw new ArtifactDescriptorCreateException("Unable to create application descriptor", e);
        }

        return desc;
    }

    private Set<ApplicationPluginDescriptor> parsePluginDescriptors(File appDir, ApplicationDescriptor appDescriptor) throws IOException
    {
        final File pluginsDir = new File(appDir, MuleFoldersUtil.PLUGINS_FOLDER);
        String[] pluginZips = pluginsDir.list(new SuffixFileFilter(".zip"));
        if (pluginZips == null || pluginZips.length == 0)
        {
            return Collections.emptySet();
        }

        Arrays.sort(pluginZips);
        Set<ApplicationPluginDescriptor> pds = new HashSet<>(pluginZips.length);

        for (String pluginZip : pluginZips)
        {
            final String pluginName = StringUtils.removeEnd(pluginZip, ".zip");
            // must unpack as there's no straightforward way for a ClassLoader to use a jar within another jar/zip
            final File tmpDir = new File(MuleContainerBootstrapUtils.getMuleTmpDir(),
                                         appDescriptor.getName() + File.separator + MuleFoldersUtil.PLUGINS_FOLDER + File.separator + pluginName);
            FileUtils.unzip(new File(pluginsDir, pluginZip), tmpDir);
            final ApplicationPluginDescriptor pd = pluginDescriptorFactory.create(tmpDir);

            pds.add(pd);
        }

        return pds;
    }

    private URL[] findSharedPluginLibs(String appName) throws MalformedURLException
    {
        Set<URL> urls = new HashSet<>();

        final File sharedPluginLibs = MuleFoldersUtil.getAppSharedPluginLibsFolder(appName);
        if (sharedPluginLibs.exists())
        {
            Collection<File> jars = FileUtils.listFiles(sharedPluginLibs, new String[] {"jar"}, false);

            for (File jar : jars)
            {
                urls.add(jar.toURI().toURL());
            }
        }

        return urls.toArray(new URL[0]);
    }

    public void setApplicationProperties(ApplicationDescriptor desc, File appPropsFile)
    {
        // ugh, no straightforward way to convert a HashTable to a map
        Map<String, String> m = new HashMap<>();

        if (appPropsFile.exists() && appPropsFile.canRead())
        {
            final Properties props;
            try
            {
                props = PropertiesUtils.loadProperties(appPropsFile.toURI().toURL());
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("Unable to obtain application properties file URL", e);
            }
            for (Object key : props.keySet())
            {
                m.put(key.toString(), props.getProperty(key.toString()));
            }
        }

        // Override with any system properties prepended with "-O" for ("override"))
        Properties sysProps = System.getProperties();
        for (Map.Entry<Object, Object> entry : sysProps.entrySet())
        {
            String key = entry.getKey().toString();
            if (key.startsWith(SYSTEM_PROPERTY_OVERRIDE))
            {
                m.put(key.substring(SYSTEM_PROPERTY_OVERRIDE.length()), entry.getValue().toString());
            }
        }
        desc.setAppProperties(m);
    }
}
