/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_APPLICATION_LOADER_ID;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkApplicationExtensionModelParserFactory;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Extensions defined as part of the same artifact.
 *
 * @since 4.5.0
 */
public class MuleSdkApplicationExtensionModelLoader extends AbstractExtensionModelLoader {

  @Override
  public String getId() {
    return MULE_SDK_APPLICATION_LOADER_ID;
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new MuleSdkApplicationExtensionModelParserFactory();
  }
}
