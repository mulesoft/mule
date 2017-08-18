/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java;

import org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;

/**
 * Loads an extension by introspecting a class which uses the Extensions API annotations
 *
 * @since 4.0
 */
public class DefaultJavaExtensionModelLoader extends AbstractJavaExtensionModelLoader {

  public static final String JAVA_LOADER_ID = "java";

  public DefaultJavaExtensionModelLoader() {
    super(JAVA_LOADER_ID, DefaultJavaModelLoaderDelegate::new);
  }
}
