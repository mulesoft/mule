/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppPluginsFolder;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.launcher.application.DuplicateExportedPackageException;
import org.mule.runtime.module.launcher.builder.ApplicationPluginFileBuilder;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationDescriptorFactoryTestCase extends AbstractMuleTestCase
{

    public static final String APP_NAME = "testApp";
    public static final String JAR_FILE_NAME = "test.jar";

    @Rule
    public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
    private ApplicationPluginRepository applicationPluginRepository;

    @Before
    public void setUp() throws Exception
    {
        applicationPluginRepository = mock(ApplicationPluginRepository.class);
        when(applicationPluginRepository.getContainerApplicationPluginDescriptors()).thenReturn(emptyList());
    }

    @Test
    public void readsPlugin() throws Exception
    {
        File pluginDir = getAppPluginsFolder(APP_NAME);
        pluginDir.mkdirs();
        final File pluginFile = new ApplicationPluginFileBuilder("plugin").usingLibrary("lib/echo-test.jar").getArtifactFile();
        copyFile(pluginFile, new File(pluginDir, "plugin1.zip"));
        copyFile(pluginFile, new File(pluginDir, "plugin2.zip"));

        final ApplicationPluginDescriptorFactory pluginDescriptorFactory = mock(ApplicationPluginDescriptorFactory.class);

        final ApplicationDescriptorFactory applicationDescriptorFactory = new ApplicationDescriptorFactory(pluginDescriptorFactory, applicationPluginRepository);
        final ApplicationPluginDescriptor expectedPluginDescriptor1 = mock(ApplicationPluginDescriptor.class);
        when(expectedPluginDescriptor1.getClassLoaderFilter()).thenReturn(ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER);
        final ApplicationPluginDescriptor expectedPluginDescriptor2 = mock(ApplicationPluginDescriptor.class);
        when(expectedPluginDescriptor2.getClassLoaderFilter()).thenReturn(ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER);
        when(pluginDescriptorFactory.create(any())).thenReturn(expectedPluginDescriptor1).thenReturn(expectedPluginDescriptor2);

        ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

        Set<ApplicationPluginDescriptor> plugins = desc.getPlugins();
        assertThat(plugins.size(), equalTo(2));
        assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor1)));
        assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor2)));
    }

    @Test
    public void readsSharedPluginLibs() throws Exception
    {
        File pluginLibDir = MuleFoldersUtil.getAppSharedPluginLibsFolder(APP_NAME);
        pluginLibDir.mkdirs();

        copyResourceAs("test-jar-with-resources.jar", pluginLibDir, JAR_FILE_NAME);
        ApplicationDescriptor desc = new ApplicationDescriptorFactory(new ApplicationPluginDescriptorFactory(new DefaultArtifactClassLoaderFilterFactory()), applicationPluginRepository).create(getAppFolder(APP_NAME));

        URL[] sharedPluginLibs = desc.getSharedPluginLibs();

        assertThat(sharedPluginLibs[0].toExternalForm(), endsWith(JAR_FILE_NAME));
    }

    @Test(expected = DuplicateExportedPackageException.class)
    public void validatesExportedPackageDuplication() throws Exception
    {
        File pluginDir = getAppPluginsFolder(APP_NAME);
        pluginDir.mkdirs();
        final File pluginFile = new ApplicationPluginFileBuilder("plugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo").getArtifactFile();
        copyFile(pluginFile, new File(pluginDir, "plugin1.zip"));
        copyFile(pluginFile, new File(pluginDir, "plugin2.zip"));

        final ApplicationPluginDescriptorFactory pluginDescriptorFactory = new ApplicationPluginDescriptorFactory(new DefaultArtifactClassLoaderFilterFactory());
        final ApplicationDescriptorFactory applicationDescriptorFactory = new ApplicationDescriptorFactory(pluginDescriptorFactory, applicationPluginRepository);

        applicationDescriptorFactory.create(getAppFolder(APP_NAME));
    }

    @Test(expected = DuplicateExportedPackageException.class)
    public void validatesExportedPackageDuplicationAgainstContainerPlugin() throws Exception
    {
        File pluginDir = getAppPluginsFolder(APP_NAME);
        pluginDir.mkdirs();
        final File pluginFile = new ApplicationPluginFileBuilder("plugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo").getArtifactFile();
        copyFile(pluginFile, new File(pluginDir, "plugin1.zip"));

        final ApplicationPluginDescriptorFactory pluginDescriptorFactory = new ApplicationPluginDescriptorFactory(new DefaultArtifactClassLoaderFilterFactory());
        final ApplicationPluginRepository applicationPluginRepository = mock(ApplicationPluginRepository.class);
        final ApplicationPluginDescriptor plugin2Descriptor = new ApplicationPluginDescriptor();
        plugin2Descriptor.setName("plugin2");
        plugin2Descriptor.setClassLoaderFilter(new ArtifactClassLoaderFilter(Collections.singleton("org.foo"), Collections.emptySet()));
        when(applicationPluginRepository.getContainerApplicationPluginDescriptors()).thenReturn(Collections.singletonList(plugin2Descriptor));
        final ApplicationDescriptorFactory applicationDescriptorFactory = new ApplicationDescriptorFactory(pluginDescriptorFactory, applicationPluginRepository);

        applicationDescriptorFactory.create(getAppFolder(APP_NAME));
    }

    private void copyResourceAs(String resourceName, File folder, String fileName) throws IOException
    {
        final InputStream sourcePlugin = IOUtils.getResourceAsStream(resourceName, getClass());
        IOUtils.copy(sourcePlugin, new FileOutputStream(new File(folder, fileName)));
    }
}
