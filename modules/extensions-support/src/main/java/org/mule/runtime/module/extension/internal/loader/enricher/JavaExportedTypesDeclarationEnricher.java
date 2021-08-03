/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.INITIALIZE;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ExportedClassNamesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Enriches the declaration with the types which are manually exported through {@link Export}
 *
 * @since 4.0
 */
public final class JavaExportedTypesDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return INITIALIZE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().getDeclaration()
        .getModelProperty(ExtensionTypeDescriptorModelProperty.class)
        .map(ExtensionTypeDescriptorModelProperty::getType)
        .flatMap(type -> type.getValueFromAnnotation(Export.class))
        .ifPresent(exportAnnotation -> {
          Set<String> exportedClassNames = new LinkedHashSet<>();
          ExtensionDeclarer declarer = extensionLoadingContext.getExtensionDeclarer();
          exportAnnotation.getClassArrayValue(Export::classes).forEach(type -> {
            exportedClassNames.add(type.getClassInformation().getClassname());
            registerType(declarer, type.asMetadataType());
          });
          exportAnnotation.getArrayValue(Export::resources).forEach(declarer::withResource);

          if (!exportedClassNames.isEmpty()) {
            declarer.withModelProperty(new ExportedClassNamesModelProperty(exportedClassNames));
          }
        });
  }

  private void registerType(ExtensionDeclarer declarer, MetadataType type) {
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (objectType.isOpen()) {
          objectType.getOpenRestriction().get().accept(this);
        } else {
          declarer.withType(objectType);
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitIntersection(IntersectionType intersectionType) {
        intersectionType.getTypes().forEach(type -> type.accept(this));
      }

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(type -> type.accept(this));
      }

      @Override
      public void visitObjectField(ObjectFieldType objectFieldType) {
        objectFieldType.getValue().accept(this);
      }
    });
  }
}
