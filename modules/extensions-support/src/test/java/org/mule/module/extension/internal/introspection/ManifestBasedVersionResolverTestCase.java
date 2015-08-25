/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.createManifestFileIfNecessary;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getMetaInfDirectory;
import org.mule.config.MuleManifest;
import org.mule.extension.annotations.Extension;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ManifestBasedVersionResolverTestCase extends AbstractMuleTestCase
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Class<?> clazz = HeisenbergExtension.class;
    private VersionResolver versionResolver = new ManifestBasedVersionResolver(clazz);

    @Test
    public void failsWithOutManifest()
    {
        thrown.expectMessage(is("Cannot resolve version for extension heisenberg: MANIFEST.MF could not be found."));
        testResolution();
    }

    @Test
    public void worksWithManifest() throws Exception
    {
        File metaInfDirectory = getMetaInfDirectory(getClass());
        File manifest = createManifestFileIfNecessary(metaInfDirectory);
        try
        {
            assertThat(testResolution(), is(MuleManifest.getProductVersion()));
        }
        finally
        {
            if (manifest.exists())
            {
                FileUtils.deleteQuietly(manifest);
            }
        }

    }

    private String testResolution()
    {
        return versionResolver.resolveVersion(clazz.getAnnotation(Extension.class));
    }
}
