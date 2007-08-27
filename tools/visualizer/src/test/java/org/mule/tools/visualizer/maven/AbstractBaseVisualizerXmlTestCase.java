/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.visualizer.maven;

import org.mule.util.FileUtils;

import java.util.Arrays;

import junit.framework.TestCase;

public abstract class AbstractBaseVisualizerXmlTestCase extends TestCase
{

    abstract String getXmlConfig();

    public void testConfig() throws Exception
    {
        MuleVisualizerPlugin plugin = new MuleVisualizerPlugin();
        String config = getXmlConfig();
        String path = FileUtils.getResourcePath(config, getClass());
        assertNotNull("missing config path: " + config , path);
        plugin.setFiles(Arrays.asList(new String[]{path}));
        plugin.setOutputdir(FileUtils.getResourcePath("target", getClass()));
        plugin.execute();
    }

}
