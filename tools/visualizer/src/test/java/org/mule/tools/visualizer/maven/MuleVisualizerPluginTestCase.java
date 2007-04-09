/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.maven;

import org.mule.util.FileUtils;

import java.util.Arrays;

import junit.framework.TestCase;

public class MuleVisualizerPluginTestCase extends TestCase
{
    
    public void testEcho() throws Exception
    {
        MuleVisualizerPlugin plugin = new MuleVisualizerPlugin();
        String file = FileUtils.getResourcePath("echo-config.xml", getClass());
        assertNotNull("missing echo-config.xml", file);
        plugin.setFiles(Arrays.asList(new String[]{file}));
        plugin.setOutputdir(FileUtils.getResourcePath("target", getClass()));
        plugin.execute();
    }

}
