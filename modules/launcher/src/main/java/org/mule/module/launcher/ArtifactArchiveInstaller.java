/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.module.launcher.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;
import static org.mule.util.FileUtils.deleteTree;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;

import java.beans.Introspector;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Installer for mule artifacts inside the mule container directories.
 */
public class ArtifactArchiveInstaller
{

    protected static final String ANCHOR_FILE_BLURB = "Delete this file while Mule is running to remove the artifact in a clean way.";

    protected transient final Log logger = LogFactory.getLog(getClass());

    private final File artifactParentDir;

    public ArtifactArchiveInstaller(File artifactParentDir)
    {
        this.artifactParentDir = artifactParentDir;
    }

    /**
     * Installs an artifact in the mule container.
     *
     * Created the artifact directory and the anchor file related.
     *
     * @param artifactUrl URL of the artifact to install. It must be present in the artifact directory as a zip file.
     * @return the name of the installed artifact.
     * @throws IOException in case there was an error reading from the artifact or writing to the artifact folder.
     */
    public String installArtifact(final URL artifactUrl) throws IOException
    {
        if (!artifactUrl.toString().toLowerCase().endsWith(ZIP_FILE_SUFFIX))
        {
            throw new IllegalArgumentException("Invalid Mule artifact archive: " + artifactUrl);
        }

        final String baseName = FilenameUtils.getBaseName(artifactUrl.toString());
        if (baseName.contains("%20"))
        {
            throw new DeploymentInitException(
                    MessageFactory.createStaticMessage("Mule artifact name may not contain spaces: " + baseName));
        }

        File artifactDir = null;
        boolean errorEncountered = false;
        String artifactName;
        try
        {
            final String fullPath = artifactUrl.toURI().toString();

            if (logger.isInfoEnabled())
            {
                logger.info("Exploding a Mule artifact archive: " + fullPath);
            }

            artifactName = FilenameUtils.getBaseName(fullPath);
            artifactDir = new File(artifactParentDir, artifactName);

            // Removes previous deployed artifact
            if (artifactDir.exists() && !deleteTree(artifactDir)) {
                throw new IOException("Cannot delete existing folder " + artifactDir);
            }

            // normalize the full path + protocol to make unzip happy
            final File source = new File(artifactUrl.toURI());

            FileUtils.unzip(source, artifactDir);
            if ("file".equals(artifactUrl.getProtocol()))
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
            throw e;
        }
        catch (Throwable t)
        {
            errorEncountered = true;
            final String msg = "Failed to install artifact from URL: " + artifactUrl;
            throw new DeploymentInitException(MessageFactory.createStaticMessage(msg), t);
        }
        finally
        {
            // delete an artifact dir, as it's broken
            if (errorEncountered && artifactDir != null && artifactDir.exists())
            {
                deleteTree(artifactDir);
            }
        }
        return artifactName;
    }

    /**
     * Desintalls an artifact from the mule container installation.
     *
     * It will remove the artifact folder and the anchor file related
     * @param artifactName name of the artifact to be uninstall.
     */
    public void desinstallArtifact(final String artifactName)
    {
        try
        {
            final File artifactDir = new File(artifactParentDir, artifactName);
            FileUtils.deleteDirectory(artifactDir);
            // remove a marker, harmless, but a tidy artifact dir is always better :)
            File marker = getArtifactAnchorFile(artifactName);
            marker.delete();
            Introspector.flushCaches();
        }
        catch (Throwable t)
        {
            if (t instanceof DeploymentException)
            {
                throw ((DeploymentException) t);
            }

            final String msg = String.format("Failed to undeployArtifact artifact [%s]", artifactName);
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    private File getArtifactAnchorFile(String artifactName)
    {
        return new File(artifactParentDir, String.format("%s%s", artifactName, MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX));
    }

    public void createAnchorFile(String artifactName) throws IOException
    {
        // save artifact's state in the marker file
        File marker = getArtifactAnchorFile(artifactName);
        FileUtils.writeStringToFile(marker, ANCHOR_FILE_BLURB);
    }

}
