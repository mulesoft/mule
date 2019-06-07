/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import java.io.File;
import java.net.URISyntaxException;

public class MavenClassLoaderModelLoaderTestCase {

  protected MavenClassLoaderModelLoader mavenClassLoaderModelLoader = new MavenClassLoaderModelLoader();

  protected File getApplicationFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
