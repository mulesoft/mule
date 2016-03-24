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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_RESOURCE_PACKAGES_PROPERTY;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PLUGIN_PROPERTIES;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PROPERTY_LOADER_OVERRIDE;
import static org.mule.util.FileUtils.stringToFile;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.module.artifact.classloader.ClassLoaderFilter;
import org.mule.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicyFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationPluginDescriptorFactoryTestCase extends AbstractMuleTestCase
{

    public static final String PLUGIN_NAME = "testPlugin";

    @Rule
    public TemporaryFolder pluginsFolder = new TemporaryFolder();

    private final ClassLoaderLookupPolicyFactory classLoaderLookupPolicyFactory = mock(ClassLoaderLookupPolicyFactory.class);
    private final ClassLoaderFilterFactory classLoaderFilterFactory = mock(ClassLoaderFilterFactory.class);
    private ApplicationPluginDescriptorFactory descriptorFactory = new ApplicationPluginDescriptorFactory(classLoaderLookupPolicyFactory, classLoaderFilterFactory);

    @Before
    public void setUp() throws Exception
    {
        when(classLoaderLookupPolicyFactory.create(null)).thenReturn(ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY);
        when(classLoaderFilterFactory.create(null, null)).thenReturn(ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER);
    }

    @Test
    public void parsesPluginWithNoDescriptor() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLoaderOverrides() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final String overrides = "org.foo, org.bar";
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(classLoaderLookupPolicyFactory.create(overrides)).thenReturn(classLoaderLookupPolicy);

        new PluginPropertiesBuilder(pluginFolder).overriding(overrides).build();

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).configuredWith(classLoaderLookupPolicy).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLoaderExportClass() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final String exportedClassPackages = "org.foo, org.bar";
        new PluginPropertiesBuilder(pluginFolder).exportingClassesFrom(exportedClassPackages).build();

        final ClassLoaderFilter classLoaderFilter = mock(ClassLoaderFilter.class);
        when(classLoaderFilterFactory.create(exportedClassPackages, null)).thenReturn(classLoaderFilter);

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).limitingAccessWith(classLoaderFilter).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLoaderExportResource() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final String exportedResourcePackages = "META-INF, META-INF/xml";
        new PluginPropertiesBuilder(pluginFolder).exportingResourcesFrom(exportedResourcePackages).build();

        final ClassLoaderFilter classLoaderFilter = mock(ClassLoaderFilter.class);
        when(classLoaderFilterFactory.create(null, exportedResourcePackages)).thenReturn(classLoaderFilter);

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).limitingAccessWith(classLoaderFilter).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLibraries() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final File pluginLibFolder = new File(pluginFolder, "lib");
        assertThat(pluginLibFolder.mkdir(), is(true));

        final File jar1 = createDummyJarFile(pluginLibFolder, "lib1.jar");
        final File jar2 = createDummyJarFile(pluginLibFolder, "lib2.jar");
        final URL[] libraries = new URL[] {jar1.toURI().toURL(), jar2.toURI().toURL()};

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).containing(libraries).assertPluginDescriptor(pluginDescriptor);
    }

    private File createPluginFolder()
    {
        final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
        assertThat(pluginFolder.mkdir(), is(true));
        return pluginFolder;
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
        private ClassLoaderLookupPolicy classLoaderLookupPolicy = ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY;
        private ClassLoaderFilter classLoaderFilter = ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;

        public PluginDescriptorChecker(File pluginFolder)
        {
            this.pluginFolder = pluginFolder;
        }

        public PluginDescriptorChecker limitingAccessWith(ClassLoaderFilter classLoaderFilter)
        {
            this.classLoaderFilter = classLoaderFilter;

            return this;
        }

        public PluginDescriptorChecker containing(URL[] libraries)
        {
            runtimeLibs = libraries;
            return this;
        }

        public PluginDescriptorChecker configuredWith(ClassLoaderLookupPolicy classLoaderLookupPolicy)
        {
            this.classLoaderLookupPolicy = classLoaderLookupPolicy;
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
            assertThat(pluginDescriptor.getRootFolder(), equalTo(pluginFolder));
            assertThat(pluginDescriptor.getClassLoaderLookupPolicy(), is(classLoaderLookupPolicy));
            assertThat(pluginDescriptor.getClassLoaderFilter(), is(classLoaderFilter));
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
        private String overrides;
        private String exportedClassPackages;
        private String exportedResourcePackages;

        public PluginPropertiesBuilder(File pluginFolder)
        {
            this.pluginFolder = pluginFolder;
        }

        public PluginPropertiesBuilder overriding(String overrides)
        {
            this.overrides = overrides;

            return this;
        }

        public PluginPropertiesBuilder exportingClassesFrom(String packages)
        {
            this.exportedClassPackages = packages;

            return this;
        }

        public PluginPropertiesBuilder exportingResourcesFrom(String packages)
        {
            this.exportedResourcePackages = packages;

            return this;
        }

        public File build() throws IOException
        {
            final File pluginProperties = new File(pluginFolder, PLUGIN_PROPERTIES);
            if (pluginProperties.exists())
            {
                throw new IllegalStateException(String.format("File '%s' already exists", pluginProperties.getAbsolutePath()));
            }

            addDescriptorProperty(pluginProperties, PROPERTY_LOADER_OVERRIDE, this.overrides);
            addDescriptorProperty(pluginProperties, EXPORTED_CLASS_PACKAGES_PROPERTY, this.exportedClassPackages);
            addDescriptorProperty(pluginProperties, EXPORTED_RESOURCE_PACKAGES_PROPERTY, this.exportedResourcePackages);

            return pluginProperties;
        }

        private void addDescriptorProperty(File pluginProperties, String propertyName, String propertyValue) throws IOException
        {
            if (!StringUtils.isEmpty(propertyValue))
            {
                final String descriptorProperty = generateDescriptorProperty(propertyName, propertyValue);

                stringToFile(pluginProperties.getAbsolutePath(), descriptorProperty, true);
            }
        }

        private String generateDescriptorProperty(String propertyName, String propertyValue)
        {
            StringBuilder builder = new StringBuilder(propertyName).append("=").append(propertyValue);

            return builder.toString();
        }
    }
}