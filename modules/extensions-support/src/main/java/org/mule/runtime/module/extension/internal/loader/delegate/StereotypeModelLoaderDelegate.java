/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.util.NameUtils.underscorize;
import static org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.SdkStereotypeDefinitionAdapter.from;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.sdk.api.stereotype.MuleStereotypes.PROCESSOR;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.api.meta.model.declaration.fluent.HasStereotypeDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.declaration.type.annotation.InfrastructureTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.utils.ExtensionNamespaceUtils;
import org.mule.runtime.extension.api.loader.parser.AllowedStereotypesModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterModelParser;
import org.mule.runtime.extension.api.loader.parser.StereotypeModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.CustomStereotypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.DefaultStereotypeModelFactory;
import org.mule.sdk.api.stereotype.ImplicitStereotypeDefinition;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Helper class for declaring sources through a {@link StereotypeModelLoaderDelegate}
 *
 * @since 4.5
 */
public class StereotypeModelLoaderDelegate {

  private final DslResolvingContext dslResolvingContext;
  private final DefaultStereotypeModelFactory stereotypeModelFactory;

  private String namespace;

  public StereotypeModelLoaderDelegate(ExtensionLoadingContext extensionLoadingContext) {
    stereotypeModelFactory = new DefaultStereotypeModelFactory();
    dslResolvingContext = extensionLoadingContext.getDslResolvingContext();
  }

  public StereotypeModel getDefaultConfigStereotype(String configName) {
    return createStereotype(configName, CONFIG);
  }

  public StereotypeModel getDefaultConnectionProviderStereotype(String connectionProviderName) {
    return createStereotype(connectionProviderName, CONNECTION);
  }

  public StereotypeModel getDefaultOperationStereotype(String operationName) {
    return createStereotype(operationName, stereotypeModelFactory.getProcessorParentStereotype());
  }

  public StereotypeModel getDefaultSourceStereotype(String sourceName) {
    return createStereotype(sourceName, stereotypeModelFactory.getSourceParentStereotype());
  }

  private StereotypeModel createStereotype(String name, StereotypeModel parent) {
    return stereotypeModelFactory.createStereotype(name, parent);
  }

  public <T extends HasStereotypeDeclarer & HasModelProperties> void addStereotypes(StereotypeModelParser parser,
                                                                                    T declarer,
                                                                                    Optional<Supplier<StereotypeModel>> fallback) {

    StereotypeModel stereotypeModel = parser.getStereotype(stereotypeModelFactory).orElse(null);
    if (stereotypeModel != null) {
      declarer.withModelProperty(CustomStereotypeModelProperty.INSTANCE);
    } else {
      stereotypeModel = fallback.map(Supplier::get).orElse(null);
    }

    if (stereotypeModel != null) {
      declarer.withStereotype(stereotypeModel);
    }
  }

  public void addStereotypes(ParameterModelParser parser, ParameterDeclarer declarer) {
    declarer.getDeclaration().getAllowedStereotypeModels().addAll(parser.getAllowedStereotypes(stereotypeModelFactory));
  }

  public void addAllowedStereotypes(AllowedStereotypesModelParser parser, NestedComponentDeclarer declarer) {
    List<StereotypeModel> allowedStereotypes = parser.getAllowedStereotypes(stereotypeModelFactory);
    if (allowedStereotypes.isEmpty()) {
      declarer.withAllowedStereotypes(PROCESSOR);
    } else {
      allowedStereotypes.forEach(declarer::withAllowedStereotypes);
    }
  }

  public void resolveDeclaredTypesStereotypes(ExtensionDeclaration declaration) {
    Map<ObjectType, ObjectType> subTypeToParent = new HashMap<>();
    declaration.getSubTypes()
        .forEach(subTypeModel -> subTypeModel.getSubTypes()
            .forEach(subType -> subTypeToParent.put(subType, subTypeModel.getBaseType())));

    ObjectTypeStereotypeResolver resolver = (type, def) -> resolveStereotype(def, type, namespace, subTypeToParent);
    LegacyObjectTypeStereotypeResolver legacyResolver = (type, def) -> resolveStereotype(def, type, namespace, subTypeToParent);

    declaration.getTypes().forEach(type -> resolveStereotype(type, resolver, legacyResolver));
  }

  private StereotypeModel resolveStereotype(Class<?> def,
                                            ObjectType type,
                                            String namespace,
                                            Map<ObjectType, ObjectType> subTypeToParent) {

    // If the type is defined in another extension, set the namespace for its stereotype accordingly
    if (def.equals(ImplicitStereotypeDefinition.class)
        || def.equals(org.mule.runtime.extension.api.stereotype.ImplicitStereotypeDefinition.class)) {
      namespace = resolveImportedTypeNamespace(type, namespace);

      final String stereotypeName = toStereotypeName(type.getAnnotation(ClassInformationAnnotation.class).get().getClassname());

      final ObjectType parentObjectType = subTypeToParent.get(type);
      final StereotypeDefinition definition = parentObjectType != null
          ? new ImplicitStereotypeDefinition(stereotypeName, new ImplicitStereotypeDefinition(toStereotypeName(parentObjectType
              .getAnnotation(ClassInformationAnnotation.class).get().getClassname())))
          : new ImplicitStereotypeDefinition(stereotypeName);

      return stereotypeModelFactory.createStereotype(definition, namespace);
    } else {
      return stereotypeModelFactory.createStereotype(from(def), namespace);
    }
  }

  private String toStereotypeName(final String classname) {
    return underscorize(classname.substring(classname.lastIndexOf(".") + 1)).toUpperCase();
  }

  private void resolveStereotype(ObjectType type,
                                 ObjectTypeStereotypeResolver resolver,
                                 LegacyObjectTypeStereotypeResolver legacyResolver) {
    type.accept(new MetadataTypeVisitor() {

      // This is created to avoid a recursive types infinite loop, producing an StackOverflow when resolving the stereotypes.
      private final Set<MetadataType> registeredTypes = new LinkedHashSet<>();

      @Override
      public void visitObject(ObjectType objectType) {
        if (!registeredTypes.contains(objectType)
            && !objectType.getAnnotation(InfrastructureTypeAnnotation.class).isPresent()) {
          registeredTypes.add(objectType);
          objectType.getAnnotation(StereotypeTypeAnnotation.class)
              .ifPresent(a -> a.resolveAllowedStereotypes(objectType, resolver, legacyResolver));
          objectType.getFields().forEach(f -> f.getValue().accept(this));
          objectType.getOpenRestriction().ifPresent(open -> open.accept(this));
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(t -> t.accept(this));
      }

      @Override
      public void visitIntersection(IntersectionType intersectionType) {
        intersectionType.getTypes().forEach(t -> t.accept(this));
      }
    });
  }

  private String resolveImportedTypeNamespace(ObjectType type, String defaultNamespace) {
    return getTypeId(type)
        .flatMap(typeId -> dslResolvingContext.getExtensionForType(typeId))
        .map(ExtensionNamespaceUtils::getExtensionsNamespace)
        .orElse(defaultNamespace);
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace.toUpperCase();
    stereotypeModelFactory.setNamespace(this.namespace);
  }

  @FunctionalInterface
  interface ObjectTypeStereotypeResolver extends BiFunction<ObjectType, Class<? extends StereotypeDefinition>, StereotypeModel> {

  }

  @FunctionalInterface
  interface LegacyObjectTypeStereotypeResolver extends
      BiFunction<ObjectType, Class<? extends org.mule.runtime.extension.api.stereotype.StereotypeDefinition>, StereotypeModel> {

  }
}
