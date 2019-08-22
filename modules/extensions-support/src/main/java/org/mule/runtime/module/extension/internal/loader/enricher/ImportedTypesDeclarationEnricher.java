/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.INITIALIZE;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseRepeatableAnnotation;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.ImportedTypes;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.util.List;
import java.util.Optional;

/**
 * Test the extension type to be annotated with {@link Import}, in which case it adds an {@link ImportedTypeModel} on the
 * extension level.
 *
 * @since 4.0
 */
public final class ImportedTypesDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return INITIALIZE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ExtensionDeclarer descriptor = extensionLoadingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = descriptor.getDeclaration();

    final Optional<ExtensionTypeDescriptorModelProperty> extensionType =
        extensionDeclaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
    if (!extensionType.isPresent()) {
      return;
    }

    Type type = extensionType.get().getType();

    final List<AnnotationValueFetcher<Import>> importTypes =
        parseRepeatableAnnotation(type, Import.class, c -> ((ImportedTypes) c).value());

    if (!importTypes.isEmpty()) {
      if (importTypes.stream().map(annotation -> annotation.getClassValue(Import::type)).distinct().collect(toList())
          .size() != importTypes.size()) {
        throw new IllegalModelDefinitionException(
                                                  format("There should be only one Import declaration for any given type in extension [%s]."
                                                      + " Multiple imports of the same type are not allowed",
                                                         extensionDeclaration.getName()));
      }

      importTypes.forEach(imported -> {
        MetadataType importedType = imported.getClassValue(Import::type).asMetadataType();

        if (!(importedType instanceof ObjectType)) {
          throw new IllegalArgumentException(format("Type '%s' is not complex. Only complex types can be imported from other extensions.",
                                                    type.getTypeName()));
        }

        extensionDeclaration
            .addImportedType(new ImportedTypeModel(getTypeId(importedType)
                .flatMap(importedTypeId -> extensionLoadingContext.getDslResolvingContext().getTypeCatalog()
                    .getType(importedTypeId))
                .orElse((ObjectType) importedType)));
      });
    }
  }
}
