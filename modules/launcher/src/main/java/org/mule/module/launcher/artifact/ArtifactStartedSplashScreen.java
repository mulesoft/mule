/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import static java.util.Arrays.asList;
import org.mule.module.launcher.descriptor.ArtifactDescriptor;
import org.mule.util.SplashScreen;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Base splash screen to log messages when an {@link Artifact} is started based on it's {@link ArtifactDescriptor}.
 *
 * @param <D> the type of {@link ArtifactDescriptor}
 */
public abstract class ArtifactStartedSplashScreen<D extends ArtifactDescriptor> extends SplashScreen
{
    protected abstract void createMessage(D descriptor);

    protected void listOverrides(ArtifactDescriptor descriptor)
    {
        listItems(descriptor.getLoaderOverride(), "Class loader overrides:");
    }

    protected List<String> getLibraries(File artifactLibFolder)
    {
        if (artifactLibFolder.exists())
        {
            String[] libraries = artifactLibFolder.list(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".jar");
                }
            });
            return asList(libraries);
        }
        return new ArrayList<>();
    }
}
