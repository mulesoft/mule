/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.util.NameUtils.underscorize;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONNECTION;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.api.meta.model.declaration.fluent.HasStereotypeDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.InfrastructureTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.parser.AllowedStereotypesModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.CustomStereotypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.DefaultStereotypeModelFactory;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;
import org.mule.sdk.api.stereotype.ImplicitStereotypeDefinition;
import org.mule.sdk.api.stereotype.MuleStereotypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class StereotypeModelLoaderDelegate {

  private final Map<ComponentDeclaration, List<StereotypeModel>> componentsConfigStereotypes = new HashMap<>();
  private final ClassTypeLoader typeLoader;
  private final DslResolvingContext dslResolvingContext;
  private final DefaultStereotypeModelFactory stereotypeModelFactory;

  private String namespace;

  public StereotypeModelLoaderDelegate(ExtensionLoadingContext extensionLoadingContext) {
    stereotypeModelFactory = new DefaultStereotypeModelFactory(extensionLoadingContext);
    dslResolvingContext = extensionLoadingContext.getDslResolvingContext();
    this.typeLoader =
        new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionLoadingContext.getExtensionClassLoader());
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
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

  public void addStereotype(StereotypeModelParser parser,
                            ConfigurationDeclarer declarer,
                            Optional<Supplier<StereotypeModel>> fallback) {
    doAddStereotypes(parser, declarer, fallback);
    populateComponentConfigsMap(declarer.getDeclaration());
  }

  public void addStereotype(StereotypeModelParser parser,
                            ConnectionProviderDeclarer declarer,
                            Optional<Supplier<StereotypeModel>> fallback) {
    doAddStereotypes(parser, declarer, fallback);
  }

  public void addStereotype(StereotypeModelParser parser,
                            ComponentDeclarer declarer,
                            Optional<Supplier<StereotypeModel>> fallback) {
    doAddStereotypes(parser, declarer, fallback);
    addConfigRefStereoTypesIfNeeded((ComponentDeclaration<?>) declarer.getDeclaration());
  }

  public void addAllowedStereotypes(AllowedStereotypesModelParser parser, NestedComponentDeclarer declarer) {
    List<StereotypeModel> allowedStereotypes = parser.getAllowedStereotypes(stereotypeModelFactory);
    if (allowedStereotypes.isEmpty()) {
      declarer.withAllowedStereotypes(MuleStereotypes.PROCESSOR);
    } else {
      allowedStereotypes.forEach(declarer::withAllowedStereotypes);
    }
  }

  public void resolveDeclaredTypesStereotypes(ExtensionDeclaration declaration) {
    Map<ObjectType, ObjectType> subTypeToParent = new HashMap<>();
    declaration.getSubTypes()
        .forEach(subTypeModel -> subTypeModel.getSubTypes()
            .forEach(subType -> subTypeToParent.put(subType, subTypeModel.getBaseType())));

    BiFunction<ObjectType, Class<? extends StereotypeDefinition>, StereotypeModel> resolver =
        (type, def) -> resolveStereotype(def, type, namespace, subTypeToParent);
    declaration.getTypes().forEach(type -> resolveStereotype(type, resolver));
  }

  private StereotypeModel resolveStereotype(Class<? extends StereotypeDefinition> def,
                                            ObjectType type,
                                            String namespace,
                                            Map<ObjectType, ObjectType> subTypeToParent) {

    // If the type is defined in another extension, set the namespace for its stereotype accordingly
    if (def.equals(ImplicitStereotypeDefinition.class)
        || def.equals(org.mule.runtime.extension.api.stereotype.ImplicitStereotypeDefinition.class)) {
      namespace = resolveImportedTypeNamespace(type, namespace);

      final String stereotypeName = toStereotypeName(type.getAnnotation(ClassInformationAnnotation.class).get().getClassname());

      final ObjectType parentObjectType = subTypeToParent.get(type);
      if (parentObjectType != null) {
        // If the type is a subtype, link to its parent stereotype
        return stereotypeModelFactory.createStereotype(
            new org.mule.sdk.api.stereotype.ImplicitStereotypeDefinition(stereotypeName,
                new org.mule.sdk.api.stereotype.ImplicitStereotypeDefinition(toStereotypeName(parentObjectType
                    .getAnnotation(ClassInformationAnnotation.class).get()
                    .getClassname()))));
      } else {
        // else, create the stereotype without a parent
        return stereotypeModelFactory.createStereotype(new ImplicitStereotypeDefinition(stereotypeName));
      }
    } else {
      return createCustomStereotype(def, namespace, stereotypes);
    }
  }

  private String toStereotypeName(final String classname) {
    return underscorize(classname.substring(classname.lastIndexOf(".") + 1)).toUpperCase();
  }

  private void resolveStereotype(ObjectType type,
                                 BiFunction<ObjectType, Class<? extends StereotypeDefinition>, StereotypeModel> resolver) {
    type.accept(new MetadataTypeVisitor() {

      // This is created to avoid a recursive types infinite loop, producing an StackOverflow when resolving the stereotypes.
      private final Set<MetadataType> registeredTypes = new HashSet<>();

      @Override
      public void visitObject(ObjectType objectType) {
        if (!registeredTypes.contains(objectType)
            && !objectType.getAnnotation(InfrastructureTypeAnnotation.class).isPresent()) {
          registeredTypes.add(objectType);
          objectType.getAnnotation(StereotypeTypeAnnotation.class).ifPresent(a -> a.resolveStereotypes(objectType, resolver));
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
        .map(MuleExtensionUtils::getExtensionsNamespace)
        .orElse(defaultNamespace);
  }

  private void addConfigRefStereoTypesIfNeeded(ComponentDeclaration<?> declaration) {
    List<StereotypeModel> configStereotypes = componentsConfigStereotypes.get(declaration);
    if (configStereotypes != null && !configStereotypes.isEmpty()) {
      declaration.getAllParameters().stream()
          .filter(p -> CONFIG_ATTRIBUTE_NAME.equals(p.getName()))
          .findAny()
          .ifPresent(configRef -> configRef.setAllowedStereotypeModels(configStereotypes));
    }
  }

  private <T extends HasStereotypeDeclarer & HasModelProperties> StereotypeModel doAddStereotypes(
      StereotypeModelParser parser,
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

    return stereotypeModel;
  }

  private void populateComponentConfigsMap(ConfigurationDeclaration config) {
    StereotypeModel configStereotype = config.getStereotype();
    if (configStereotype != null) {
      config.getConstructs().forEach(construct -> addComponentConfigStereotype(construct, configStereotype));
      config.getMessageSources().forEach(source -> addComponentConfigStereotype(source, configStereotype));
      config.getOperations().forEach(operation -> addComponentConfigStereotype(operation, configStereotype));
    }
  }

  private void addComponentConfigStereotype(ComponentDeclaration declaration, StereotypeModel configStereotype) {
    componentsConfigStereotypes.computeIfAbsent(declaration, key -> new LinkedList<>()).add(configStereotype);
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
    stereotypeModelFactory.setNamespace(namespace.toUpperCase());
  }
}
