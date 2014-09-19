/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.nativelib;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.MuleFoldersUtil;
import org.mule.util.BackwardsCompatibilityPropertyChecker;

import java.io.File;

/**
 * Creates different {@link NativeLibraryFinder} instances depending on the
 * value of system property {@value #COPY_APPLICATION_NATIVE_LIBRARIES_PROPERTY},
 * which value is true by default.
 */
public class DefaultNativeLibraryFinderFactory implements NativeLibraryFinderFactory
{

    public static final String COPY_APPLICATION_NATIVE_LIBRARIES_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "copyApplicationNativeLibraries";

    public static final BackwardsCompatibilityPropertyChecker COPY_APPLICATION_NATIVE_LIBRARIES = new BackwardsCompatibilityPropertyChecker(COPY_APPLICATION_NATIVE_LIBRARIES_PROPERTY);

    @Override
    public NativeLibraryFinder create(String appName)
    {
        File libDir = MuleFoldersUtil.getAppLibFolder(appName);

        NativeLibraryFinder nativeLibraryFinder;

        if (COPY_APPLICATION_NATIVE_LIBRARIES.isEnabled())
        {
            File perAppNativeLibs = new File(MuleFoldersUtil.getAppTempFolder(appName), MuleFoldersUtil.LIB_FOLDER);

            nativeLibraryFinder = new PerAppCopyNativeLibraryFinder(libDir, perAppNativeLibs);
        }
        else
        {
            nativeLibraryFinder = new PerAppNativeLibraryFinder(libDir);
        }

        return nativeLibraryFinder;
    }

}
