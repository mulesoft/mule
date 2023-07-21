/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

@NoImplement
public interface DisposableClassLoader {

  /**
   * Gets rid of the class loader resources.
   */
  void dispose();

}
