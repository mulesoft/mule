/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_CLASSLOADER;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.noClassLoaderException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.property.ClassLoaderModelProperty;
import org.mule.runtime.module.extension.internal.ExtensionProperties;

/**
 * If the {@link ExtensionProperties#EXTENSION_CLASSLOADER} parameter is set on the {@link DescribingContext}, then a
 * {@link ClassLoaderModelProperty} is added at the {@link ExtensionModel} level, pointing to such property's value.
 *
 * If the parameter is not set, then an {@link IllegalModelDefinitionException} is thrown.
 *
 * @since 4.0
 */
public class ClassLoaderModelEnricher implements ModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    ClassLoader classLoader = describingContext.getParameter(EXTENSION_CLASSLOADER, ClassLoader.class);
    if (classLoader == null) {
      throw noClassLoaderException(describingContext.getExtensionDeclarer().getDeclaration().getName());
    }

    describingContext.getExtensionDeclarer().withModelProperty(new ClassLoaderModelProperty(classLoader));
  }
}
