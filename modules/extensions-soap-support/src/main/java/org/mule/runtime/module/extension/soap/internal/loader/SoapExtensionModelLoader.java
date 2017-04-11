/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static java.util.Collections.singleton;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.enricher.ConnectionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader;

/**
 * Loads a Soap Based extension by introspecting a class which uses the Soap Extensions API annotations
 *
 * @since 4.0
 */
public class SoapExtensionModelLoader extends AbstractJavaExtensionModelLoader {

  private static final String LOADER_ID = "soap";

  public SoapExtensionModelLoader() {
    super(LOADER_ID, SoapModelLoaderDelegate::new);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    //super.configureContextBeforeDeclaration(context);
    context.addCustomDeclarationEnrichers(singleton(new ConnectionDeclarationEnricher()));
  }
}
