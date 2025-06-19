/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_APPLICATION_LOADER_ID;

import org.mule.runtime.extension.api.loader.AbstractParserBasedExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkApplicationExtensionModelParserFactory;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Extensions defined as part of the same artifact.
 *
 * @since 4.5.0
 */
public class MuleSdkApplicationExtensionModelLoader extends AbstractParserBasedExtensionModelLoader {

  @Override
  public String getId() {
    return MULE_SDK_APPLICATION_LOADER_ID;
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new MuleSdkApplicationExtensionModelParserFactory();
  }
}
