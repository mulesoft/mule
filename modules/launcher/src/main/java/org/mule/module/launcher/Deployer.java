/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.api.MuleContext;

/**
 *
 * @param <M> - meta data class type
 */
public interface Deployer<M>
{
    void install() throws InstallException;

    void init();

    void start() throws DeploymentStartException;

    void stop();

    void dispose();

    void redeploy();

    void setMetaData(M metaData);

    M getMetaData();

    MuleContext getMuleContext();

    /**
     * @return a classloader associated with this deployment 
     */
    ClassLoader getDeploymentClassLoader();
}
