/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifactapi.classloader;

import org.mule.api.annotation.NoImplement;

@NoImplement
public interface DisposableClassLoader {

  /**
   * Gets rid of the class loader resources.
   */
  void dispose();
}
