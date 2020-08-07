/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.artifact;

import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.descriptor.ArtifactDescriptor;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ArtifactWrapperTestCase
{
    @Test
    public void testCancelStart() throws IOException
    {
        // Given an artifact wrapper
        ArtifactSubtype artifact = mock(ArtifactSubtype.class);
        ArtifactWrapper<ArtifactSubtype, ArtifactSubtypeDescriptor> artifactWrapper = new ArtifactWrapper<>(artifact);

        // When cancelling start
        artifactWrapper.cancelStart();

        // Then the delegate is told to cancel start
        verify(artifact).cancelStart();
    }

    @Test
    public void testCancelStartWhenStopping() throws IOException
    {
        // Given an artifact wrapper
        ArtifactSubtype artifact = mock(ArtifactSubtype.class);
        ArtifactWrapper<ArtifactSubtype, ArtifactSubtypeDescriptor> artifactWrapper = new ArtifactWrapper<>(artifact);

        // When cancelling start
        artifactWrapper.stop();

        // Then the delegate is told to cancel start
        verify(artifact).cancelStart();
    }

    private class ArtifactSubtype implements Artifact<ArtifactSubtypeDescriptor>
    {
        @Override
        public void install() throws InstallException
        {

        }

        @Override
        public void init()
        {

        }

        @Override
        public void start() throws DeploymentStartException
        {

        }

        @Override
        public void stop()
        {

        }

        @Override
        public void dispose()
        {

        }

        @Override
        public String getArtifactName()
        {
            return null;
        }

        @Override
        public ArtifactSubtypeDescriptor getDescriptor()
        {
            return null;
        }

        @Override
        public File[] getResourceFiles()
        {
            return new File[0];
        }

        @Override
        public ArtifactClassLoader getArtifactClassLoader()
        {
            return null;
        }

        @Override
        public MuleContext getMuleContext()
        {
            return null;
        }

        @Override
        public void cancelStart()
        {

        }
    }

    private class ArtifactSubtypeDescriptor extends ArtifactDescriptor
    {

    }
}