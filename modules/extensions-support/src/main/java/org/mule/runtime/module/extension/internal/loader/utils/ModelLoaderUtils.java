/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSemanticTermsDeclaration;
import org.mule.runtime.extension.api.declaration.type.annotation.InfrastructureTypeAnnotation;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.parser.SemanticTermsParser;

/**
 * Utility class for {@link ModelLoaderDelegate model loaders}
 *
 * @since 4.0
 */
public final class ModelLoaderUtils {

  private ModelLoaderUtils() {}

  /**
   * Adds all the semantic terms in the {@code parser} into the given {@code declaration}
   *
   * @param declaration a declaration
   * @param parser      a parser
   * @since 4.5.0
   */
  public static void addSemanticTerms(WithSemanticTermsDeclaration declaration, SemanticTermsParser parser) {
    declaration.getSemanticTerms().addAll(parser.getSemanticTerms());
  }

  public static void registerType(ExtensionDeclarer declarer, MetadataType type) {
    if (!getId(type).isPresent() || type.getAnnotation(InfrastructureTypeAnnotation.class).isPresent()) {
      return;
    }

    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (objectType instanceof MessageMetadataType) {
          MessageMetadataType messageType = (MessageMetadataType) objectType;
          messageType.getPayloadType().ifPresent(type -> type.accept(this));
          messageType.getAttributesType().ifPresent(type -> type.accept(this));
        }
        declarer.withType(objectType);
        objectType.getOpenRestriction().ifPresent(type -> type.accept(this));
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
