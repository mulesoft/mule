/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.launcher.MuleFoldersUtil.getAppFolder;
import static org.mule.module.launcher.MuleFoldersUtil.getAppPluginsFolder;
import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationDescriptorFactoryTestCase extends AbstractMuleTestCase
{

    public static final String APP_NAME = "testApp";
    public static final String JAR_FILE_NAME = "test.jar";

    @Rule
    public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);

    @Test
    public void readsPlugin() throws Exception
    {
        File pluginDir = getAppPluginsFolder(APP_NAME);
        pluginDir.mkdirs();
        copyResourceAs("plugins/groovy-plugin.zip", pluginDir, "groovy-plugin1.zip");
        copyResourceAs("plugins/groovy-plugin.zip", pluginDir, "groovy-plugin2.zip");

        final ApplicationDescriptorFactory applicationDescriptorFactory = new ApplicationDescriptorFactory();
        final ApplicationPluginDescriptorFactory pluginDescriptorFactory = mock(ApplicationPluginDescriptorFactory.class);
        final ApplicationPluginDescriptor expectedPluginDescriptor1 = mock(ApplicationPluginDescriptor.class);
        final ApplicationPluginDescriptor expectedPluginDescriptor2 = mock(ApplicationPluginDescriptor.class);
        when(pluginDescriptorFactory.create(any())).thenReturn(expectedPluginDescriptor1).thenReturn(expectedPluginDescriptor2);

        applicationDescriptorFactory.setPluginDescriptorFactory(pluginDescriptorFactory);

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
        ApplicationDescriptor desc = new ApplicationDescriptorFactory().create(getAppFolder(APP_NAME));

        URL[] sharedPluginLibs = desc.getSharedPluginLibs();

        assertThat(sharedPluginLibs[0].toExternalForm(), endsWith(JAR_FILE_NAME));
    }

    private void copyResourceAs(String resourceName, File folder, String fileName) throws IOException
    {
        final InputStream sourcePlugin = IOUtils.getResourceAsStream(resourceName, getClass());
        IOUtils.copy(sourcePlugin, new FileOutputStream(new File(folder, fileName)));
    }
}
