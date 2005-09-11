/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.components.script;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.UMODescriptorAware;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.util.ClassHelper;
import org.mule.util.monitor.FileListener;
import org.mule.util.monitor.FileMonitor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <code>AbstractScriptComponent</code> is a component that can execute
 * scripts as components in Mule. This component also supports reloading if the
 * script file changes (providing the file is on the file system)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractScriptComponent implements Initialisable, Lifecycle, UMODescriptorAware, FileListener,
        Callable
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private String script = null;
    private String scriptText = null;

    private boolean autoReload = true;
    protected UMODescriptor descriptor;
    private FileMonitor monitor;
    private long reloadInterval = 60000;

    public void setDescriptor(UMODescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    public void initialise() throws InitialisationException
    {
        if (getScript() == null && getScriptText()== null) {
            String extension = getDefaultFileExtension();
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            setScript(descriptor.getName() + extension);
            logger.info("script name is not set, using default: " + descriptor.getName() + extension);
        }

        if(getScriptText()!=null) {
            loadInterpreter(getScriptText());
        } else {
            // load script before creating a file monitor so that the script name
            // can be monified
            loadInterpreter(getScriptUrl(getScript()));
        }
        if (autoReload) {
            File f = new File(getScript());
            if (f.exists()) {
                monitor = new FileMonitor(reloadInterval);
                monitor.addFile(f);
                monitor.addListener(this);
                logger.debug("Component script is reloadable");
            } else {
                logger.warn("Cannot setup autoreload as the script fie is not on the local file system");
            }
        }
    }

    protected URL getScriptUrl(String scriptLocation)
    {
        File f = new File(scriptLocation);
        if (f.exists()) {
            try {
                return f.toURL();
            } catch (MalformedURLException e) {
                logger.error("Failed to create URL from file: " + f.getAbsolutePath(), e);
                return null;
            }
        } else {
            return ClassHelper.getResource(scriptLocation, getClass());
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

    public void start() throws UMOException
    {
        if (monitor != null) {
            monitor.start();
        }
    }

    public void stop() throws UMOException
    {
        if (monitor != null) {
            monitor.stop();
        }
    }

    public void dispose()
    {
        try {
            stop();
        } catch (UMOException e) {
            logger.error(e.getMessage(), e);
        }
    }


    public String getScriptText() {
        return scriptText;
    }

    public void setScriptText(String scriptText) {
        this.scriptText = scriptText;
    }

    protected abstract void loadInterpreter(URL script) throws InitialisationException;

    protected abstract void loadInterpreter(String scriptText) throws InitialisationException;

    protected abstract String getDefaultFileExtension();
}
