/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.artifact.ArtifactFactory;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;

import java.beans.Introspector;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleDeployer<T extends Artifact> implements MuleDeployer
{

    protected static final String ANCHOR_FILE_BLURB = "Delete this file while Mule is running to undeploy this artifact in a clean way.";
    protected transient final Log logger = LogFactory.getLog(getClass());

    protected ArtifactFactory<T> artifactFactory;

    public void setArtifactFactory(ArtifactFactory<T> artifactFactory)
    {
        this.artifactFactory = artifactFactory;
    }

    public void deploy(Artifact artifact)
    {
        try
        {
            artifact.install();
            artifact.init();
            artifact.start();
            // save artifact's state in the marker file
            File marker = getArtifactMarkerFile(artifact);
            FileUtils.writeStringToFile(marker, ANCHOR_FILE_BLURB);
        }
        catch (Throwable t)
        {
            artifact.dispose();

            if (t instanceof DeploymentException)
            {
                // re-throw as is
                throw ((DeploymentException) t);
            }

            final String msg = String.format("Failed to deploy artifact [%s]", artifact.getArtifactName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    public void undeploy(Artifact artifact)
    {
        try
        {
            tryToStopApp(artifact);
            tryToDisposeApp(artifact);

            final File artifactDir = new File(artifactFactory.getArtifactDir(), artifact.getArtifactName());
            FileUtils.deleteDirectory(artifactDir);
            // remove a marker, harmless, but a tidy artifact dir is always better :)
            File marker = getArtifactMarkerFile(artifact);
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

            final String msg = String.format("Failed to undeploy artifact [%s]", artifact.getArtifactName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    private void tryToDisposeApp(Artifact artifact)
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

    private void tryToStopApp(Artifact artifact)
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

    public T installFromDir(String packedArtifactName) throws IOException
    {
        final File artifactsDir = artifactFactory.getArtifactDir();
        File artifactFile = new File(artifactsDir, packedArtifactName);

        // basic security measure: outside artifacts dir use installFrom(url) and go through any
        // restrictions applied to it
        if (!artifactFile.getParentFile().equals(artifactsDir))
        {
            throw new SecurityException("installFromAppDir() can only deploy from $MULE_HOME/" + artifactFactory.getArtifactDir() + ". Use installFrom(url) instead.");
        }

        return installFrom(artifactFile.toURL());
    }

    public T installFrom(URL url) throws IOException
    {
        if (artifactFactory == null)
        {
            throw new IllegalStateException("There is no artifact factory");
        }

        // TODO plug in app-bloodhound/validator here?
        if (!url.toString().endsWith(".zip"))
        {
            throw new IllegalArgumentException("Invalid Mule artifact archive: " + url);
        }

        final String baseName = FilenameUtils.getBaseName(url.toString());
        if (baseName.contains("%20"))
        {
            throw new DeploymentInitException(
                    MessageFactory.createStaticMessage("Mule artifact name may not contain spaces: " + baseName));
        }

        String artifactName;
        File artifactDir = null;
        boolean errorEncountered = false;
        try
        {
            final File aritfactsDir = artifactFactory.getArtifactDir();

            final String fullPath = url.toURI().toString();

            if (logger.isInfoEnabled())
            {
                logger.info("Exploding a Mule artifact archive: " + fullPath);
            }

            artifactName = FilenameUtils.getBaseName(fullPath);
            artifactDir = new File(aritfactsDir, artifactName);
            // normalize the full path + protocol to make unzip happy
            final File source = new File(url.toURI());

            FileUtils.unzip(source, artifactDir);
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
            final String msg = "Failed to install artifact from URL: " + url;
            throw new DeploymentInitException(MessageFactory.createStaticMessage(msg), t);
        }
        finally
        {
            // delete an artifact dir, as it's broken
            if (errorEncountered && artifactDir != null && artifactDir.exists())
            {
                final boolean couldNotDelete = FileUtils.deleteTree(artifactDir);
                /*
                if (couldNotDelete)
                {
                    final String msg = String.format("Couldn't delete artifact directory '%s' after it failed to install", aritfactDir);
                    logger.error(msg);
                }
                */
            }
        }

        // artifactName is never null by now
        return artifactFactory.createArtifact(artifactName);
    }

    private File getArtifactMarkerFile(Artifact artifact)
    {
        return new File(artifactFactory.getArtifactDir(), String.format("%s%s", artifact.getArtifactName(), MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX));
    }
}
