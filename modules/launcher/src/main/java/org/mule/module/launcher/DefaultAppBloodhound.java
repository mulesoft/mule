/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.MuleRuntimeException;
import org.mule.config.PreferredObjectSelector;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.descriptor.DescriptorParser;
import org.mule.module.launcher.descriptor.EmptyApplicationDescriptor;
import org.mule.module.launcher.descriptor.PropertiesDescriptorParser;
import org.mule.module.launcher.plugin.PluginDescriptor;
import org.mule.module.launcher.plugin.PluginDescriptorParser;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;
import org.mule.util.PropertiesUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.imageio.spi.ServiceRegistry;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 */
public class DefaultAppBloodhound implements AppBloodhound
{

    // file extension -> parser implementation
    protected Map<String, DescriptorParser> parserRegistry = new HashMap<String, DescriptorParser>();
    public static final String SYSTEM_PROPERTY_OVERRIDE = "-O";

    public DefaultAppBloodhound()
    {
        // defaults first
        parserRegistry.put("properties", new PropertiesDescriptorParser());

        final Iterator<DescriptorParser> it = ServiceRegistry.lookupProviders(DescriptorParser.class);

        MultiMap overrides = new MultiValueMap();
        while (it.hasNext())
        {
            final DescriptorParser parser = it.next();
            overrides.put(parser.getSupportedFormat(), parser);
        }
        mergeParserOverrides(overrides);
    }

    public ApplicationDescriptor fetch(String appName) throws IOException
    {
        final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
        File appDir = new File(appsDir, appName);
        if (!appDir.exists())
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage(
                            String.format("Application directory does not exist: '%s'", appDir)));
        }
        // list mule-deploy.* files
        @SuppressWarnings("unchecked")
        Collection<File> deployFiles = FileUtils.listFiles(appDir, new WildcardFileFilter("mule-deploy.*"), null);
        if (deployFiles.size() > 1)
        {
            // TODO need some kind of an InvalidAppFormatException
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage(
                            String.format("More than one mule-deploy descriptors found in application '%s'", appName)));
        }

        ApplicationDescriptor desc;

        // none found, return defaults
        if (deployFiles.isEmpty())
        {
            desc = new EmptyApplicationDescriptor(appName);
        }
        else
        {
            // lookup the implementation by extension
            final File descriptorFile = deployFiles.iterator().next();
            final String ext = FilenameUtils.getExtension(descriptorFile.getName());
            final DescriptorParser descriptorParser = parserRegistry.get(ext);

            if (descriptorParser == null)
            {
                // TODO need some kind of an InvalidAppFormatException
                throw new MuleRuntimeException(
                        MessageFactory.createStaticMessage(
                                String.format("Unsupported deployment descriptor format for app '%s': %s", appName, ext)));
            }

            desc = descriptorParser.parse(descriptorFile, appName);
            // app name is external to the deployment descriptor
            desc.setAppName(appName);
        }

        // get a ref to an optional app props file (right next to the descriptor)
        final File appPropsFile = new File(appDir, ApplicationDescriptor.DEFAULT_APP_PROPERTIES_RESOURCE);
        setApplicationProperties(desc, appPropsFile);

        final Set<PluginDescriptor> plugins = new PluginDescriptorParser(desc, appDir).parse();
        desc.setPlugins(plugins);

        desc.setSharedPluginLibs(findSharedPluginLibs(appName));

        return desc;
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

    public void setApplicationProperties(ApplicationDescriptor desc, File appPropsFile) throws IOException
    {
        // ugh, no straightforward way to convert a HashTable to a map
        Map<String, String> m = new HashMap<String, String>();

        if (appPropsFile.exists() && appPropsFile.canRead())
        {
            final Properties props = PropertiesUtils.loadProperties(appPropsFile.toURI().toURL());
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

    /**
     * Merge default and discovered overrides for descriptor parsers, taking weight into account
     *
     * @param overrides discovered parser overrides
     */
    protected void mergeParserOverrides(MultiMap overrides)
    {
        PreferredObjectSelector<DescriptorParser> selector = new PreferredObjectSelector<DescriptorParser>();

        for (Map.Entry<String, DescriptorParser> entry : parserRegistry.entrySet())
        {
            @SuppressWarnings("unchecked")
            final Collection<DescriptorParser> candidates = (Collection<DescriptorParser>) overrides.get(entry.getKey());

            if (candidates != null)
            {
                parserRegistry.put(entry.getKey(), selector.select(candidates.iterator()));
            }
        }

    }
}
