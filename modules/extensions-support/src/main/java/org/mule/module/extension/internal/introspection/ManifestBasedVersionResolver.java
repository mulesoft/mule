/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.annotations.Extension;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Implementation of {@link VersionResolver} that infers an extension's version based on the MANIFEST.MF file contained
 * on it's JAR. If that attempt fails, it fallbacks to searching for the file under target/test-classes.
 *
 * @since 4.0
 */
final class ManifestBasedVersionResolver implements VersionResolver
{

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
            version = fallback();
        }
        if (version == null)
        {
            throw new MuleRuntimeException(createStaticMessage(String.format("Cannot resolve version for extension %s: MANIFEST.MF could not be found.", extension.name())));
        }
        return version;
    }

    private String fallback()
    {
        //look for the location of this class
        String packageName = extensionType.getPackage().getName();
        String classFileName = extensionType.getName().substring(packageName.length() + 1) + ".class";
        String classFilePath = extensionType.getResource(classFileName).toString();
        //get rid of the package part of the path to avoid conflicts with user-defined directories
        String packagePath = packageName.replace(".", "/");
        classFilePath = classFilePath.substring(0, classFilePath.lastIndexOf(packagePath));
        //get the target index since the class could be in /classes too
        int pathIndex = classFilePath.lastIndexOf("/target/");
        //find and load manifest
        String manifestUrl = String.format("%s/target/test-classes/META-INF/MANIFEST.MF", classFilePath.substring(0, pathIndex + 1));
        try (InputStream manifestInputStream = new URL(manifestUrl).openStream())
        {
            Manifest manifest = new Manifest(manifestInputStream);
            return manifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Version"));
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
