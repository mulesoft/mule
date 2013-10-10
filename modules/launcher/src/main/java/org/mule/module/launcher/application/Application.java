/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.application;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;

public interface Application
{
    void install() throws InstallException;

    void init();

    void start() throws DeploymentStartException;

    void stop();

    void dispose();

    void redeploy();

    MuleContext getMuleContext();

    /**
     * @return a classloader associated with this deployment 
     */
    ClassLoader getDeploymentClassLoader();

    String getAppName();

    ApplicationDescriptor getDescriptor();
}
