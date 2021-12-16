/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java;

import static java.util.Arrays.asList;

import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegateFactory;
import org.mule.runtime.module.extension.internal.loader.enricher.CustomStaticTypeDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.base.delegate.DefaultExtensionModelLoaderDelegate;

import java.util.List;

/**
 * Loads an extension by introspecting a class which uses the Extensions API annotations
 *
 * @since 4.0
 */
public class DefaultJavaExtensionModelLoader extends AbstractJavaExtensionModelLoader {

  public static final String JAVA_LOADER_ID = "java";

  private List<DeclarationEnricher> customEnrichers = asList(new CustomStaticTypeDeclarationEnricher());

  public DefaultJavaExtensionModelLoader() {
    super(JAVA_LOADER_ID, (ModelLoaderDelegateFactory) DefaultExtensionModelLoaderDelegate::new);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    super.configureContextBeforeDeclaration(context);
    context.addCustomDeclarationEnrichers(customEnrichers);
  }
}
