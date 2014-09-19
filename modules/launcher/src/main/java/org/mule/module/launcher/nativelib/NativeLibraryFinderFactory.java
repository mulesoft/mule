/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.nativelib;

/**
 * Creates {@link NativeLibraryFinder} instances
 */
public interface NativeLibraryFinderFactory
{

    /**
     * Creates a ntive library finder for the given application
     *
     * @param appName name of the application owning the finder
     * @return a non null instance
     */
    NativeLibraryFinder create(String appName);
}
