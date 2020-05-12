/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.internal.model.type.MetadataTypeModelAdapter.createMetadataTypeModelAdapterWithSterotype;
import static org.mule.runtime.config.internal.model.type.MetadataTypeModelAdapter.createParameterizedTypeModelAdapter;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getSubstitutionGroup;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Provides utilities to obtain the models/types for the elements of a mule config.
 *
 * @since 4.4
 */
public final class ApplicationModelTypeUtils {

  private ApplicationModelTypeUtils() {
    // Nothing to do
  }

  public static void resolveMetadataTypes(ExtensionModelHelper extensionModelHelper, Stream<ComponentAst> components) {
    // Use ExtensionModel to register top level and subTypes elements
    ReflectionCache reflectionCache = new ReflectionCache();
    Map<ComponentIdentifier, MetadataTypeModelAdapter> registry = new HashMap<>();
    extensionModelHelper.getExtensionsModels().stream().forEach(extensionModel -> extensionModel.getTypes().stream()
        .filter(p -> isInstantiable(p, reflectionCache))
        .forEach(parameterType -> registerTopLevelParameter(parameterType, reflectionCache, registry, extensionModel,
                                                            extensionModelHelper)));

    components
        .filter(componentModel -> registry.containsKey(componentModel.getIdentifier()))
        .forEach(componentModel -> ((ComponentModel) componentModel)
            .setMetadataTypeModelAdapter(registry.get(componentModel.getIdentifier())));
  }

  private static void registerTopLevelParameter(MetadataType parameterType, ReflectionCache reflectionCache,
                                                Map<ComponentIdentifier, MetadataTypeModelAdapter> registry,
                                                ExtensionModel extensionModel, ExtensionModelHelper extensionModelHelper) {
    Optional<DslElementSyntax> dslElement = extensionModelHelper.resolveDslElementModel(parameterType, extensionModel);
    if (!dslElement.isPresent() ||
        registry.containsKey(builder().name(dslElement.get().getElementName()).namespace(dslElement.get().getPrefix()).build())) {
      return;
    }

    DslElementSyntax dsl = dslElement.get();

    parameterType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (dsl.supportsTopLevelDeclaration() || (dsl.supportsChildDeclaration() && dsl.isWrapped())
            || getSubstitutionGroup(objectType).isPresent() ||
            extensionModelHelper.getAllSubTypes().contains(objectType)) {

          registry.put(builder().name(dsl.getElementName()).namespace(dsl.getPrefix())
              .build(), createMetadataTypeModelAdapterWithSterotype(objectType, extensionModelHelper)
                  .orElse(createParameterizedTypeModelAdapter(objectType, extensionModelHelper)));
        }

        registerSubTypes(objectType, reflectionCache, registry, extensionModel, extensionModelHelper);
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        registerTopLevelParameter(arrayType.getType(), reflectionCache, registry, extensionModel, extensionModelHelper);
      }

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(type -> type.accept(this));
      }

    });
  }

  private static void registerSubTypes(MetadataType type, ReflectionCache reflectionCache,
                                       Map<ComponentIdentifier, MetadataTypeModelAdapter> registry,
                                       ExtensionModel extensionModel, ExtensionModelHelper extensionModelHelper) {
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(type -> type.accept(this));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitObject(ObjectType objectType) {
        Optional<MetadataType> openRestriction = objectType.getOpenRestriction();
        if (objectType.isOpen() && openRestriction.isPresent()) {
          openRestriction.get().accept(this);
        } else {
          extensionModelHelper.getSubTypes(objectType)
              .forEach(subtype -> registerTopLevelParameter(subtype, reflectionCache, registry, extensionModel,
                                                            extensionModelHelper));
        }
      }
    });
  }

}
