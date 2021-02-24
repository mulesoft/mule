/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static com.google.common.base.Optional.absent;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.mule.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.module.launcher.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.module.launcher.DeploymentPropertiesUtils.resolveDeploymentProperties;

import org.mule.ArtifactStoppedPersistenceListener;
import org.mule.DefaultMuleContext;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.Registry;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.artifact.Artifact;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import com.google.common.base.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultArtifactDeployer<T extends Artifact> implements ArtifactDeployer
{

    protected transient final Log logger = LogFactory.getLog(getClass());

    public void undeploy(Artifact artifact)
    {
        try
        {
            doNotPersistArtifactStop(artifact);
            tryToStopArtifact(artifact);
            tryToDisposeArtifact(artifact);
        }
        catch (Throwable t)
        {
            if (t instanceof DeploymentException)
            {
                throw ((DeploymentException) t);
            }

            final String msg = format("Failed to undeployArtifact artifact [%s]", artifact.getArtifactName());
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
            logger.error(format("Unable to cleanly dispose artifact '%s'. Restart Mule if you get errors redeploying this artifact", artifact.getArtifactName()), t);
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
            logger.error(format("Unable to cleanly stop artifact '%s'. Restart Mule if you get errors redeploying this artifact", artifact.getArtifactName()), t);
        }
    }

    @Override
    public void deploy(Artifact artifact, boolean startArtifact)
    {
        try
        {
            artifact.install();
            artifact.init();
            if (shouldStartArtifact(artifact))
            {
                artifact.start();
            }
            if (artifact.getMuleContext() != null && artifact.getMuleContext().getRegistry() != null)
            {
                ArtifactStoppedPersistenceListener artifactStoppedDeploymentListener =
                    new ArtifactStoppedDeploymentPersistenceListener(artifact.getArtifactName());
                DefaultMuleContext defaultMuleContext = (DefaultMuleContext) artifact.getMuleContext();
                MuleRegistry muleRegistry = defaultMuleContext.getRegistry();
                muleRegistry.registerObject(ARTIFACT_STOPPED_LISTENER, artifactStoppedDeploymentListener);
            }
        }
        catch (Throwable t)
        {
            artifact.dispose();

            if (t instanceof DeploymentException)
            {
                throw ((DeploymentException) t);
            }

            final String msg = format("Failed to deploy artifact [%s]", artifact.getArtifactName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    @Override
    public void deploy(Artifact artifact)
    {
        deploy(artifact, true);
    }

    private Boolean shouldStartArtifact(Artifact artifact)
    {
        Properties deploymentProperties = null;
        try
        {
            Optional<Properties> properties = absent();
            deploymentProperties = resolveDeploymentProperties(artifact.getArtifactName(), properties);
        }
        catch (IOException e)
        {
            logger.error(format("Failed to load deployment property for artifact %s",
                artifact.getArtifactName()), e);
        }
        return deploymentProperties != null
            && parseBoolean(deploymentProperties.getProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, "true"));
    }

    public void doNotPersistArtifactStop(Artifact artifact)
    {
        if (artifact.getMuleContext() == null || artifact.getMuleContext().getRegistry() == null)
        {
            return;
        }
        Registry artifactRegistry = artifact.getMuleContext().getRegistry();
        Collection<ArtifactStoppedPersistenceListener> listeners =
            artifactRegistry.lookupObjects(ArtifactStoppedPersistenceListener.class);
        for (ArtifactStoppedPersistenceListener artifactStoppedPersistenceListener : listeners)
        {
            artifactStoppedPersistenceListener.doNotPersist();
        }
    }

}
