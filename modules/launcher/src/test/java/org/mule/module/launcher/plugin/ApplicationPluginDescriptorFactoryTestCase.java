/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PLUGIN_PROPERTIES;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PROPERTY_LOADER_EXPORTED;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PROPERTY_LOADER_OVERRIDE;
import static org.mule.util.FileUtils.stringToFile;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationPluginDescriptorFactoryTestCase extends AbstractMuleTestCase
{

    public static final String PLUGIN_NAME = "testPlugin";

    @Rule
    public TemporaryFolder pluginsFolder = new TemporaryFolder();

    private ApplicationPluginDescriptorFactory descriptorFactory = new ApplicationPluginDescriptorFactory();


    @Test
    public void parsesPluginWithNoDescriptor() throws Exception
    {

        final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
        assertThat(pluginFolder.mkdir(), is(true));

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLoaderOverrides() throws Exception
    {

        final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
        assertThat(pluginFolder.mkdir(), is(true));

        final Set<String> loaderOverrides = new HashSet<>();
        loaderOverrides.add("org.foo");
        loaderOverrides.add("org.bar");

        new PluginPropertiesBuilder(pluginFolder).overriding(loaderOverrides).build();


        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).overriding(loaderOverrides).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLoaderExport() throws Exception
    {

        final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
        assertThat(pluginFolder.mkdir(), is(true));

        final Set<String> loaderExport = new HashSet<>();
        loaderExport.add("org.foo");
        loaderExport.add("org.bar");

        new PluginPropertiesBuilder(pluginFolder).exporting(loaderExport).build();


        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).exporting(loaderExport).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLibraries() throws Exception
    {

        final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
        assertThat(pluginFolder.mkdir(), is(true));

        final File pluginLibFolder = new File(pluginFolder, "lib");
        assertThat(pluginLibFolder.mkdir(), is(true));

        final File jar1 = createDummyJarFile(pluginLibFolder, "lib1.jar");
        final File jar2 = createDummyJarFile(pluginLibFolder, "lib2.jar");
        final URL[] libraries = new URL[] {jar1.toURI().toURL(), jar2.toURI().toURL()};

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).containing(libraries).assertPluginDescriptor(pluginDescriptor);
    }

    private File createDummyJarFile(File pluginLibFolder, String child) throws IOException
    {
        final File jar1 = new File(pluginLibFolder, child);
        FileUtils.write(jar1, "foo");
        return jar1;
    }

    private static class PluginDescriptorChecker
    {

        private final File pluginFolder;
        private URL[] runtimeLibs = new URL[0];;
        private Set<String> blockedPrefixes = Collections.emptySet();
        private Set<String> exportedPrefixes = Collections.emptySet();
        private Set<String> overriddenPrefixes = Collections.emptySet();

        public PluginDescriptorChecker(File pluginFolder)
        {
            this.pluginFolder = pluginFolder;
        }

        public PluginDescriptorChecker overriding(Set<String> overrides)
        {
             overriddenPrefixes = overrides;
            return this;
        }

        public PluginDescriptorChecker exporting(Set<String> exports)
        {
            exportedPrefixes = exports;
            return this;
        }


        public PluginDescriptorChecker blocking(Set<String> blocks)
        {
            overriddenPrefixes = blocks;
            return this;
        }

        public PluginDescriptorChecker containing(URL[] libraries)
        {
            runtimeLibs = libraries;
            return this;
        }

        public void assertPluginDescriptor(ApplicationPluginDescriptor pluginDescriptor)
        {
            assertThat(pluginDescriptor.getName(), equalTo(pluginFolder.getName()));
            try
            {
                assertThat(pluginDescriptor.getRuntimeClassesDir(), equalTo(new File(pluginFolder, "classes").toURI().toURL()));
            }
            catch (MalformedURLException e)
            {
                throw new AssertionError("Can't compare classes dir", e);
            }

            assertRuntimeLibs(pluginDescriptor);
            blockedPrefixes = Collections.emptySet();
            assertThat(pluginDescriptor.getBlockedPrefixNames(), equalTo(blockedPrefixes));
            assertThat(pluginDescriptor.getExportedPrefixNames(), equalTo(exportedPrefixes));
            assertThat(pluginDescriptor.getLoaderOverrides(), equalTo(overriddenPrefixes));
            assertThat(pluginDescriptor.getRootFolder(), equalTo(pluginFolder));
        }

        private void assertRuntimeLibs(ApplicationPluginDescriptor pluginDescriptor)
        {
            assertThat(pluginDescriptor.getRuntimeLibs().length, equalTo(runtimeLibs.length));
            for (URL libUrl : pluginDescriptor.getRuntimeLibs())
            {
                assertThat(pluginDescriptor.getRuntimeLibs(), hasItemInArray(equalTo(libUrl)));
            }
        }
    }

    private static class PluginPropertiesBuilder
    {

        private final File pluginFolder;
        private Set<String> overrides = new HashSet<>();
        private Set<String> exporting = new HashSet<>();

        public PluginPropertiesBuilder(File pluginFolder)
        {
            this.pluginFolder = pluginFolder;
        }

        public PluginPropertiesBuilder overriding(Set<String> overrides)
        {
            this.overrides = overrides;

            return this;
        }

        public PluginPropertiesBuilder exporting(Set<String> exporting)
        {
            this.exporting = exporting;

            return this;
        }

        public File build() throws IOException
        {
            final File pluginProperties = new File(pluginFolder, PLUGIN_PROPERTIES);
            if (pluginProperties.exists())
            {
                throw new IllegalStateException(String.format("File '%s' already exists", pluginProperties.getAbsolutePath()));
            }

            if (!overrides.isEmpty())
            {
                final String descriptorProperty = generatePackageListProperty(this.overrides, PROPERTY_LOADER_OVERRIDE);

                stringToFile(pluginProperties.getAbsolutePath(), descriptorProperty, true);
            }

            if (!exporting.isEmpty())
            {
                final String descriptorProperty = generatePackageListProperty(this.exporting, PROPERTY_LOADER_EXPORTED);

                stringToFile(pluginProperties.getAbsolutePath(), descriptorProperty, true);
            }

            return pluginProperties;
        }

        private String generatePackageListProperty(Set<String> packages, String propertyName)
        {
            StringBuilder builder = new StringBuilder(propertyName).append("=");
            boolean firstElement = true;
            for (String override : packages)
            {
                if (firstElement)
                {
                    firstElement = false;
                }
                else
                {
                    builder.append(",");
                }
                builder.append(override);
            }

            return builder.toString();
        }
    }
}