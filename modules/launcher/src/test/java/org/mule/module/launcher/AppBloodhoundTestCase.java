/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.plugin.PluginClasspath;
import org.mule.module.launcher.plugin.PluginDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AppBloodhoundTestCase extends AbstractMuleTestCase
{

    private File muleHome;
    private File appsDir;

    @Test
    public void testPlugin() throws Exception
    {
        // set up some mule home structure
        final String tmpDir = System.getProperty("java.io.tmpdir");
        muleHome = new File(tmpDir, getClass().getSimpleName() + System.currentTimeMillis());
        appsDir = new File(muleHome, "apps");
        appsDir.mkdirs();
        System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

        final String appName = "app-with-plugin";
        final File appDir = new File(appsDir, appName);
        appDir.mkdirs();
        final File pluginDir = new File(appDir, "plugins");
        pluginDir.mkdirs();
        final InputStream sourcePlugin = IOUtils.getResourceAsStream("plugins/groovy-plugin.zip", getClass());
        IOUtils.copy(sourcePlugin, new FileOutputStream(new File(pluginDir, "groovy-plugin.zip")));

        AppBloodhound hound = new DefaultAppBloodhound();
        ApplicationDescriptor desc = hound.fetch(appName);
        assertNotNull(desc);
        Set<PluginDescriptor> plugins = desc.getPlugins();
        assertNotNull(plugins);
        assertEquals(1, plugins.size());

        final PluginDescriptor plugin = plugins.iterator().next();
        assertEquals("groovy-plugin", plugin.getName());
        final PluginClasspath cp = plugin.getClasspath();
        assertEquals(2, cp.toURLs().length);
        assertTrue(cp.getRuntimeLibs()[0].toExternalForm().endsWith("groovy-all-1.8.0.jar"));
    }
}
