/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;

import java.io.File;

/**
 * An Artifact is an abstract representation of a deployable unit within the mule container.
 */
public interface Artifact
{

    /**
     * Install the artifact. Most commonly this includes the creation of the class loader and validation of resources.
     */
    void install() throws InstallException;

    /**
     * Initialise the artifact resources
     */
    void init();

    /**
     * Starts the artifact execution
     */
    void start() throws DeploymentStartException;

    /**
     * Stops the artifact execution
     */
    void stop();

    /**
     * Dispose the artifact. Most commonly this includes the release of the resources held by the artifact
     */
    void dispose();

    /**
     * @return the artifact identifier
     */
    String getArtifactName();

    /**
     * @return an array with the configuration files of the artifact. Never returns null.
     *         If there's no configuration file then returns an empty array.
     */
    File[] getResourceFiles();

    /**
     * @return class loader responsible for loading resources for this artifact.
     */
    ArtifactClassLoader getArtifactClassLoader();

    /**
     * @return MuleContext created from the artifact configurations files.
     */
    MuleContext getMuleContext();
}
