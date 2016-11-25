/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.roleOf;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExceptionEnricherFactory;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExtension;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseLayoutAnnotations;
import static org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty.CallbackPhase.ON_ERROR;
import static org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty.CallbackPhase.ON_SUCCESS;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnAttributesType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ElementDslModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExtensionOf;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.Describer;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.manifest.DescriberManifest;
import org.mule.runtime.extension.api.model.property.PagedOperationModelProperty;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.internal.introspection.BasicTypeMetadataVisitor;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.introspection.describer.contributor.FunctionParameterTypeContributor;
import org.mule.runtime.module.extension.internal.introspection.describer.contributor.InfrastructureFieldContributor;
import org.mule.runtime.module.extension.internal.introspection.describer.contributor.ParameterDeclarerContributor;
import org.mule.runtime.module.extension.internal.introspection.describer.contributor.ParameterResolverParameterTypeContributor;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ComponentElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConfigurationElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConnectionProviderElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionTypeFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.model.FieldElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.MethodElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.OperationContainerElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.SourceElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.Type;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithAnnotations;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithConnectionProviders;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithMessageSources;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithOperationContainers;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithParameters;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.FieldWrapper;
import org.mule.runtime.module.extension.internal.introspection.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.introspection.version.VersionResolver;
import org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty.CallbackPhase;
import org.mule.runtime.module.extension.internal.model.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ExceptionEnricherModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.InterceptingModelProperty;
import org.mule.runtime.module.extension.internal.model.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.model.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.model.property.SourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSourceFactory;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;


/**
 * Implementation of {@link Describer} which generates a {@link ExtensionDeclarer} by scanning annotations on a type provided in
 * the constructor
 *
 * @since 3.7.0
 */
public final class AnnotationsBasedDescriber implements Describer {

  /**
   * The ID which represents {@code this} {@link Describer} in a {@link DescriberManifest}
   */
  public static final String DESCRIBER_ID = "annotations";

  /**
   * A {@link DescriberManifest} property key which points to the class which should be introspected by instances of this class
   */
  public static final String TYPE_PROPERTY_NAME = "type";

  public static final String DEFAULT_CONNECTION_PROVIDER_NAME = "connection";
  private static final String CUSTOM_CONNECTION_PROVIDER_SUFFIX = "-" + DEFAULT_CONNECTION_PROVIDER_NAME;
  private static final String CONNECTION_PROVIDER = "Connection Provider";
  private static final String CONFIGURATION = "Configuration";
  private static final String SOURCE = "Source";
  private static final String OPERATION = "Operation";

  private final Class<?> extensionType;
  private final VersionResolver versionResolver;
  private final ClassTypeLoader typeLoader;

  private final Map<MethodElement, OperationDeclarer> operationDeclarers = new HashMap<>();
  private final Map<Class<?>, SourceDeclarer> sourceDeclarers = new HashMap<>();
  private final Map<Class<?>, ConnectionProviderDeclarer> connectionProviderDeclarers = new HashMap<>();

  private List<ParameterDeclarerContributor> fieldParameterContributors;
  private List<ParameterDeclarerContributor> methodParameterContributors;

  public AnnotationsBasedDescriber(Class<?> extensionType, VersionResolver versionResolver) {
    checkArgument(extensionType != null, format("describer %s does not specify an extension type", getClass().getName()));
    this.extensionType = extensionType;
    this.versionResolver = versionResolver;
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(extensionType.getClassLoader());

    fieldParameterContributors =
        ImmutableList.of(new InfrastructureFieldContributor(), new FunctionParameterTypeContributor(typeLoader));
    methodParameterContributors = ImmutableList.of(new ParameterResolverParameterTypeContributor(typeLoader),
                                                   new FunctionParameterTypeContributor(typeLoader));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ExtensionDeclarer describe(DescribingContext context) {
    final ExtensionElement extensionElement = ExtensionTypeFactory.getExtensionType(extensionType);
    Extension extension = getExtension(extensionType);
    ExtensionDeclarer declarer =
        context.getExtensionDeclarer()
            .named(extension.name())
            .onVersion(getVersion(extension))
            .fromVendor(extension.vendor())
            .withCategory(extension.category())
            .withMinMuleVersion(new MuleVersion(extension.minMuleVersion()))
            .describedAs(extension.description())
            .withModelProperty(new ImplementingTypeModelProperty(extensionType));

    addExceptionEnricher(extensionElement, declarer);

    declareConfigurations(declarer, extensionElement);
    declareConnectionProviders(declarer, extensionElement);

    if (!CollectionUtils.isEmpty(extensionElement.getConfigurations())) {
      declareOperations(declarer, declarer, extensionElement.getOperations(), false);
      extensionElement.getSources().forEach(source -> declareMessageSource(declarer, declarer, source, false));
    }

    return declarer;
  }

  private <M extends WithAnnotations> HasModelProperties addExceptionEnricher(M model, HasModelProperties declarer) {
    getExceptionEnricherFactory(model).map(ExceptionEnricherModelProperty::new).ifPresent(declarer::withModelProperty);
    return declarer;
  }

  private String getVersion(Extension extension) {
    return versionResolver.resolveVersion(extension);
  }

  private void declareConfigurations(ExtensionDeclarer declaration, ExtensionElement extensionElement) {
    List<ConfigurationElement> configurations = extensionElement.getConfigurations();
    if (configurations.isEmpty()) {
      declareConfiguration(declaration, extensionElement, extensionElement);
    } else {
      for (ConfigurationElement configuration : configurations) {
        declareConfiguration(declaration, extensionElement, configuration);
      }
    }
  }

  private void declareConfiguration(ExtensionDeclarer declarer, ExtensionElement extensionType,
                                    ComponentElement configurationType) {
    checkConfigurationIsNotAnOperation(configurationType.getDeclaringClass());
    ConfigurationDeclarer configurationDeclarer;

    Optional<Configuration> configurationAnnotation = configurationType.getAnnotation(Configuration.class);
    if (configurationAnnotation.isPresent()) {
      final Configuration configuration = configurationAnnotation.get();
      configurationDeclarer = declarer.withConfig(configuration.name()).describedAs(configuration.description());
    } else {
      configurationDeclarer =
          declarer.withConfig(DEFAULT_CONFIG_NAME).describedAs(DEFAULT_CONFIG_DESCRIPTION);
    }

    configurationDeclarer.withModelProperty(
                                            new ConfigurationFactoryModelProperty(new TypeAwareConfigurationFactory(configurationType
                                                .getDeclaringClass(),
                                                                                                                    extensionType
                                                                                                                        .getDeclaringClass()
                                                                                                                        .getClassLoader())))
        .withModelProperty(new ImplementingTypeModelProperty(configurationType.getDeclaringClass()));

    declareFieldBasedParameters(configurationDeclarer, configurationType.getParameters(),
                                new ParameterDeclarationContext(CONFIGURATION, configurationDeclarer.getDeclaration()));

    declareOperations(declarer, configurationDeclarer, configurationType);
    declareMessageSources(declarer, configurationDeclarer, configurationType);
    declareConnectionProviders(configurationDeclarer, configurationType);
  }

  private void declareMessageSources(ExtensionDeclarer extensionDeclarer, HasSourceDeclarer declarer,
                                     WithMessageSources typeComponent) {
    // TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
    typeComponent.getSources().forEach(source -> declareMessageSource(extensionDeclarer, declarer, source, true));
  }

  private void declareMessageSource(ExtensionDeclarer extensionDeclarer,
                                    HasSourceDeclarer declarer,
                                    SourceElement sourceType,
                                    boolean supportsConfig) {
    //TODO: MULE-9220 - Add a syntax validator which checks that the sourceType doesn't implement
    // any lifecycle interface
    final Optional<ExtensionParameter> configParameter = getConfigParameter(sourceType);
    final Optional<ExtensionParameter> connectionParameter = getConnectionParameter(sourceType);

    if (isInvalidConfigSupport(supportsConfig, configParameter, connectionParameter)) {
      throw new IllegalSourceModelDefinitionException(
                                                      format("Source '%s' is defined at the extension level but it requires a config parameter. "
                                                          + "Remove such parameter or move the source to the proper config",
                                                             sourceType.getName()));
    }

    HasSourceDeclarer actualDeclarer =
        (HasSourceDeclarer) selectDeclarerBasedOnConfig(extensionDeclarer, (Declarer) declarer, configParameter,
                                                        connectionParameter);

    SourceDeclarer source = sourceDeclarers.get(sourceType.getDeclaringClass());
    if (source != null) {
      actualDeclarer.withMessageSource(source);
      return;
    }

    source = actualDeclarer.withMessageSource(sourceType.getAlias());
    List<java.lang.reflect.Type> sourceGenerics = sourceType.getSuperClassGenerics();

    if (sourceGenerics.size() != 2) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalModelDefinitionException(format("Message source class '%s' was expected to have 2 generic types "
          + "(one for the Payload type and another for the Attributes type) but %d were found",
                                                       sourceType.getName(),
                                                       sourceGenerics.size()));
    }

    source
        .hasResponse(sourceType.isAnnotatedWith(EmitsResponse.class))
        .withModelProperty(new SourceFactoryModelProperty(new DefaultSourceFactory(sourceType.getDeclaringClass())))
        .withModelProperty(new ImplementingTypeModelProperty(sourceType.getDeclaringClass()))
        .withOutput().ofType(typeLoader.load(sourceGenerics.get(0)));
    source.withOutputAttributes().ofType(typeLoader.load(sourceGenerics.get(1)));

    addExceptionEnricher(sourceType, source);

    declareSourceParameters(sourceType, source);
    declareSourceCallback(sourceType, source);

    sourceDeclarers.put(sourceType.getDeclaringClass(), source);
  }

  /**
   * Declares the parameters needed to generate messages
   */
  private void declareSourceParameters(SourceElement sourceType, SourceDeclarer source) {
    declareFieldBasedParameters(source, sourceType.getParameters(),
                                new ParameterDeclarationContext(SOURCE, source.getDeclaration()))
                                    .forEach(p -> p.withExpressionSupport(NOT_SUPPORTED));
  }

  private void declareSourceCallback(SourceElement sourceType, SourceDeclarer source) {
    final Optional<MethodElement> onResponseMethod = sourceType.getOnResponseMethod();
    final Optional<MethodElement> onErrorMethod = sourceType.getOnErrorMethod();

    //TODO: MULE-9220 add syntax validator to check that none of these use @UseConfig or @Connection
    declareSourceCallbackParameters(source, onResponseMethod, ON_SUCCESS);
    declareSourceCallbackParameters(source, onErrorMethod, ON_ERROR);

    source.withModelProperty(new SourceCallbackModelProperty(getMethod(onResponseMethod), getMethod(onErrorMethod)));
  }

  private void declareSourceCallbackParameters(SourceDeclarer source, Optional<MethodElement> sourceCallback,
                                               CallbackPhase callbackPhase) {
    sourceCallback.ifPresent(method -> declareMethodBasedParameters(
                                                                    source, method.getParameters(),
                                                                    new ParameterDeclarationContext(SOURCE,
                                                                                                    source.getDeclaration()))
                                                                                                        .forEach(p -> p
                                                                                                            .withModelProperty(new CallbackParameterModelProperty(callbackPhase))));
  }

  private Optional<Method> getMethod(Optional<MethodElement> method) {
    return method.map(MethodElement::getMethod);
  }

  private void declareOperations(ExtensionDeclarer extensionDeclarer, HasOperationDeclarer declarer,
                                 WithOperationContainers operationContainers) {
    operationContainers.getOperationContainers()
        .forEach(operationContainer -> declareOperations(extensionDeclarer, declarer, operationContainer));
  }

  private Class<?>[] getOperationClasses(Class<?> extensionType) {
    Operations operations = extensionType.getAnnotation(Operations.class);
    return operations == null ? ArrayUtils.EMPTY_CLASS_ARRAY : operations.value();
  }


  private void declareOperations(ExtensionDeclarer extensionDeclarer, HasOperationDeclarer declarer,
                                 OperationContainerElement operationsContainer) {
    declareOperations(extensionDeclarer, declarer, operationsContainer.getOperations(), true);
  }

  private void declareOperations(ExtensionDeclarer extensionDeclarer, HasOperationDeclarer declarer,
                                 List<MethodElement> operations, boolean supportsConfig) {

    for (MethodElement operationMethod : operations) {
      final Class<?> declaringClass = operationMethod.getDeclaringClass();
      checkOperationIsNotAnExtension(declaringClass);

      final Method method = operationMethod.getMethod();
      final Optional<ExtensionParameter> configParameter = getConfigParameter(operationMethod);
      final Optional<ExtensionParameter> connectionParameter = getConnectionParameter(operationMethod);

      if (isInvalidConfigSupport(supportsConfig, configParameter, connectionParameter)) {
        throw new IllegalOperationModelDefinitionException(format(
                                                                  "Operation '%s' is defined at the extension level but it requires a config. "
                                                                      + "Remove such parameter or move the operation to the proper config",
                                                                  method.getName()));
      }

      HasOperationDeclarer actualDeclarer =
          (HasOperationDeclarer) selectDeclarerBasedOnConfig(extensionDeclarer, (Declarer) declarer, configParameter,
                                                             connectionParameter);

      if (operationDeclarers.containsKey(operationMethod)) {
        actualDeclarer.withOperation(operationDeclarers.get(operationMethod));
        continue;
      }

      final OperationDeclarer operation = actualDeclarer.withOperation(operationMethod.getAlias())
          .withModelProperty(new ImplementingMethodModelProperty(method))
          .withModelProperty(new OperationExecutorModelProperty(new ReflectiveOperationExecutorFactory<>(declaringClass,
                                                                                                         method)));

      addExceptionEnricher(operationMethod, operation);
      operation.withOutput().ofType(getMethodReturnType(method, typeLoader));
      operation.withOutputAttributes().ofType(getMethodReturnAttributesType(method, typeLoader));
      addInterceptingCallbackModelProperty(operationMethod, operation);
      addPagedOperationModelProperty(operationMethod, operation, supportsConfig);
      declareMethodBasedParameters(operation, operationMethod.getParameters(),
                                   new ParameterDeclarationContext(OPERATION, operation.getDeclaration()));
      calculateExtendedTypes(declaringClass, method, operation);
      operationDeclarers.put(operationMethod, operation);
    }
  }

  private boolean isInvalidConfigSupport(boolean supportsConfig, Optional<ExtensionParameter>... parameters) {
    return !supportsConfig && Stream.of(parameters).anyMatch(Optional::isPresent);
  }

  private Declarer selectDeclarerBasedOnConfig(ExtensionDeclarer extensionDeclarer,
                                               Declarer declarer,
                                               Optional<ExtensionParameter>... parameters) {

    for (Optional<ExtensionParameter> parameter : parameters) {
      if (parameter.isPresent()) {
        return declarer;
      }
    }

    return extensionDeclarer;
  }

  private Optional<ExtensionParameter> getConfigParameter(WithParameters element) {
    return element.getParametersAnnotatedWith(UseConfig.class).stream().findFirst();
  }

  private Optional<ExtensionParameter> getConnectionParameter(WithParameters element) {
    return element.getParametersAnnotatedWith(Connection.class).stream().findFirst();
  }

  private void declareConnectionProviders(HasConnectionProviderDeclarer declarer,
                                          WithConnectionProviders withConnectionProviders) {
    withConnectionProviders.getConnectionProviders().forEach(provider -> declareConnectionProvider(declarer, provider));
  }

  private void declareConnectionProvider(HasConnectionProviderDeclarer declarer, ConnectionProviderElement providerType) {
    final Class<?> providerClass = providerType.getDeclaringClass();
    ConnectionProviderDeclarer providerDeclarer = connectionProviderDeclarers.get(providerClass);
    if (providerDeclarer != null) {
      declarer.withConnectionProvider(providerDeclarer);
      return;
    }

    String name = providerType.getAlias() + CUSTOM_CONNECTION_PROVIDER_SUFFIX;
    String description = providerType.getDescription();

    if (providerType.getName().equals(providerType.getAlias())) {
      name = DEFAULT_CONNECTION_PROVIDER_NAME;
    }

    List<Class<?>> providerGenerics = providerType.getInterfaceGenerics(ConnectionProvider.class);

    if (providerGenerics.size() != 1) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalConnectionProviderModelDefinitionException(
                                                                  format("Connection provider class '%s' was expected to have 1 generic type "
                                                                      + "(for the connection type) but %d were found",
                                                                         providerType.getName(), providerGenerics.size()));
    }

    providerDeclarer = declarer.withConnectionProvider(name).describedAs(description)
        .withModelProperty(new ConnectionProviderFactoryModelProperty(new DefaultConnectionProviderFactory<>(providerClass,
                                                                                                             extensionType
                                                                                                                 .getClassLoader())))
        .withModelProperty(new ConnectionTypeModelProperty(providerGenerics.get(0)))
        .withModelProperty(new ImplementingTypeModelProperty(providerClass));

    ConnectionManagementType managementType = NONE;
    if (PoolingConnectionProvider.class.isAssignableFrom(providerClass)) {
      managementType = POOLING;
    } else if (CachedConnectionProvider.class.isAssignableFrom(providerClass)) {
      managementType = CACHED;
    }

    providerDeclarer.withConnectionManagementType(managementType);

    connectionProviderDeclarers.put(providerClass, providerDeclarer);
    declareFieldBasedParameters(providerDeclarer, providerType.getParameters(),
                                new ParameterDeclarationContext(CONNECTION_PROVIDER, providerDeclarer.getDeclaration()));
  }

  private List<ParameterDeclarer> declareFieldBasedParameters(ParameterizedDeclarer component,
                                                              List<ExtensionParameter> parameters,
                                                              ParameterDeclarationContext componentName) {
    return declareParameters(component, parameters, fieldParameterContributors, componentName, empty());
  }

  private List<ParameterDeclarer> declareMethodBasedParameters(ParameterizedDeclarer component,
                                                               List<ExtensionParameter> parameters,
                                                               ParameterDeclarationContext componentName) {
    return declareParameters(component, parameters, methodParameterContributors, componentName, empty());
  }

  private List<ParameterDeclarer> declareParameters(ParameterizedDeclarer component,
                                                    List<? extends ExtensionParameter> parameters,
                                                    List<ParameterDeclarerContributor> contributors,
                                                    ParameterDeclarationContext declarationContext,
                                                    Optional<ParameterGroupDeclarer> parameterGroupDeclarer) {
    List<ParameterDeclarer> declarerList = new ArrayList<>();
    checkAnnotationsNotUsedMoreThanOnce(parameters, Connection.class, UseConfig.class, MetadataKeyId.class);

    for (ExtensionParameter extensionParameter : parameters) {

      if (!extensionParameter.shouldBeAdvertised()) {
        continue;
      }

      if (declaredAsGroup(component, contributors, declarationContext, extensionParameter)) {
        continue;
      }

      ParameterGroupDeclarer groupDeclarer = parameterGroupDeclarer.orElseGet(component::onDefaultParameterGroup);

      ParameterDeclarer parameter;
      if (extensionParameter.isRequired()) {
        parameter = groupDeclarer.withRequiredParameter(extensionParameter.getAlias());
      } else {
        parameter = groupDeclarer.withOptionalParameter(extensionParameter.getAlias())
            .defaultingTo(extensionParameter.defaultValue().isPresent() ? extensionParameter.defaultValue().get() : null);
      }

      parameter.ofType(extensionParameter.getMetadataType(typeLoader)).describedAs(extensionParameter.getDescription());
      parseParameterRole(extensionParameter, parameter);
      parseExpressionSupport(extensionParameter, parameter);
      parseNullSafe(extensionParameter, parameter);
      addTypeRestrictions(extensionParameter, parameter);
      parseLayout(extensionParameter, parameter);
      addImplementingTypeModelProperty(extensionParameter, parameter);
      parseXmlHints(extensionParameter, parameter);
      contributors.forEach(contributor -> contributor.contribute(extensionParameter, parameter, declarationContext));
      declarerList.add(parameter);
    }

    return declarerList;
  }

  private boolean declaredAsGroup(ParameterizedDeclarer component,
                                  List<ParameterDeclarerContributor> contributors,
                                  ParameterDeclarationContext declarationContext,
                                  ExtensionParameter groupParameter) {

    ParameterGroup groupAnnotation = groupParameter.getAnnotation(ParameterGroup.class).orElse(null);
    if (groupAnnotation == null) {
      return false;
    }

    final String groupName = groupAnnotation.value();
    if (ParameterGroupModel.DEFAULT_GROUP_NAME.equals(groupName)) {
      throw new IllegalParameterModelDefinitionException(
                                                         format("%s '%s' defines parameter group of name '%s' which is the default one. "
                                                             + "@%s cannot be used with the default group name",
                                                                getComponentModelTypeName(component),
                                                                ((NamedDeclaration) component.getDeclaration()).getName(),
                                                                groupName,
                                                                ParameterGroup.class.getSimpleName()));
    }

    final Type type = groupParameter.getType();

    final List<FieldElement> nestedGroups = type.getAnnotatedFields(ParameterGroup.class);
    if (!nestedGroups.isEmpty()) {
      throw new IllegalParameterModelDefinitionException(format(
                                                                "Class '%s' is used as a @%s but contains fields which also hold that annotation. Nesting groups is not allowed. "
                                                                    + "Offending fields are: [%s]",
                                                                type.getName(),
                                                                ParameterGroup.class.getSimpleName(),
                                                                nestedGroups.stream().map(element -> element.getName())
                                                                    .collect(joining(","))));
    }

    final List<FieldElement> annotatedParameters = type.getAnnotatedFields(Parameter.class);

    // TODO: MULE-9220: Add a syntax validator for this
    if (groupParameter.isAnnotatedWith(org.mule.runtime.extension.api.annotation.param.Optional.class)) {
      throw new IllegalParameterModelDefinitionException(format(
                                                                "@%s can not be applied alongside with @%s. Affected parameter is [%s].",
                                                                org.mule.runtime.extension.api.annotation.param.Optional.class
                                                                    .getSimpleName(),
                                                                ParameterGroup.class.getSimpleName(),
                                                                groupParameter.getName()));
    }

    ParameterGroupDeclarer declarer = component.withParameterGroup(groupName);
    if (declarer.getDeclaration().getModelProperty(ParameterGroupModelProperty.class).isPresent()) {
      throw new IllegalParameterModelDefinitionException(format("Parameter group '%s' has already been declared on %s '%s'",
                                                                groupName,
                                                                getComponentDeclarationTypeName(component),
                                                                ((NamedDeclaration) component.getDeclaration()).getName()));
    } else {
      declarer.withModelProperty(new ParameterGroupModelProperty(new ParameterGroupDescriptor(
                                                                                              groupName, type, groupParameter
                                                                                                  .getDeclaringElement())));
    }

    type.getAnnotation(ExclusiveOptionals.class).ifPresent(annotation -> {
      Set<String> optionalParamNames = annotatedParameters.stream()
          .filter(f -> !f.isRequired())
          .map(f -> f.getAlias())
          .collect(toSet());

      declarer.withExclusiveOptionals(optionalParamNames, annotation.isOneRequired());
    });


    parseLayoutAnnotations(groupParameter, LayoutModel.builder()).ifPresent(declarer::withLayout);

    if (!annotatedParameters.isEmpty()) {
      declareParameters(component, annotatedParameters, contributors, declarationContext, ofNullable(declarer));
    } else {
      declareParameters(component, getFieldsWithGetters(type.getDeclaringClass()).stream().map(FieldWrapper::new)
          .collect(toList()), contributors, declarationContext, ofNullable(declarer));
    }

    return true;
  }

  private void checkConfigurationIsNotAnOperation(Class<?> configurationType) {
    Class<?>[] operationClasses = getOperationClasses(extensionType);
    for (Class<?> operationClass : operationClasses) {
      if (configurationType.isAssignableFrom(operationClass) || operationClass.isAssignableFrom(configurationType)) {
        throw new IllegalConfigurationModelDefinitionException(
                                                               format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                      configurationType.getName(), operationClass.getName()));
      }
    }
  }

  private void checkOperationIsNotAnExtension(Class<?> operationType) {
    if (operationType.isAssignableFrom(extensionType) || extensionType.isAssignableFrom(operationType)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                operationType.getName(), extensionType.getName()));
    }
  }

  private void calculateExtendedTypes(Class<?> actingClass, Method method, OperationDeclarer operation) {
    ExtensionOf extensionOf = method.getAnnotation(ExtensionOf.class);
    if (extensionOf == null) {
      extensionOf = actingClass.getAnnotation(ExtensionOf.class);
    }

    if (extensionOf != null) {
      operation.withModelProperty(new ExtendingOperationModelProperty(extensionOf.value()));
    } else if (isExtensible()) {
      operation.withModelProperty(new ExtendingOperationModelProperty(extensionType));
    }
  }

  private boolean isExtensible() {
    return extensionType.getAnnotation(Extensible.class) != null;
  }

  private void parseParameterRole(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parameter.withRole(roleOf(extensionParameter.getAnnotation(Content.class)));
  }

  private void parseExpressionSupport(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    final Optional<Expression> annotation = extensionParameter.getAnnotation(Expression.class);
    if (annotation.isPresent()) {
      parameter.withExpressionSupport(getExpressionSupport(annotation.get()));
    }
  }

  private void parseNullSafe(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    if (extensionParameter.isAnnotatedWith(NullSafe.class)) {
      if (extensionParameter.isRequired()) {
        throw new IllegalParameterModelDefinitionException(
                                                           format("Parameter '%s' is required but annotated with '@%s', which is redundant",
                                                                  extensionParameter.getName(), NullSafe.class.getSimpleName()));
      }

      parameter.getDeclaration().getType().accept(new BasicTypeMetadataVisitor() {

        @Override
        protected void visitBasicType(MetadataType metadataType) {
          throw new IllegalParameterModelDefinitionException(
                                                             format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                                                                 + "used with complex types (Pojos, Lists, Maps)",
                                                                    extensionParameter.getName(), NullSafe.class.getSimpleName(),
                                                                    extensionParameter.getType().getName()));
        }
      });

      parameter.withModelProperty(new NullSafeModelProperty());
    }
  }

  private void parseLayout(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parseLayoutAnnotations(extensionParameter, LayoutModel.builder()).ifPresent(parameter::withLayout);
  }

  private void parseXmlHints(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(XmlHints.class).ifPresent(
                                                               hints -> parameter.withDsl(ElementDslModel.builder()
                                                                   .allowsInlineDefinition(hints.allowInlineDefinition())
                                                                   .allowsReferences(hints.allowReferences())
                                                                   .allowTopLevelDefinition(hints.allowTopLevelDefinition())
                                                                   .build()));
  }

  private void checkAnnotationsNotUsedMoreThanOnce(List<? extends ExtensionParameter> parameters,
                                                   Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      final long count = parameters.stream().filter(param -> param.isAnnotatedWith(annotation)).count();
      if (count > 1) {
        throw new IllegalModelDefinitionException(
                                                  format("The defined parameters %s from %s, uses the annotation @%s more than once",
                                                         parameters.stream().map(p -> p.getName()).collect(toList()),
                                                         parameters.get(0).getOwnerDescription(), annotation.getSimpleName()));
      }
    }
  }

  private void addTypeRestrictions(WithAnnotations withAnnotations, ParameterDeclarer parameter) {
    Optional<RestrictedTo> typeRestriction = withAnnotations.getAnnotation(RestrictedTo.class);
    if (typeRestriction.isPresent()) {
      parameter.withModelProperty(new TypeRestrictionModelProperty<>(typeRestriction.get().value()));
    }
  }

  private void addImplementingTypeModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    AnnotatedElement element = extensionParameter.getDeclaringElement();
    parameter.withModelProperty(element instanceof Field
        ? new DeclaringMemberModelProperty(((Field) element))
        : new ImplementingParameterModelProperty((java.lang.reflect.Parameter) element));
  }

  private void addPagedOperationModelProperty(MethodElement operationMethod, OperationDeclarer operation,
                                              boolean supportsConfig) {
    if (PagingProvider.class.isAssignableFrom(operationMethod.getReturnType())) {
      if (!supportsConfig) {
        throw new IllegalOperationModelDefinitionException(format(
                                                                  "Paged operation '%s' is defined at the extension level but it requires a config, since connections "
                                                                      + "are required for paging",
                                                                  operationMethod.getName()));
      }
      operation.withModelProperty(new PagedOperationModelProperty());
    }
  }

  private void addInterceptingCallbackModelProperty(MethodElement operationMethod, OperationDeclarer operation) {
    if (InterceptingCallback.class.isAssignableFrom(operationMethod.getReturnType())) {
      operation.withModelProperty(new InterceptingModelProperty());
    }
  }
}
