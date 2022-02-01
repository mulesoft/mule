/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_LOADER_ID;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkExtensionModelParserFactory;

/**
 * {@link ExtensionModelLoader} implementation for the Mule SDK
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionModelLoader extends AbstractExtensionModelLoader {

  @Override
  public String getId() {
    return MULE_SDK_LOADER_ID;
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new MuleSdkExtensionModelParserFactory();
  }
}
