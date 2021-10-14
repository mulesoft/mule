/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.getXmlDslModel;
import static org.mule.runtime.extension.internal.util.ExtensionNamespaceUtils.getExtensionsNamespace;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.declaration.type.annotation.InfrastructureTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.error.ErrorsModelFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Describes an {@link ExtensionModel} by analyzing the annotations in the class provided in the constructor
 *
 * @since 4.0
 */
public class DefaultJavaModelLoaderDelegate implements ModelLoaderDelegate {

  protected Class<?> extensionType;
  protected final ExtensionElement extensionElement;
  protected final String version;

  private final ConfigModelLoaderDelegate configLoaderDelegate = new ConfigModelLoaderDelegate(this);
  private final OperationModelLoaderDelegate operationLoaderDelegate = new OperationModelLoaderDelegate(this);
  private final FunctionModelLoaderDelegate functionModelLoaderDelegate = new FunctionModelLoaderDelegate(this);
  private final SourceModelLoaderDelegate sourceModelLoaderDelegate = new SourceModelLoaderDelegate(this);
  private final ConnectionProviderModelLoaderDelegate connectionProviderModelLoaderDelegate =
      new ConnectionProviderModelLoaderDelegate(this);
  private final ParameterModelsLoaderDelegate parameterModelsLoaderDelegate =
      new ParameterModelsLoaderDelegate(this, this::getStereotypeModelLoaderDelegate);

  private StereotypeModelLoaderDelegate stereotypeModelLoaderDelegate;
  private Supplier<ErrorsModelFactory> errorsModelFactorySupplier;
  private ExtensionDeclarer declarer;

  public DefaultJavaModelLoaderDelegate(ExtensionElement extensionElement, String version) {
    this.version = version;
    this.extensionElement = extensionElement;
  }


  public DefaultJavaModelLoaderDelegate(Class<?> extensionType, String version) {
    this(new ExtensionTypeWrapper<>(extensionType, getDefault().createTypeLoader(extensionType.getClassLoader())), version);
    this.extensionType = extensionType;
  }

  private void reset() {
    stereotypeModelLoaderDelegate = null;
    declarer = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    try {
      stereotypeModelLoaderDelegate = new StereotypeModelLoaderDelegate(context);
      ExtensionModelParser parser = new JavaExtensionModelParser(extensionElement, stereotypeModelLoaderDelegate, context);
      ExtensionDeclarer declarer =
          context.getExtensionDeclarer()
              .named(parser.getName())
              .onVersion(version)
              .fromVendor(parser.getVendor())
              .withCategory(parser.getCategory())
              .withModelProperty(parser.getLicenseModelProperty())
              .withXmlDsl(getXmlDslModel(extensionElement, version, parser.getXmlDslConfiguration()));

      // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
      context.getParameter("COMPILATION_MODE")
          .ifPresent(m -> declarer.withModelProperty(new CompileTimeModelProperty()));

      this.declarer = declarer;
      
      parser.getDeprecationModel().ifPresent(declarer::withDeprecation);
      parser.getExternalLibraryModels().forEach(declarer::withExternalLibrary);
      parser.getExtensionHandlerModelProperty().ifPresent(declarer::withModelProperty);
      parser.getAdditionalModelProperties().forEach(declarer::withModelProperty);

      configLoaderDelegate.declareConfigurations(declarer, parser);
      connectionProviderModelLoaderDelegate.declareConnectionProviders(declarer, parser.getConnectionProviderModelParsers());

      if (!extensionElement.getConfigurations().isEmpty()) {
        operationLoaderDelegate.declareOperations(declarer, declarer, parser.getOperationModelParsers());
        functionModelLoaderDelegate.declareFunctions(declarer, parser.getFunctionModelParsers());
        sourceModelLoaderDelegate.declareMessageSources(declarer, declarer, parser.getSourceModelParsers());
      }

      parseErrorModels(parser, declarer);
      parseExports(parser, declarer);
      parseImportedTypes(parser, declarer, context);
      parseSubTypes(parser, declarer, context);

      getStereotypeModelLoaderDelegate().resolveDeclaredTypesStereotypes(declarer.getDeclaration());

      return declarer;
    } finally {
      reset();
    }
  }

  private void parseErrorModels(ExtensionModelParser parser, ExtensionDeclarer declarer) {
    initErrorModelFactorySupplier(parser, getExtensionsNamespace(declarer.getDeclaration()));
    declarer.getDeclaration().getErrorModels().addAll(createErrorModelFactory().getErrorModels());
  }

  private void parseExports(ExtensionModelParser parser, ExtensionDeclarer declarer) {
    parser.getExportedTypes().forEach(type -> registerExportedType(declarer, type));
    parser.getExportedResources().forEach(declarer::withResource);
    parser.getPrivilegedExportedArtifacts().forEach(declarer::withPrivilegedArtifact);
    parser.getPrivilegedExportedPackages().forEach(declarer::withPrivilegedPackage);
  }

  private void parseImportedTypes(ExtensionModelParser parser, ExtensionDeclarer declarer, ExtensionLoadingContext context) {
    parser.getImportedTypes().forEach(importedType -> {
      final Optional<String> typeId = getTypeId(importedType);

      if (!(importedType instanceof ObjectType)) {
        throw new IllegalArgumentException(format("Type '%s' is not complex. Only complex types can be imported from other extensions.",
                                                  typeId.orElseGet(importedType::toString)));
      }

      declarer.withImportedType(new ImportedTypeModel(typeId
          .flatMap(importedTypeId -> context.getDslResolvingContext().getTypeCatalog().getType(importedTypeId))
          .orElse((ObjectType) importedType)));
    });
  }

  private void parseSubTypes(ExtensionModelParser parser, ExtensionDeclarer declarer, ExtensionLoadingContext context) {
    parser.getSubTypes().forEach((base, subTypes) -> {
      declarer.withSubTypes(base, subTypes);

      // For subtypes that reference types from other artifacts, auto-import them.
      autoImportReferencedTypes(declarer, base, context);
      subTypes.forEach(subTypeEntry -> autoImportReferencedTypes(declarer, subTypeEntry, context));

      registerType(base);
      subTypes.forEach(sub -> registerType(sub));
    });
  }

  private void autoImportReferencedTypes(ExtensionDeclarer declarer,
                                         MetadataType subType,
                                         ExtensionLoadingContext loadingContext) {
    getTypeId(subType)
        .filter(imported -> getType(subType, loadingContext.getExtensionClassLoader())
            .map(clazz -> !clazz.getClassLoader().equals(loadingContext.getExtensionClassLoader()))
            .orElse(true))
        .ifPresent(subTypeId -> loadingContext.getDslResolvingContext().getTypeCatalog().getType(subTypeId)
            .map(ImportedTypeModel::new)
            .ifPresent(declarer::withImportedType));
  }

  public void registerOutputTypes(ExecutableComponentDeclaration<?> declaration) {
    if (declaration.getOutput() == null) {
      throw new IllegalModelDefinitionException(format("%s '%s' doesn't specify an output type",
          getComponentDeclarationTypeName(declaration), declaration.getName()));
    }

    if (declaration.getOutputAttributes() == null) {
      throw new IllegalModelDefinitionException(format("%s '%s' doesn't specify output attributes types",
          getComponentDeclarationTypeName(declaration), declaration.getName()));
    }

    registerType(declaration.getOutput().getType());
    registerType(declaration.getOutputAttributes().getType());
    // TODO:
    // declaration.getNotificationModels().forEach(notification -> registerType(declarer, notification.getType()));
  }

  public void registerType(MetadataType type) {
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

  private void registerExportedType(ExtensionDeclarer declarer, MetadataType type) {
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

  private void initErrorModelFactorySupplier(ExtensionModelParser parser, String namespace) {
    errorsModelFactorySupplier = () -> new ErrorsModelFactory(parser.getErrorModelParsers(), namespace);
  }

  OperationModelLoaderDelegate getOperationLoaderDelegate() {
    return operationLoaderDelegate;
  }

  FunctionModelLoaderDelegate getFunctionModelLoaderDelegate() {
    return functionModelLoaderDelegate;
  }

  SourceModelLoaderDelegate getSourceModelLoaderDelegate() {
    return sourceModelLoaderDelegate;
  }

  ConnectionProviderModelLoaderDelegate getConnectionProviderModelLoaderDelegate() {
    return connectionProviderModelLoaderDelegate;
  }

  StereotypeModelLoaderDelegate getStereotypeModelLoaderDelegate() {
    checkState(stereotypeModelLoaderDelegate != null, "stereotypeDelegate not yet initialized");
    return stereotypeModelLoaderDelegate;
  }

  ErrorsModelFactory createErrorModelFactory() {
    checkState(errorsModelFactorySupplier != null, "errorModelFactorySupplier not yet initialized");
    return errorsModelFactorySupplier.get();
  }

  public ParameterModelsLoaderDelegate getParameterModelsLoaderDelegate() {
    return parameterModelsLoaderDelegate;
  }
}
