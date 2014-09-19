/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.nativelib;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.MuleFoldersUtil;

import java.io.File;

/**
 * Creates different {@link NativeLibraryFinder} instances depending on the
 * value of system property {@value #COPY_APPLICATION_NATIVE_LIBRARIES},
 * which value is true by default.
 */
public class DefaultNativeLibraryFinderFactory implements NativeLibraryFinderFactory
{

    public static final String COPY_APPLICATION_NATIVE_LIBRARIES = MuleProperties.SYSTEM_PROPERTY_PREFIX + "copyApplicationNativeLibraries";

    @Override
    public NativeLibraryFinder create(String appName)
    {
        File libDir = MuleFoldersUtil.getAppLibFolder(appName);

        NativeLibraryFinder nativeLibraryFinder;
        if (isCopyLibraries())
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

    private boolean isCopyLibraries()
    {
        String property = System.getProperty(COPY_APPLICATION_NATIVE_LIBRARIES, "true");
        return Boolean.parseBoolean(property);
    }
}
