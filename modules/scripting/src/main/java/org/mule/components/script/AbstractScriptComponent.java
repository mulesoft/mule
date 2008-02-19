/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.monitor.FileListener;
import org.mule.util.monitor.FileMonitor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractScriptComponent</code> is a service that can execute scripts as
 * components in Mule. This service also supports reloading if the script file
 * changes (providing the file is on the file system)
 * 
 */
public abstract class AbstractScriptComponent
    implements Lifecycle, ServiceAware, FileListener, Callable
{
    public static final int DEFAULT_RELOAD_INTERVAL_MS = 60000;

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    private String script = null;

    private String scriptText = null;
    private boolean autoReload = true;
    protected Service service;
    private FileMonitor monitor;
    private long reloadInterval = DEFAULT_RELOAD_INTERVAL_MS;

    public void setService(Service service) throws ConfigurationException
    {
        this.service = service;
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        if (getScript() == null && getScriptText() == null)
        {
            String extension = getDefaultFileExtension();
            if (!extension.startsWith("."))
            {
                extension = "." + extension;
            }
            setScript(service.getName() + extension);
            logger.info("script name is not set, using default: " + service.getName() + extension);
        }

        if (getScriptText() != null)
        {
            loadInterpreter(getScriptText());
        }
        else
        {
            // load script before creating a file monitor so that the script name
            // can be monified
            loadInterpreter(getScriptUrl(getScript()));
        }
        if (autoReload)
        {
            File f = FileUtils.newFile(getScript());
            if (f.exists())
            {
                monitor = new FileMonitor(reloadInterval);
                monitor.addFile(f);
                monitor.addListener(this);
                logger.debug("Service script is reloadable");
            }
            else
            {
                logger.warn("Cannot setup autoreload as the script file is not on the local file system");
            }
        }
        return LifecycleTransitionResult.OK;
    }

    protected URL getScriptUrl(String scriptLocation)
    {
        File f = FileUtils.newFile(scriptLocation);
        if (f.exists())
        {
            try
            {
                return f.toURL();
            }
            catch (MalformedURLException e)
            {
                logger.error("Failed to create URL from file: " + f.getAbsolutePath(), e);
                return null;
            }
        }
        else
        {
            return ClassUtils.getResource(scriptLocation, getClass());
        }
    }

    public String getScript()
    {
        return script;
    }

    public void setScript(String script)
    {
        this.script = script;
    }

    public boolean isAutoReload()
    {
        return autoReload;
    }

    public void setAutoReload(boolean autoReload)
    {
        this.autoReload = autoReload;
    }

    public LifecycleTransitionResult start() throws MuleException
    {
        if (monitor != null)
        {
            monitor.start();
        }
        return LifecycleTransitionResult.OK;
    }

    public LifecycleTransitionResult stop() throws MuleException
    {
        if (monitor != null)
        {
            monitor.stop();
        }
        return LifecycleTransitionResult.OK;
    }

    public void dispose()
    {
        try
        {
            stop();
        }
        catch (MuleException e)
        {
            logger.error(e.getMessage(), e);
        }
    }

    public String getScriptText()
    {
        return scriptText;
    }

    public void setScriptText(String scriptText)
    {
        this.scriptText = scriptText;
    }

    protected abstract void loadInterpreter(URL script) throws InitialisationException;

    protected abstract void loadInterpreter(String scriptText) throws InitialisationException;

    protected abstract String getDefaultFileExtension();
}
