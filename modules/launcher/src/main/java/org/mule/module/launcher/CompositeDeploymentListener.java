/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CompositeDeploymentListener implements DeploymentListener
{

    private transient final Log logger = LogFactory.getLog(getClass());

    private List<DeploymentListener> deploymentListeners = new CopyOnWriteArrayList<DeploymentListener>();

    public void addDeploymentListener(DeploymentListener listener)
    {
        this.deploymentListeners.add(listener);
    }

    public void removeDeploymentListener(DeploymentListener listener)
    {
        this.deploymentListeners.remove(listener);
    }

    @Override
    public void onDeploymentStart(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onDeploymentStart(appName);
            }
            catch (Throwable t)
            {
                logNotificationProcessingError(appName, listener, "onDeploymentStart", t);
            }
        }
    }

    @Override
    public void onDeploymentSuccess(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onDeploymentSuccess(appName);
            }
            catch (Throwable t)
            {
                logNotificationProcessingError(appName, listener, "onDeploymentSuccess", t);
            }
        }
    }

    @Override
    public void onDeploymentFailure(String appName, Throwable cause)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onDeploymentFailure(appName, cause);
            }
            catch (Throwable t)
            {
                logNotificationProcessingError(appName, listener, "onDeploymentFailure", t);
            }
        }
    }

    @Override
    public void onUndeploymentStart(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onUndeploymentStart(appName);
            }
            catch (Throwable t)
            {
                logNotificationProcessingError(appName, listener, "onUndeploymentStart", t);
            }
        }
    }

    @Override
    public void onUndeploymentSuccess(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onUndeploymentSuccess(appName);
            }
            catch (Throwable t)
            {
                logNotificationProcessingError(appName, listener, "onUndeploymentSuccess", t);
            }
        }
    }

    @Override
    public void onUndeploymentFailure(String appName, Throwable cause)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onUndeploymentFailure(appName, cause);
            }
            catch (Throwable t)
            {
                logNotificationProcessingError(appName, listener, "onUndeploymentFailure", t);
            }
        }
    }

    private void logNotificationProcessingError(String appName, DeploymentListener listener, String notification, Throwable error)
    {
        logger.error(String.format("Listener '%s' failed to process notification '%s' for application '%s'", listener, notification, appName), error);
    }
}
