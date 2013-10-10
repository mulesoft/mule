/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.plugin;

import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
        final File pluginsDir = new File(appDir, "plugins");
        // TODO decide if we want to support 'exploded' plugins, for now no
        String[] pluginZips = pluginsDir.list(new SuffixFileFilter(".zip"));
        if (pluginZips == null || pluginZips.length == 0)
        {
            return Collections.emptySet();
        }

        Arrays.sort(pluginZips);
        Set<PluginDescriptor> pds = new HashSet<PluginDescriptor>(pluginZips.length);

        for (String pluginZip : pluginZips)
        {
            final String pluginName = StringUtils.removeEnd(pluginZip, ".zip");
            // must unpack as there's no straightforward way for a ClassLoader to use a jar within another jar/zip
            final File tmpDir = new File(MuleContainerBootstrapUtils.getMuleTmpDir(),
                                         appDescriptor.getAppName() + "/plugins/" + pluginName);
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
                    Set<String> values = new HashSet<String>();
                    final String[] overrides = overrideString.split(",");
                    Collections.addAll(values, overrides);
                    pd.setLoaderOverride(values);
                }
            }

            PluginClasspath cp = PluginClasspath.from(tmpDir);
            pd.setClasspath(cp);
            pds.add(pd);
        }

        return pds;
    }

    public ApplicationDescriptor getAppDescriptor()
    {
        return appDescriptor;
    }

    public File getAppDir()
    {
        return appDir;
    }
}
