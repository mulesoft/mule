/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
