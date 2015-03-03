/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.plugin.PluginClasspath;
import org.mule.module.launcher.plugin.PluginDescriptor;
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

public class AppBloodhoundTestCase extends AbstractMuleTestCase
{

    public static final String APP_NAME = "testApp";
    public static final String JAR_FILE_NAME = "test.jar";

    @Rule
    public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);

    @Test
    public void readsPlugin() throws Exception
    {
        File pluginDir = MuleFoldersUtil.getAppPluginsFolder(APP_NAME);
        pluginDir.mkdirs();
        copyResourceAs("plugins/groovy-plugin.zip", pluginDir, "groovy-plugin.zip");

        ApplicationDescriptor desc = new DefaultAppBloodhound().fetch(APP_NAME);

        Set<PluginDescriptor> plugins = desc.getPlugins();
        assertThat(plugins.size(), equalTo(1));

        final PluginDescriptor plugin = plugins.iterator().next();
        assertThat(plugin.getName(), equalTo("groovy-plugin"));
        final PluginClasspath cp = plugin.getClasspath();
        assertThat(cp.toURLs().length, equalTo(2));
        assertThat(cp.getRuntimeLibs()[0].toExternalForm(), endsWith("groovy-all-1.8.0.jar"));
    }

    @Test
    public void readsSharedPluginLibs() throws Exception
    {
        File pluginLibDir = MuleFoldersUtil.getAppSharedPluginLibsFolder(APP_NAME);
        pluginLibDir.mkdirs();

        copyResourceAs("test-jar-with-resources.jar", pluginLibDir, JAR_FILE_NAME);
        ApplicationDescriptor desc = new DefaultAppBloodhound().fetch(APP_NAME);

        URL[] sharedPluginLibs = desc.getSharedPluginLibs();

        assertThat(sharedPluginLibs[0].toExternalForm(), endsWith(JAR_FILE_NAME));
    }

    private void copyResourceAs(String resourceName, File folder, String fileName) throws IOException
    {
        final InputStream sourcePlugin = IOUtils.getResourceAsStream(resourceName, getClass());
        IOUtils.copy(sourcePlugin, new FileOutputStream(new File(folder, fileName)));
    }
}
