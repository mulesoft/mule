/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class MulePluginClassLoaderManagerTestCase extends AbstractMuleTestCase
{

    private MulePluginClassLoaderManager pluginClassLoaderManager = new MulePluginClassLoaderManager();

    @Test
    public void isEmptyOnStartup() throws Exception
    {
        assertThat(pluginClassLoaderManager.getPluginClassLoaders().size(), equalTo(0));
    }

    @Test
    public void addsPluginClassLoaders() throws Exception
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        pluginClassLoaderManager.addPluginClassLoader(classLoader);

        assertThat(pluginClassLoaderManager.getPluginClassLoaders().size(), equalTo(1));
        assertThat(pluginClassLoaderManager.getPluginClassLoaders(), hasItem(classLoader));
    }
}
