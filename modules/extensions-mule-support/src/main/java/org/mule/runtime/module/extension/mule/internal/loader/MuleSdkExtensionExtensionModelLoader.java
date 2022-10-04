/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_LOADER_ID;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkExtensionExtensionModelParserFactory.create;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Extensions.
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionExtensionModelLoader extends AbstractExtensionModelLoader {

  @Override
  public String getId() {
    return MULE_SDK_EXTENSION_LOADER_ID;
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return create(context);
  }
}
