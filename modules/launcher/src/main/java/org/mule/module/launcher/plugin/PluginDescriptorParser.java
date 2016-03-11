/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.plugin;

import org.mule.module.launcher.MuleFoldersUtil;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.filefilter.SuffixFileFilter;

public class PluginDescriptorParser
{

    protected static final String PROPERTY_LOADER_OVERRIDE = "loader.override";

    private ApplicationDescriptor appDescriptor;
    private File appDir;

    public PluginDescriptorParser(ApplicationDescriptor appDescriptor, File appDir)
    {
        this.appDescriptor = appDescriptor;
        this.appDir = appDir;
    }

    public Set<PluginDescriptor> parse() throws IOException
    {
        // parse plugins
        final File pluginsDir = new File(appDir, MuleFoldersUtil.PLUGINS_FOLDER);
        String[] pluginZips = pluginsDir.list(new SuffixFileFilter(".zip"));
        if (pluginZips == null || pluginZips.length == 0)
        {
            return Collections.emptySet();
        }

        Arrays.sort(pluginZips);
        Set<PluginDescriptor> pds = new HashSet<>(pluginZips.length);

        for (String pluginZip : pluginZips)
        {
            final String pluginName = StringUtils.removeEnd(pluginZip, ".zip");
            // must unpack as there's no straightforward way for a ClassLoader to use a jar within another jar/zip
            final File tmpDir = new File(MuleContainerBootstrapUtils.getMuleTmpDir(),
                                         appDescriptor.getName() + "/plugins/" + pluginName);
            // TODO fix unzip impl to not stumble over existing dirs
            FileUtils.unzip(new File(pluginsDir, pluginZip), tmpDir);
            final PluginDescriptor pd = new PluginDescriptor();
            pd.setName(pluginName);
            pd.setAppDescriptor(appDescriptor);

            final File pluginPropsFile = new File(tmpDir, "plugin.properties");
            if (pluginPropsFile.exists())
            {
                Properties props = new Properties();
                props.load(new FileReader(pluginPropsFile));

                final String overrideString = props.getProperty(PROPERTY_LOADER_OVERRIDE);
                if (StringUtils.isNotBlank(overrideString))
                {
                    Set<String> values = new HashSet<>();
                    final String[] overrides = overrideString.split(",");
                    Collections.addAll(values, overrides);
                    pd.setLoaderOverride(values);
                }
            }

            try
            {
                pd.setRuntimeClassesDir(new File(tmpDir, "classes").toURI().toURL());
                final File libDir = new File(tmpDir, "lib");
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
                pd.setRuntimeLibs(urls);
            }
            catch (MalformedURLException e)
            {
                throw new IllegalArgumentException("Failed to create plugin descriptor " + tmpDir);
            }

            pds.add(pd);
        }

        return pds;
    }
}
