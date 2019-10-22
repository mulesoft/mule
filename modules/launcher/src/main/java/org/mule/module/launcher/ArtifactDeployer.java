/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.artifact.Artifact;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Deployes and Undeploys artifacts in the container.
 *
 * @param <T> artifact type
 */
public interface ArtifactDeployer<T extends Artifact>
{

    /**
     * Deploys an artifact.
     *
     * The deployer executes the artifact installation phases until the artifact is deployed After this method call the Artifact
     * will be installed in the container and started.
     *  @param artifact artifact to be deployed
     *  @param startArtifact whether the artifact should be started after initialisation
     */
    void deploy(final T artifact, boolean startArtifact);

    /**
     * Deploys an artifact.
     *
     * The deployer executes the artifact installation phases until the artifact is deployed After this method call the Artifact
     * will be installed in the container and started.
     *  @param artifact artifact to be deployed
     */
    void deploy(final T artifact);

    /**
     * Undeploys an artifact.
     *
     * The deployer executes the artifact desinstallation
     * phases until de artifact is undeployed.
     * After this method call the Artifact will not longer be running inside
     * the container.
     *
     * @param artifact artifact to be undeployed
     */
    void undeploy(final T artifact);

}
