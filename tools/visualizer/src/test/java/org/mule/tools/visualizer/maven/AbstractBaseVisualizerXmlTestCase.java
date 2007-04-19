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
