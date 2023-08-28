/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.extension.api.ExtensionConstants.JAVA_SDK_LOADER_ID;

/**
 * Loads an extension by introspecting a class which uses the Extensions API annotations
 *
 * @since 4.0
 */
public class DefaultJavaExtensionModelLoader extends AbstractJavaExtensionModelLoader {

  public static final String JAVA_LOADER_ID = JAVA_SDK_LOADER_ID;

  public DefaultJavaExtensionModelLoader() {
    super(JAVA_LOADER_ID);
  }
}
