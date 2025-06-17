/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import static org.mule.runtime.core.internal.util.version.JdkVersionUtils.getJdkVersion;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_SUPPORTED_JAVA_VERSIONS;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.declarerWithMmv;
import static org.mule.runtime.module.extension.internal.loader.utils.ExtensionNamespaceUtils.getExtensionsNamespace;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getXmlDslModel;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.core.internal.util.version.JdkVersionUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.parser.LicenseModelParser;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.extension.api.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.error.ErrorsModelFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ArtifactLifecycleListenerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DevelopmentFrameworkModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.TypeLoaderModelProperty;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParser;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.internal.loader.validator.DeprecationModelValidator;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalSourceException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Describes an {@link ExtensionModel} by analyzing the annotations in the class provided in the constructor
 *
 * @since 4.0
 */
public class DefaultExtensionModelLoaderDelegate implements ModelLoaderDelegate {

  private final List<ExtensionModelValidator> VALIDATORS = singletonList(new DeprecationModelValidator());

  protected final String version;
  private final String loaderId;

  private final ConfigModelLoaderDelegate configLoaderDelegate = new ConfigModelLoaderDelegate(this);
  private final OperationModelLoaderDelegate operationLoaderDelegate = new OperationModelLoaderDelegate(this);
  private final FunctionModelLoaderDelegate functionModelLoaderDelegate = new FunctionModelLoaderDelegate(this);
  private final SourceModelLoaderDelegate sourceModelLoaderDelegate = new SourceModelLoaderDelegate(this);
  private final ConnectionProviderModelLoaderDelegate connectionProviderModelLoaderDelegate =
      new ConnectionProviderModelLoaderDelegate(this);
  private final ParameterModelsLoaderDelegate parameterModelsLoaderDelegate =
      new ParameterModelsLoaderDelegate(this::getStereotypeModelLoaderDelegate, this::registerType);
  private final Map<String, NotificationModel> notificationModels = new LinkedHashMap<>();

  private StereotypeModelLoaderDelegate stereotypeModelLoaderDelegate;
  private Supplier<ErrorsModelFactory> errorsModelFactorySupplier;
  private ExtensionDeclarer declarer;
  private String namespace;

  public DefaultExtensionModelLoaderDelegate(String version, String loaderId) {
    this.version = version;
    this.loaderId = loaderId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionDeclarer declare(ExtensionModelParserFactory parserFactory, ExtensionLoadingContext context) {
    try {
      return doDeclare(parserFactory, context);
    } catch (Exception e) {
      // Handle errors caused by the java version before the actual validation takes place, since the validation needs the
      // extension model
      extractOfType(e, NoClassDefFoundError.class)
          .filter(ncdfe -> ncdfe.getMessage().startsWith("javax/"))
          .ifPresent(ncdfe -> {
            JdkVersionUtils.JdkVersion runningJdkVersion = getJdkVersion();
            Set<String> supportedJavaVersions = context.getExtensionDeclarer().getDeclaration().getSupportedJavaVersions();
            if (supportedJavaVersions.isEmpty()) {
              supportedJavaVersions = DEFAULT_SUPPORTED_JAVA_VERSIONS;
            }

            throw new IllegalSourceException(format("Extension '%s' version %s does not support Mule 4.6+ on Java %s. Supported Java versions are: %s. (%s)",
                                                    context.getExtensionDeclarer().getDeclaration().getName(),
                                                    context.getExtensionDeclarer().getDeclaration().getVersion(),
                                                    runningJdkVersion.getMajor(),
                                                    supportedJavaVersions,
                                                    ncdfe));
          });

      throw e;
    }
  }

  private ExtensionDeclarer doDeclare(ExtensionModelParserFactory parserFactory, ExtensionLoadingContext context) {
    context.addCustomValidators(VALIDATORS);

    ExtensionModelParser parser = parserFactory.createParser(context);
    ExtensionDeclarer declarer =
        context.getExtensionDeclarer()
            .named(parser.getName())
            .onVersion(version)
            .supportingJavaVersions(parser.getSupportedJavaVersions())
            .fromVendor(parser.getVendor())
            .withCategory(parser.getCategory())
            .withModelProperty(createLicenseModelProperty(parser.getLicensingParser()))
            .withXmlDsl(getXmlDslModel(parser.getName(), version, parser.getXmlDslConfiguration()));

    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    context.getParameter("COMPILATION_MODE")
        .ifPresent(m -> declarer.withModelProperty(new CompileTimeModelProperty()));

    declarer.withModelProperty(new DevelopmentFrameworkModelProperty(loaderId));
    declarer.withModelProperty(new TypeLoaderModelProperty(context.getTypeLoader()));
    parser.getArtifactLifecycleListenerClass()
        .map(ArtifactLifecycleListenerModelProperty::new)
        .ifPresent(declarer::withModelProperty);

    this.declarer = declarer;
    namespace = getExtensionsNamespace(declarer.getDeclaration());
    stereotypeModelLoaderDelegate = new StereotypeModelLoaderDelegate(context);
    stereotypeModelLoaderDelegate.setNamespace(namespace);

    parser.getDeprecationModel().ifPresent(declarer::withDeprecation);
    parser.getExternalLibraryModels().forEach(declarer::withExternalLibrary);
    parser.getExceptionHandlerFactory().map(ExceptionHandlerModelProperty::new).ifPresent(declarer::withModelProperty);
    parser.getAdditionalModelProperties().forEach(declarer::withModelProperty);
    if (context.isResolveMinMuleVersion()) {
      parser.getResolvedMinMuleVersion().ifPresent(resolvedMMV -> declarerWithMmv(declarer, resolvedMMV));
    }

    declareErrorModels(parser, declarer);
    declareExports(parser, declarer);
    declareImportedTypes(parser, declarer, context);
    declareSubTypes(parser, declarer, context);
    declareNotifications(parser, declarer);

    configLoaderDelegate.declareConfigurations(declarer, parser, context);
    connectionProviderModelLoaderDelegate.declareConnectionProviders(declarer, parser.getConnectionProviderModelParsers(),
                                                                     context);

    operationLoaderDelegate.declareOperations(declarer, declarer, parser.getOperationModelParsers(), context);
    functionModelLoaderDelegate.declareFunctions(declarer, parser.getFunctionModelParsers(), context);
    sourceModelLoaderDelegate.declareMessageSources(declarer, declarer, parser.getSourceModelParsers(), context);

    getStereotypeModelLoaderDelegate().resolveDeclaredTypesStereotypes(declarer.getDeclaration());

    return declarer;
  }

  private void declareNotifications(ExtensionModelParser parser, ExtensionDeclarer declarer) {
    parser.getNotificationModels().forEach(notification -> {
      declarer.withNotificationModel(notification);
      registerType(notification.getType());
      notificationModels.put(notification.getIdentifier(), notification);
    });
  }

  private void declareErrorModels(ExtensionModelParser parser, ExtensionDeclarer declarer) {
    initErrorModelFactorySupplier(parser);
    declarer.getDeclaration().getErrorModels().addAll(createErrorModelFactory().getErrorModels());
  }

  private void declareExports(ExtensionModelParser parser, ExtensionDeclarer declarer) {
    parser.getExportedTypes().forEach(type -> registerExportedType(declarer, type));
    parser.getExportedResources().forEach(declarer::withResource);
    parser.getPrivilegedExportedArtifacts().forEach(declarer::withPrivilegedArtifact);
    parser.getPrivilegedExportedPackages().forEach(declarer::withPrivilegedPackage);
  }

  private void declareImportedTypes(ExtensionModelParser parser, ExtensionDeclarer declarer, ExtensionLoadingContext context) {
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

  private void declareSubTypes(ExtensionModelParser parser, ExtensionDeclarer declarer, ExtensionLoadingContext context) {
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
  }

  public void registerType(MetadataType type) {
    ExtensionMetadataTypeUtils.registerType(declarer, type);
  }

  public Optional<NotificationModel> getNotificationModel(String identifier) {
    return ofNullable(notificationModels.get(identifier));
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

  private void initErrorModelFactorySupplier(ExtensionModelParser parser) {
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

  private LicenseModelProperty createLicenseModelProperty(LicenseModelParser parser) {
    return new LicenseModelProperty(parser.requiresEeLicense(), parser.isAllowsEvaluationLicense(),
                                    parser.getRequiredEntitlement());
  }
}
