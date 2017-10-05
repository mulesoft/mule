/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

import static org.mule.metadata.api.utils.MetadataTypeUtils.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppDataFolder;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import java.io.File;
import java.net.URL;

/**
 * Creates {@link NativeLibraryFinder}
 */
public class DefaultNativeLibraryFinderFactory implements NativeLibraryFinderFactory {

  @Override
  public NativeLibraryFinder create(String name, URL[] urls) {
    checkArgument(!isEmpty(name), "appName cannot be empty");
    checkArgument(urls != null, "urls cannot be null");

    return new ArtifactCopyNativeLibraryFinder(new File(getAppDataFolder(name), "temp"), urls);
  }

}
