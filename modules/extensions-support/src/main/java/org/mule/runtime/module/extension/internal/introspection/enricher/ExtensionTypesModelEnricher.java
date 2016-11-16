/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.model.property.ExportModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.util.Collection;

/**
 * Navigates all the components of the extension and automatically declares all complex types.
 *
 * @since 4.0
 */
public class ExtensionTypesModelEnricher implements ModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    final ExtensionDeclarer declarer = describingContext.getExtensionDeclarer();

    declareDefaultTypes(declarer);
    declareExportedTypes(declarer);
    declareSubTypes(declarer);
  }

  private void declareExportedTypes(ExtensionDeclarer declarer) {
    declarer.getDeclaration().getModelProperty(ExportModelProperty.class)
        .ifPresent(p -> registerTypes(declarer, p.getExportedTypes()));
  }

  private void declareSubTypes(ExtensionDeclarer declarer) {
    declarer.getDeclaration().getSubTypes().forEach(type -> registerTypes(declarer, type.getSubTypes()));
  }

  private void declareDefaultTypes(final ExtensionDeclarer declarer) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {
        registerType(declarer, declaration.getType());
      }

      @Override
      public void onSource(SourceDeclaration declaration) {
        registerType(declarer, declaration);
      }

      @Override
      public void onOperation(OperationDeclaration declaration) {
        registerType(declarer, declaration);
      }

    }.walk(declarer.getDeclaration());
  }


  private void registerTypes(ExtensionDeclarer declarer, Collection<MetadataType> types) {
    types.forEach(type -> registerType(declarer, type));
  }

  private void registerType(ExtensionDeclarer declarer, ComponentDeclaration declaration) {
    registerType(declarer, declaration.getOutput().getType());
    registerType(declarer, declaration.getOutputAttributes().getType());
  }

  private void registerType(ExtensionDeclarer declarer, MetadataType type) {
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        declarer.withType(objectType);
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        dictionaryType.getKeyType().accept(this);
        dictionaryType.getValueType().accept(this);
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
