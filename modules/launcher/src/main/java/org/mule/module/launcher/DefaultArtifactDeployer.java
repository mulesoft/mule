/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.artifact.Artifact;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultArtifactDeployer<T extends Artifact> implements ArtifactDeployer
{

    protected transient final Log logger = LogFactory.getLog(getClass());

    public void undeploy(Artifact artifact)
    {
        try
        {
            tryToStopArtifact(artifact);
            tryToDisposeArtifact(artifact);
        }
        catch (Throwable t)
        {
            if (t instanceof DeploymentException)
            {
                throw ((DeploymentException) t);
            }

            final String msg = String.format("Failed to undeployArtifact artifact [%s]", artifact.getArtifactName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    private void tryToDisposeArtifact(Artifact artifact)
    {
        try
        {
            artifact.dispose();
        }
        catch (Throwable t)
        {
            logger.error(String.format("Unable to cleanly dispose artifact '%s'. Restart Mule if you get errors redeploying this artifact", artifact.getArtifactName()), t);
        }
    }

    private void tryToStopArtifact(Artifact artifact)
    {

        try
        {
            artifact.stop();
        }
        catch (Throwable t)
        {
            logger.error(String.format("Unable to cleanly stop artifact '%s'. Restart Mule if you get errors redeploying this artifact", artifact.getArtifactName()), t);
        }
    }

    @Override
    public void deploy(Artifact artifact, boolean startArtifact)
    {
        try
        {
            artifact.install();
            artifact.init();
            if (startArtifact)
            {
                artifact.start();
            }
        }
        catch (Throwable t)
        {
            artifact.dispose();

            if (t instanceof DeploymentException)
            {
                throw ((DeploymentException) t);
            }

            final String msg = String.format("Failed to deploy artifact [%s]", artifact.getArtifactName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    @Override
    public void deploy(Artifact artifact)
    {
        deploy(artifact, true);
    }

}
