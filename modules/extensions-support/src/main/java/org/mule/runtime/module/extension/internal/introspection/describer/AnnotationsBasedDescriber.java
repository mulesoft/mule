/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import com.google.common.collect.ImmutableList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getGenericTypeAt;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExceptionEnricherFactory;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExtension;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseLayoutAnnotations;
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
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExtensionOf;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.property.PagedOperationModelProperty;
import org.mule.runtime.extension.api.introspection.streaming.PagingProvider;
import org.mule.runtime.extension.api.manifest.DescriberManifest;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ComponentElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConfigurationElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConnectionProviderElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionTypeFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.model.FieldElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.InfrastructureTypeMapping;
import org.mule.runtime.module.extension.internal.introspection.describer.model.MethodElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.OperationContainerElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ParameterElement;
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
import org.mule.runtime.module.extension.internal.model.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ExceptionEnricherModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.InfrastructureParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.InterceptingModelProperty;
import org.mule.runtime.module.extension.internal.model.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.model.property.SourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSourceFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private List<ParameterDeclarerContributor> fieldParameterContributor = ImmutableList.of(new InfrastructureFieldContributor());
  private List<ParameterDeclarerContributor> methodParameterContributor =
      ImmutableList.of(new ParameterResolverParameterTypeContributor());

  public AnnotationsBasedDescriber(Class<?> extensionType, VersionResolver versionResolver) {
    checkArgument(extensionType != null, format("describer %s does not specify an extension type", getClass().getName()));
    this.extensionType = extensionType;
    this.versionResolver = versionResolver;
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(extensionType.getClassLoader());
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
        .withModelProperty(new SourceFactoryModelProperty(new DefaultSourceFactory(sourceType.getDeclaringClass())))
        .withModelProperty(new ImplementingTypeModelProperty(sourceType.getDeclaringClass()))
        .withOutput().ofType(typeLoader.load(sourceGenerics.get(0)));
    source.withOutputAttributes().ofType(typeLoader.load(sourceGenerics.get(1)));

    addExceptionEnricher(sourceType, source);
    declareFieldBasedParameters(source, sourceType.getParameters(),
                                new ParameterDeclarationContext(SOURCE, source.getDeclaration()));

    sourceDeclarers.put(sourceType.getDeclaringClass(), source);
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
    return declareParameters(component, parameters, this.fieldParameterContributor, componentName, null);
  }

  private List<ParameterDeclarer> declareMethodBasedParameters(ParameterizedDeclarer component,
                                                               List<ExtensionParameter> parameters,
                                                               ParameterDeclarationContext componentName) {
    return declareParameters(component, parameters, this.methodParameterContributor, componentName, null);
  }

  private List<ParameterDeclarer> declareParameters(ParameterizedDeclarer component, List<ExtensionParameter> parameters,
                                                    List<ParameterDeclarerContributor> contributors,
                                                    ParameterDeclarationContext declarationContext,
                                                    ExtensionParameter parameterGroupOwner) {
    List<ParameterDeclarer> declarerList = new ArrayList<>();
    checkAnnotationsNotUsedMoreThanOnce(parameters, Connection.class, UseConfig.class, MetadataKeyId.class);
    for (ExtensionParameter extensionParameter : parameters) {
      if (extensionParameter.shouldBeAdvertised()) {
        ParameterDeclarer parameter =
            extensionParameter.isRequired() ? component.withRequiredParameter(extensionParameter.getAlias())
                : component.withOptionalParameter(extensionParameter.getAlias())
                    .defaultingTo(extensionParameter.defaultValue().isPresent() ? extensionParameter.defaultValue().get() : null);
        parameter.ofType(extensionParameter.getMetadataType(typeLoader));

        parameter.describedAs(EMPTY);
        addExpressionModelProperty(extensionParameter, parameter);
        addTypeRestrictions(extensionParameter, parameter);
        addLayoutModelProperty(extensionParameter, parameter);
        addImplementingTypeModelProperty(extensionParameter, parameter);
        addXmlHintsModelProperty(extensionParameter, parameter);
        contributors.forEach(contributor -> contributor.contribute(extensionParameter, parameter, declarationContext));
        declarerList.add(parameter);
      }

      if (extensionParameter.isAnnotatedWith(org.mule.runtime.extension.api.annotation.ParameterGroup.class)) {
        final Type type = extensionParameter.getType();
        final List<ExtensionParameter> annotatedParameters =
            ImmutableList.<ExtensionParameter>builder().addAll(type.getAnnotatedFields(Parameter.class))
                .addAll(type.getAnnotatedFields(org.mule.runtime.extension.api.annotation.ParameterGroup.class)).build();

        // TODO: MULE-9220: Add a syntax validator for this
        if (extensionParameter.isAnnotatedWith(org.mule.runtime.extension.api.annotation.param.Optional.class)) {
          throw new IllegalParameterModelDefinitionException(format("@%s can not be applied along with @%s. Affected field [%s].",
                                                                    org.mule.runtime.extension.api.annotation.param.Optional.class
                                                                        .getSimpleName(),
                                                                    org.mule.runtime.extension.api.annotation.ParameterGroup.class
                                                                        .getSimpleName(),
                                                                    extensionParameter.getName()));
        }

        if (!annotatedParameters.isEmpty()) {
          if (extensionParameter.equals(parameterGroupOwner)) {
            throw new IllegalParameterModelDefinitionException(
                                                               format(
                                                                      "@%s cannot be applied recursively within the same class but field '%s' was found inside class '%s'",
                                                                      org.mule.runtime.extension.api.annotation.ParameterGroup.class
                                                                          .getSimpleName(),
                                                                      parameterGroupOwner.getName(),
                                                                      parameterGroupOwner.getType().getName()));
          } else {
            declareParameters(component, annotatedParameters, contributors, declarationContext, extensionParameter);
          }

        } else {
          declareParameters(component, getFieldsWithGetters(type.getDeclaringClass()).stream().map(FieldWrapper::new)
              .collect(toList()), contributors, declarationContext, extensionParameter);
        }
      }
    }

    return declarerList;
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

  private void addExpressionModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    final Optional<Expression> annotation = extensionParameter.getAnnotation(Expression.class);
    if (annotation.isPresent()) {
      parameter.withExpressionSupport(getExpressionSupport(annotation.get()));
    }
  }

  private void addLayoutModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parseLayoutAnnotations(extensionParameter, LayoutModel.builder()).ifPresent(parameter::withLayout);
  }

  private void addXmlHintsModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(XmlHints.class).ifPresent(
                                                               hints -> parameter.withDsl(ElementDslModel.builder()
                                                                   .allowsInlineDefinition(hints.allowInlineDefinition())
                                                                   .allowsReferences(hints.allowReferences())
                                                                   .allowTopLevelDefinition(hints.allowTopLevelDefinition())
                                                                   .build()));
  }

  private void checkAnnotationsNotUsedMoreThanOnce(List<ExtensionParameter> parameters,
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
    parameter.withModelProperty(extensionParameter.isFieldBased()
        ? new DeclaringMemberModelProperty(((FieldElement) extensionParameter).getField())
        : new ImplementingParameterModelProperty(
                                                 ((ParameterElement) extensionParameter).getParameter()));
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

  private interface ParameterDeclarerContributor {

    void contribute(ExtensionParameter parameter, ParameterDeclarer declarer, ParameterDeclarationContext declarationContext);
  }


  private static class InfrastructureFieldContributor implements ParameterDeclarerContributor {

    @Override
    public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                           ParameterDeclarationContext declarationContext) {
      if (InfrastructureTypeMapping.getMap().containsKey(parameter.getType().getDeclaringClass())) {
        declarer.withModelProperty(new InfrastructureParameterModelProperty());
        declarer.withExpressionSupport(NOT_SUPPORTED);
      }
    }
  }


  private class ParameterResolverParameterTypeContributor implements ParameterDeclarerContributor {

    @Override
    public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                           ParameterDeclarationContext declarationContext) {
      MetadataType metadataType = parameter.getMetadataType(typeLoader);
      if (ParameterResolver.class.isAssignableFrom(parameter.getType().getDeclaringClass())) {
        final Optional<MetadataType> expressionResolverType = getGenericTypeAt(metadataType, 0, typeLoader);
        if (expressionResolverType.isPresent()) {
          metadataType = expressionResolverType.get();
        } else {
          throw new IllegalParameterModelDefinitionException(String
              .format(
                      "The parameter [%s] from the Operation [%s] doesn't specify the %s parameterized type",
                      parameter.getName(),
                      declarationContext.getName(),
                      ParameterResolver.class.getSimpleName()));
        }
        declarer.ofType(metadataType);
        declarer.withModelProperty(new ParameterResolverTypeModelProperty());
      }
    }
  }
}
