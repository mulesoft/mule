/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Arrays.stream;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.property.ExportModelProperty;

/**
 * Test the declaring class to be annotated with {@link Export}, in which case it adds an {@link ExportModelProperty} on the
 * extension level.
 *
 * @since 4.0
 */
public class ExportModelEnricher extends AbstractAnnotatedModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    Export exportAnnotation = extractAnnotation(describingContext.getExtensionDeclarer().getDeclaration(), Export.class);
    if (exportAnnotation != null) {
      final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

      describingContext.getExtensionDeclarer().withModelProperty(new ExportModelProperty(stream(exportAnnotation.classes())
          .map(typeLoader::load).collect(new ImmutableListCollector<>()), copyOf(exportAnnotation.resources())));
    }
  }
}
