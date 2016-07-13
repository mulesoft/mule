/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.plugin;

import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.mule.runtime.core.util.FileUtils.unzip;

import java.io.File;
import java.io.IOException;

/**
 * Loads a {@link ArtifactPluginDescriptor} from a file resource.
 *
 * @since 4.0
 */
public class ArtifactPluginDescriptorLoader
{

    private final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory;

    public ArtifactPluginDescriptorLoader(ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory)
    {
        this.artifactPluginDescriptorFactory = artifactPluginDescriptorFactory;
    }

    public ArtifactPluginDescriptor load(File pluginZip, File unpackDestination) throws IOException
    {
        final String pluginName = removeEnd(pluginZip.getName(), ".zip");
        // must unpack as there's no straightforward way for a ClassLoader to use a jar within another jar/zip
        final File tmpDir = new File(unpackDestination, pluginName);
        unzip(pluginZip, tmpDir);
        return artifactPluginDescriptorFactory.create(tmpDir);
    }

}
