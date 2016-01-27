/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import org.mule.config.MuleManifest;
import org.mule.extension.annotation.api.Extension;
import org.mule.module.extension.internal.introspection.VersionResolver;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link VersionResolver} that infers an extension's version based on the MANIFEST.MF file contained
 * on it's JAR. If that attempt fails, it fallbacks to searching for the file under target/test-classes. If the file can
 * not be found, it uses the version from {@link org.mule.config.MuleManifest}.
 *
 * @since 4.0
 */
final class ManifestBasedVersionResolver implements VersionResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestBasedVersionResolver.class);
    private final Class extensionType;

    public ManifestBasedVersionResolver(Class extensionType)
    {
        this.extensionType = extensionType;
    }

    @Override
    public String resolveVersion(Extension extension)
    {
        String version = extensionType.getPackage().getImplementationVersion();
        if (version == null)
        {
            LOGGER.debug("Could not resolve version from JAR's MANIFEST.MF. Searching for file under target/test-classes.");
            version = fallback();
        }
        return version;
    }

    private String fallback()
    {
        String manifestUrl = ClassUtils.getPathURL(extensionType) + "/target/test-classes/META-INF/MANIFEST.MF";
        try (InputStream manifestInputStream = new URL(manifestUrl).openStream())
        {
            Manifest manifest = new Manifest(manifestInputStream);
            return manifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Version"));
        }
        catch (IOException e)
        {
            LOGGER.debug("Could not find MANIFEST.MF under target/test-classes. Using mule-core MANIFEST.MF.");
            return MuleManifest.getProductVersion();
        }
    }
}
