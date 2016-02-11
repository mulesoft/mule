/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.config.MuleManifest.getProductVersion;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.createManifestFileIfNecessary;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getMetaInfDirectory;
import static org.mule.util.IOUtils.getResourceAsStream;
import org.mule.extension.annotation.api.Extension;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.introspection.VersionResolver;
import org.mule.module.extension.internal.introspection.describer.ManifestBasedVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.InputStream;
import java.util.jar.Manifest;

import org.junit.Test;

public class ManifestBasedVersionResolverTestCase extends AbstractMuleTestCase
{

    private static final String TEST_MANIFEST_VERSION = "1.0.0-test";
    private Class<?> clazz = HeisenbergExtension.class;
    private VersionResolver versionResolver = new ManifestBasedVersionResolver(clazz);

    @Test
    public void worksWithoutManifest()
    {
        assertThat(testResolution(), is(getProductVersion()));
    }

    @Test
    public void worksWithManifest() throws Exception
    {
        File metaInfDirectory = getMetaInfDirectory(getClass());
        InputStream testManifestInputStream = getResourceAsStream("test-manifest.mf", getClass());
        File manifest = createManifestFileIfNecessary(metaInfDirectory, new Manifest(testManifestInputStream));
        try
        {
            assertThat(testResolution(), is(TEST_MANIFEST_VERSION));
        }
        finally
        {
            if (manifest.exists())
            {
                deleteQuietly(manifest);
            }
            closeQuietly(testManifestInputStream);
        }

    }

    private String testResolution()
    {
        return versionResolver.resolveVersion(clazz.getAnnotation(Extension.class));
    }
}
