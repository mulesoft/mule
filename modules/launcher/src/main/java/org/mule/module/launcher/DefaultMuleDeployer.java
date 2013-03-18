/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.api.lifecycle.Stoppable;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;

import java.beans.Introspector;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleDeployer implements MuleDeployer
{

    protected transient final Log logger = LogFactory.getLog(getClass());

    protected ApplicationFactory applicationFactory;

    public void setApplicationFactory(ApplicationFactory applicationFactory)
    {
        this.applicationFactory = applicationFactory;
    }

    public void deploy(Application app)
    {
        try
        {
            app.install();
            app.init();
            app.start();
        }
        catch (Throwable t)
        {
            app.dispose();

            if (t instanceof DeploymentException)
            {
                // re-throw as is
                throw ((DeploymentException) t);
            }

            final String msg = String.format("Failed to deploy application [%s]", app.getAppName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    public void undeploy(Application app)
    {
        try
        {
            tryToStopApp(app);
            tryToDisposeApp(app);

            final File appDir = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), app.getAppName());
            FileUtils.deleteDirectory(appDir);
            // remove a marker, harmless, but a tidy app dir is always better :)
            File marker = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), String.format("%s-anchor.txt", app.getAppName()));
            marker.delete();
            Introspector.flushCaches();
        }
        catch (Throwable t)
        {
            if (t instanceof DeploymentException)
            {
                // re-throw as is
                throw ((DeploymentException) t);
            }

            final String msg = String.format("Failed to undeploy application [%s]", app.getAppName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    private void tryToDisposeApp(Application app)
    {
        try
        {
            app.dispose();
        }
        catch (Throwable t)
        {
            logger.error(String.format("Unable to cleanly dispose application '%s'. Restart Mule if you get errors redeploying this application", app.getAppName()), t);
        }
    }

    private void tryToStopApp(Application app)
    {
        if (app.getMuleContext() == null || !app.getMuleContext().getLifecycleManager().isDirectTransition(Stoppable.PHASE_NAME))
        {
            return;
        }

        try
        {
            app.stop();
        }
        catch (Throwable t)
        {
            logger.error(String.format("Unable to cleanly stop application '%s'. Restart Mule if you get errors redeploying this application", app.getAppName()), t);
        }
    }

    public Application installFromAppDir(String packedMuleAppFileName) throws IOException
    {
        final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
        File appFile = new File(appsDir, packedMuleAppFileName);

        // basic security measure: outside apps dir use installFrom(url) and go through any
        // restrictions applied to it
        if (!appFile.getParentFile().equals(appsDir))
        {
            throw new SecurityException("installFromAppDir() can only deploy from $MULE_HOME/apps. Use installFrom(url) instead.");
        }

        return installFrom(appFile.toURL());
    }

    public Application installFrom(URL url) throws IOException
    {
        if (applicationFactory == null)
        {
           throw new IllegalStateException("There is no application factory");
        }

        // TODO plug in app-bloodhound/validator here?
        if (!url.toString().endsWith(".zip"))
        {
            throw new IllegalArgumentException("Invalid Mule application archive: " + url);
        }

        final String baseName = FilenameUtils.getBaseName(url.toString());
        if (baseName.contains("%20"))
        {
            throw new DeploymentInitException(
                    MessageFactory.createStaticMessage("Mule application name may not contain spaces: " + baseName));
        }

        String appName;
        File appDir = null;
        boolean errorEncountered = false;
        try
        {
            final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();

            final String fullPath = url.toURI().toString();

            if (logger.isInfoEnabled())
            {
                logger.info("Exploding a Mule application archive: " + fullPath);
            }

            appName = FilenameUtils.getBaseName(fullPath);
            appDir = new File(appsDir, appName);
            // normalize the full path + protocol to make unzip happy
            final File source = new File(url.toURI());

            FileUtils.unzip(source, appDir);
            if ("file".equals(url.getProtocol()))
            {
                FileUtils.deleteQuietly(source);
            }
        }
        catch (URISyntaxException e)
        {
            errorEncountered = true;
            final IOException ex = new IOException(e.getMessage());
            ex.fillInStackTrace();
            throw ex;
        }
        catch (IOException e)
        {
            errorEncountered = true;
            // re-throw
            throw e;
        }
        catch (Throwable t)
        {
            errorEncountered = true;
            final String msg = "Failed to install app from URL: " + url;
            throw new DeploymentInitException(MessageFactory.createStaticMessage(msg), t);
        }
        finally
        {
            // delete an app dir, as it's broken
            if (errorEncountered && appDir != null && appDir.exists())
            {
                final boolean couldNotDelete = FileUtils.deleteTree(appDir);
                /*
                if (couldNotDelete)
                {
                    final String msg = String.format("Couldn't delete app directory '%s' after it failed to install", appDir);
                    logger.error(msg);
                }
                */
            }
        }

        // appname is never null by now
        return applicationFactory.createApp(appName);
    }
}
