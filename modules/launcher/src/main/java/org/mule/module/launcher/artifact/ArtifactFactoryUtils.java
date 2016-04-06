/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.FileUtils;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Utility class providing useful methods when creating {@link Artifact}s.
 */
public class ArtifactFactoryUtils
{

    private ArtifactFactoryUtils()
    {
    }

    /**
     * Finds the deployment file within a given artifact.
     *
     * @param artifactDir the artifact directory where the deployment file should be present
     * @param artifactName the artifact whose deployment file is needed
     * @param pattern the pattern the deployment file should match
     * @return the artifact's deployment file or {@code null} if none was found
     */
    public static File getDeploymentFile(File artifactDir, String artifactName, String pattern)
    {
        if (!artifactDir.exists())
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage(
                            String.format("Artifact directory does not exist: '%s'", artifactDir)));
        }
        // list mule-deploy.* files
        @SuppressWarnings("unchecked")
        Collection<File> deployFiles = FileUtils.listFiles(artifactDir, new WildcardFileFilter(pattern), null);
        if (deployFiles.size() > 1)
        {
            // TODO need some kind of an InvalidAppFormatException
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage(
                            String.format("More than one mule-deploy descriptors found in artifact '%s'", artifactName)));
        }

        if (deployFiles.isEmpty())
        {
            return null;
        }
        else
        {
            return deployFiles.iterator().next();
        }
    }

}
