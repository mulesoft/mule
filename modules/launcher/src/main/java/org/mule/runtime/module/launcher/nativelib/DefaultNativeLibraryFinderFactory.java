/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.nativelib;

import org.mule.runtime.module.launcher.MuleFoldersUtil;

/**
 * Creates {@link NativeLibraryFinder}
 */
public class DefaultNativeLibraryFinderFactory implements NativeLibraryFinderFactory {

  @Override
  public NativeLibraryFinder create(String appName) {
    return new PerAppNativeLibraryFinder(MuleFoldersUtil.getAppLibFolder(appName));
  }

}
