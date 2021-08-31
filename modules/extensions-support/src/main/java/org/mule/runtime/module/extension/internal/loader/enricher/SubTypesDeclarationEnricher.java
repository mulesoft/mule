/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.STRUCTURE;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseRepeatableAnnotation;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.SubTypesMapping;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.enricher.stereotypes.StereotypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Test the extension type to be annotated with {@link SubTypeMapping}, in which case it adds an {@link ImportedTypeModel} on the
 * extension level.
 *
 * @since 4.0
 */
public final class SubTypesDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  /**
   * This has to run before {@link StereotypesDeclarationEnricher}.
   */
  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ExtensionDeclarer declarer = extensionLoadingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();

    Optional<ExtensionTypeDescriptorModelProperty> implementingType =
        extensionDeclaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
    if (!implementingType.isPresent()) {
      return;
    }
    Type type = implementingType.get().getType();

    List<AnnotationValueFetcher<SubTypeMapping>> typeMappings =
        parseRepeatableAnnotation(type, SubTypeMapping.class, c -> ((SubTypesMapping) c).value());

    if (!typeMappings.isEmpty()) {
      declareSubTypesMapping(declarer, typeMappings, extensionLoadingContext.getDslResolvingContext(),
                             extensionDeclaration.getName());
    }
  }

  private void declareSubTypesMapping(ExtensionDeclarer declarer, List<AnnotationValueFetcher<SubTypeMapping>> typeMappings,
                                      DslResolvingContext dslResolvingContext, String name) {
    Set<Object> baseTypes = new HashSet<>();

    typeMappings.forEach(mapping -> {
      final Type baseType = mapping.getClassValue(SubTypeMapping::baseType);
      baseTypes.add(baseType);

      MetadataType baseMetadataType = baseType.asMetadataType();
      Map<Type, MetadataType> subTypesMetadataTypes = mapping.getClassArrayValue(SubTypeMapping::subTypes)
          .stream()
          .collect(toMap(identity(), Type::asMetadataType, (u, v) -> {
            throw new IllegalStateException(format("Duplicate key %s", u));
          }, LinkedHashMap::new));

      declarer.withSubTypes(baseMetadataType, subTypesMetadataTypes.values());

      // For subtypes that reference types from other artifacts, auto-import them.
      autoImportReferencedTypes(declarer, dslResolvingContext, baseType, baseMetadataType);

      subTypesMetadataTypes.entrySet()
          .forEach(subTypeEntry -> autoImportReferencedTypes(declarer, dslResolvingContext, subTypeEntry.getKey(),
                                                             subTypeEntry.getValue()));
    });

    if (typeMappings.size() != baseTypes.size()) {
      throw new IllegalModelDefinitionException(format("There should be only one SubtypeMapping for any given base type in extension [%s]."
          + " Duplicated base types are not allowed", name));
    }
  }

  private void autoImportReferencedTypes(ExtensionDeclarer declarer, DslResolvingContext dslResolvingContext,
                                         Type subType, MetadataType subTypeMetadataType) {
    getTypeId(subTypeMetadataType)
        .filter(importedDeclaringExtension -> subType.getDeclaringClass()
            .map(c -> !c.getClassLoader().equals(currentThread().getContextClassLoader()))
            .orElse(true))
        .ifPresent(subTypeId -> dslResolvingContext.getTypeCatalog().getType(subTypeId)
            .map(ImportedTypeModel::new)
            .ifPresent(declarer::withImportedType));
  }

}
