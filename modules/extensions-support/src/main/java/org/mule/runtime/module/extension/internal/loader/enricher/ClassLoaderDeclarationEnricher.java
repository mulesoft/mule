/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.noClassLoaderException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ClassLoaderModelProperty;

/**
 * Adds a {@link ClassLoaderModelProperty} pointing to {@link ExtensionLoadingContext#getExtensionClassLoader()}
 *
 * @since 4.0
 */
public class ClassLoaderDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ClassLoader classLoader = extensionLoadingContext.getExtensionClassLoader();
    if (classLoader == null) {
      throw noClassLoaderException(extensionLoadingContext.getExtensionDeclarer().getDeclaration().getName());
    }

    extensionLoadingContext.getExtensionDeclarer().withModelProperty(new ClassLoaderModelProperty(classLoader));
  }
}
