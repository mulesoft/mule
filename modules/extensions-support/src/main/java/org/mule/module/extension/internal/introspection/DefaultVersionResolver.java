/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Default implementation of {@link VersionResolver}, it fallbacks to searching for the manifest file
 * under target/test-classes.
 */
final class DefaultVersionResolver extends VersionResolver
{

    @Override
    public String fallback(Class extensionType)
    {
        try
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
            Manifest manifest = new Manifest(new URL(manifestUrl).openStream());
            return manifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Version"));
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
