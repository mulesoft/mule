/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.groovy.config;

import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManager;
import org.mule.util.ClassHelper;
import org.mule.util.Utility;

import java.io.File;
import java.net.URL;

/**
 * <code>GroovyConfigurationBuilder</code> can be used to configure a Mule Instance from
 * Groovy.
 * The user can parse in a comma separated lst of groovy scripts to execute, which will be
 * executed in the order they are parsed in.
 *
 * The scripts are assumed to be 'scripts' rather than Grooy obejects.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class GroovyConfigurationBuilder implements ConfigurationBuilder
{
    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources a comma separated list of configuration files to load,
     *                        this should be accessible on the classpath or filesystem
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManager configure(String configResources) throws ConfigurationException
    {
        String script = null;
        try
        {
            String[] scripts = Utility.split(configResources, ",");
            for (int i = 0; i < scripts.length; i++)
            {
                script = scripts[i];
                File f = new File(script);
                GroovyShell shell = new GroovyShell(getClass().getClassLoader());
                Script s = null;
                if(f.exists()) {
                    s = shell.parse(new GroovyCodeSource(f));
                } else {
                    URL url = ClassHelper.getResource(script, getClass());
                    if(url == null) {
                        throw new ConfigurationException("Config resource: " + script + " was not found on the file system of classpath");
                    }
                    s = shell.parse(new GroovyCodeSource(url));
                }
                s.run();
            }
        } catch (Exception e)
        {
            throw new ConfigurationException("Failed to load script: " + script + ", " + e.getMessage(), e);
        }
        try
        {
            MuleManager.getInstance().start();
        } catch (UMOException e)
        {
            throw new ConfigurationException("Failed to start Mule server from builder: " + e.getMessage(), e);
        }
        return MuleManager.getInstance();
    }
}
