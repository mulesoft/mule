/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.config;

import org.mule.tools.visualizer.postrenderers.MuleDocPostRenderer;
import org.mule.tools.visualizer.util.VelocityLogger;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.LogSystem;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public abstract class VelocitySupport
{

    public static final String FILE = "file";
    public static final String JAR = "jar";
    // this loads from the claspath
    // http://velocity.apache.org/engine/devel/apidocs/org/apache/velocity/runtime/resource/loader/ClasspathResourceLoader.html
    public static final String MAGIC_VELOCITY_RESOURCE_LOADER = "class.resource.loader.class";

    private VelocityEngine ve;

    private GraphEnvironment env = null;

    private static LogSystem logSystem;

    protected VelocitySupport(GraphEnvironment env) throws Exception
    {
        this.setEnv(env);
        logSystem = new VelocityLogger(env);
        setVe(new VelocityEngine());
        getVe().setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, logSystem);
        inferVelocityLoaderPath(getVe());
        getVe().init();
    }

    /**
     * This is something of a hack.  Velocity loads files relative to the property 
     * VelocityEngine.FILE_RESOURCE_LOADER_PATH.  So if we can find the template ourselves then 
     * we can infer what the property value should be so that velocity works ok.
     */
    private void inferVelocityLoaderPath(VelocityEngine ve) throws IOException
    {
        String defaultPath = (String) ve.getProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH);
        if (null == defaultPath || StringUtils.isEmpty(defaultPath))
        {
            URL url = IOUtils.getResourceAsUrl(MuleDocPostRenderer.DEFAULT_MULE_TEMPLATE, getClass());
            if (FILE.equals(url.getProtocol()))
            {
                String path = FileUtils.getResourcePath(MuleDocPostRenderer.DEFAULT_MULE_TEMPLATE, getClass());
                if (!StringUtils.isEmpty(path))
                {
                    File fullPath = new File(path);
                    File target = new File(MuleDocPostRenderer.DEFAULT_MULE_TEMPLATE);

                    // drop trailing files until we are at the relative parent
                    while (null != target && !StringUtils.isEmpty(target.getPath()))
                    {
                        env.log(fullPath.getPath() + " - " + target.getPath());
                        target = target.getParentFile();
                        fullPath = fullPath.getParentFile();
                    }

                    path = fullPath.getPath();
                    if (path.endsWith("!"))
                    {
                        path = path + File.separator;
                    }
                    getEnv().log(VelocityEngine.FILE_RESOURCE_LOADER_PATH + " = " + path);
                    ve.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, path);
                }
            }
            else if (JAR.equals(url.getProtocol()))
            {
                env.log(url.toString());
                ve.setProperty(VelocityEngine.RESOURCE_LOADER, "class");
                ve.setProperty(MAGIC_VELOCITY_RESOURCE_LOADER, ClasspathResourceLoader.class.getName());
            }
        }
    }

    protected void setEnv(GraphEnvironment env)
    {
        this.env = env;
    }

    protected GraphEnvironment getEnv()
    {
        return env;
    }

    protected void setVe(VelocityEngine ve)
    {
        this.ve = ve;
    }

    protected VelocityEngine getVe()
    {
        return ve;
    }

}
