/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseRepeatableAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataType;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.core.util.collection.ImmutableMapCollector;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.SubTypesMapping;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;

import java.util.List;
import java.util.Map;

/**
 * Test the extension type to be annotated with {@link SubTypeMapping}, in which case it adds an
 * {@link ImportedTypesModelProperty} on the extension level.
 *
 * @since 4.0
 */
public final class SubTypesModelEnricher extends AbstractAnnotatedModelEnricher {

  private ClassTypeLoader typeLoader;

  @Override
  public void enrich(DescribingContext describingContext) {
    ExtensionDeclarer descriptor = describingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = descriptor.getDeclaration();

    Class<?> type = extractExtensionType(extensionDeclaration);
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(type.getClassLoader());

    List<SubTypeMapping> typeMappings = parseRepeatableAnnotation(type, SubTypeMapping.class, c -> ((SubTypesMapping) c).value());
    if (!typeMappings.isEmpty()) {
      extensionDeclaration.addModelProperty(declareSubTypesMapping(typeMappings, extensionDeclaration.getName()));
    }

  }

  private SubTypesModelProperty declareSubTypesMapping(List<SubTypeMapping> typeMappings, String name) {
    if (typeMappings.stream().map(SubTypeMapping::baseType).distinct().collect(toList()).size() != typeMappings.size()) {
      throw new IllegalModelDefinitionException(String
          .format("There should be only one SubtypeMapping for any given base type in extension [%s]."
              + " Duplicated base types are not allowed", name));
    }

    Map<MetadataType, List<MetadataType>> subTypesMap = typeMappings.stream()
        .collect(new ImmutableMapCollector<>(mapping -> getMetadataType(mapping.baseType(), typeLoader),
                                             mapping -> stream(mapping.subTypes())
                                                 .map(subType -> getMetadataType(subType, typeLoader))
                                                 .collect(new ImmutableListCollector<>())));

    return new SubTypesModelProperty(subTypesMap);
  }

}
