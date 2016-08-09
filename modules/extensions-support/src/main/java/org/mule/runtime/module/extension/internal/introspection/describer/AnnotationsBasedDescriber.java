/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExceptionEnricherFactory;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExtension;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseLayoutAnnotations;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetterAndSetters;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnAttributesType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.MuleVersion;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.core.util.ArrayUtils;
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
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasConnectionProviderDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.property.LayoutModelProperty;
import org.mule.runtime.extension.api.manifest.DescriberManifest;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.extension.xml.dsl.api.property.XmlHintsModelProperty;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConfigurationElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConnectionProviderElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ComponentElement;
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
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.FieldWrapper;
import org.mule.runtime.module.extension.internal.introspection.version.VersionResolver;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.InfrastructureParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.InterceptingModelProperty;
import org.mule.runtime.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSourceFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  private final Class<?> extensionType;
  private final VersionResolver versionResolver;
  private final ClassTypeLoader typeLoader;

  private final Multimap<Class<?>, OperationDeclarer> operationDeclarers = LinkedListMultimap.create();
  private final Map<Class<?>, SourceDeclarer> sourceDeclarers = new HashMap<>();
  private final Map<Class<?>, ConnectionProviderDeclarer> connectionProviderDeclarers = new HashMap<>();

  private List<ParameterDeclarerContributor> fielParameterContributor = ImmutableList.of(new InfrastructureFieldContributor());

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
    final ExtensionElement extensionElement = ExtensionTypeFactory.getExtensionType(this.extensionType);
    Extension extension = getExtension(this.extensionType);
    ExtensionDeclarer declaration =
        context.getExtensionDeclarer().named(extension.name()).onVersion(getVersion(extension)).fromVendor(extension.vendor())
            .withCategory(extension.category()).withMinMuleVersion(new MuleVersion(extension.minMuleVersion()))
            .describedAs(extension.description()).withExceptionEnricherFactory(getExceptionEnricherFactory(extensionElement))
            .withModelProperty(new ImplementingTypeModelProperty(this.extensionType));

    declareConfigurations(declaration, extensionElement);
    declareOperations(declaration, extensionElement);
    declareConnectionProviders(declaration, extensionElement);
    declareMessageSources(declaration, extensionElement);

    return declaration;
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

  private void declareConfiguration(ExtensionDeclarer declaration, ExtensionElement extensionType,
                                    ComponentElement configurationType) {
    checkConfigurationIsNotAnOperation(configurationType.getDeclaredClass());
    ConfigurationDeclarer configurationDeclarer;

    Optional<Configuration> configurationAnnotation = configurationType.getAnnotation(Configuration.class);
    if (configurationAnnotation.isPresent()) {
      final Configuration configuration = configurationAnnotation.get();
      configurationDeclarer = declaration.withConfig(configuration.name()).describedAs(configuration.description());
    } else {
      configurationDeclarer =
          declaration.withConfig(Extension.DEFAULT_CONFIG_NAME).describedAs(Extension.DEFAULT_CONFIG_DESCRIPTION);
    }

    configurationDeclarer
        .createdWith(new TypeAwareConfigurationFactory(configurationType.getDeclaredClass(),
                                                       extensionType.getDeclaredClass().getClassLoader()))
        .withModelProperty(new ImplementingTypeModelProperty(configurationType.getDeclaredClass()));

    declareFieldBasedParameters(configurationDeclarer, configurationType.getParameters());

    if (!extensionType.equals(configurationType)) {
      declareOperations(configurationDeclarer, configurationType);
      declareMessageSources(configurationDeclarer, configurationType);
      declareConnectionProviders(configurationDeclarer, configurationType);
    }
  }

  private void declareMessageSources(HasSourceDeclarer declarer, WithMessageSources typeComponent) {
    // TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
    typeComponent.getSources().forEach(source -> declareMessageSource(declarer, source));
  }

  private void declareMessageSource(HasSourceDeclarer declarer, SourceElement sourceType) {
    SourceDeclarer source = sourceDeclarers.get(sourceType.getDeclaredClass());
    if (source != null) {
      declarer.withMessageSource(source);
      return;
    }

    source = declarer.withMessageSource(sourceType.getAlias());
    List<java.lang.reflect.Type> sourceGenerics = sourceType.getSuperClassGenerics();

    if (sourceGenerics.size() != 2) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalModelDefinitionException(format("Message source class '%s' was expected to have 2 generic types "
          + "(one for the Payload type and another for the Attributes type) but %d were found", sourceType.getName(),
                                                       sourceGenerics.size()));
    }

    source.sourceCreatedBy(new DefaultSourceFactory(sourceType.getDeclaredClass()))
        .withExceptionEnricherFactory(getExceptionEnricherFactory(sourceType))
        .withModelProperty(new ImplementingTypeModelProperty(sourceType.getDeclaredClass()));

    source.withOutput().ofType(typeLoader.load(sourceGenerics.get(0)));
    source.withOutputAttributes().ofType(typeLoader.load(sourceGenerics.get(1)));
    declareFieldBasedParameters(source, sourceType.getParameters());

    sourceDeclarers.put(sourceType.getDeclaredClass(), source);
  }

  private void declareOperations(HasOperationDeclarer declarer, WithOperationContainers operationContainers) {
    operationContainers.getOperationContainers().forEach(operationContainer -> declareOperation(declarer, operationContainer));
  }

  private Class<?>[] getOperationClasses(Class<?> extensionType) {
    Operations operations = extensionType.getAnnotation(Operations.class);
    return operations == null ? ArrayUtils.EMPTY_CLASS_ARRAY : operations.value();
  }

  private void declareOperation(HasOperationDeclarer declarer, OperationContainerElement operationsContainer) {
    final Class<?> declaredClass = operationsContainer.getDeclaredClass();
    if (operationDeclarers.containsKey(declaredClass)) {
      operationDeclarers.get(declaredClass).forEach(declarer::withOperation);
      return;
    }

    checkOperationIsNotAnExtension(declaredClass);

    for (MethodElement operationMethod : operationsContainer.getOperations()) {

      final OperationDeclarer operation = declarer.withOperation(operationMethod.getAlias())
          .withModelProperty(new ImplementingMethodModelProperty(operationMethod.getMethod()))
          .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(declaredClass, operationMethod.getMethod()))
          .withExceptionEnricherFactory(getExceptionEnricherFactory(operationMethod));

      operation.withOutput().ofType(getMethodReturnType(operationMethod.getMethod(), typeLoader));
      operation.withOutputAttributes().ofType(getMethodReturnAttributesType(operationMethod.getMethod(), typeLoader));
      addInterceptingCallbackModelProperty(operationMethod, operation);
      declareMethodBasedParameters(operation, operationMethod.getParameters());
      calculateExtendedTypes(declaredClass, operationMethod.getMethod(), operation);
      operationDeclarers.put(declaredClass, operation);
    }
  }

  private void declareConnectionProviders(HasConnectionProviderDeclarer declarer,
                                          WithConnectionProviders withConnectionProviders) {
    withConnectionProviders.getConnectionProviders().forEach(provider -> declareConnectionProvider(declarer, provider));
  }

  private void declareConnectionProvider(HasConnectionProviderDeclarer declarer, ConnectionProviderElement providerType) {
    final Class<?> providerClass = providerType.getDeclaredClass();
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
      throw new IllegalConnectionProviderModelDefinitionException(format("Connection provider class '%s' was expected to have 1 generic type "
          + "(for the connection type) but %d were found", providerType.getName(), providerGenerics.size()));
    }

    providerDeclarer = declarer.withConnectionProvider(name).describedAs(description)
        .createdWith(new DefaultConnectionProviderFactory<>(providerClass, extensionType.getClassLoader()))
        .whichGivesConnectionsOfType(providerGenerics.get(0)).withModelProperty(new ImplementingTypeModelProperty(providerClass));

    ConnectionManagementType managementType = NONE;
    if (PoolingConnectionProvider.class.isAssignableFrom(providerClass)) {
      managementType = POOLING;
    } else if (CachedConnectionProvider.class.isAssignableFrom(providerClass)) {
      managementType = CACHED;
    }

    providerDeclarer.withConnectionManagementType(managementType);

    connectionProviderDeclarers.put(providerClass, providerDeclarer);
    declareFieldBasedParameters(providerDeclarer, providerType.getParameters());
  }

  private List<ParameterDeclarer> declareFieldBasedParameters(ParameterizedDeclarer component,
                                                              List<ExtensionParameter> parameters) {
    return declareParameters(component, parameters, this.fielParameterContributor, null);
  }

  private List<ParameterDeclarer> declareMethodBasedParameters(ParameterizedDeclarer component,
                                                               List<ExtensionParameter> parameters) {
    return declareParameters(component, parameters, emptyList(), null);
  }

  private List<ParameterDeclarer> declareParameters(ParameterizedDeclarer component, List<ExtensionParameter> parameters,
                                                    List<ParameterDeclarerContributor> contributors,
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
        addLayoutModelProperty(extensionParameter, parameter, parameterGroupOwner);
        addImplementingTypeModelProperty(extensionParameter, parameter);
        addXmlHintsModelProperty(extensionParameter, parameter);
        contributors.forEach(contributor -> contributor.contribute(extensionParameter, parameter));
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
          declareParameters(component, annotatedParameters, contributors, extensionParameter);
        } else {
          declareParameters(component, getFieldsWithGetterAndSetters(type.getDeclaredClass()).stream().map(FieldWrapper::new)
              .collect(toList()), contributors, extensionParameter);
        }
      }
    }

    return declarerList;
  }

  private void checkConfigurationIsNotAnOperation(Class<?> configurationType) {
    Class<?>[] operationClasses = getOperationClasses(extensionType);
    for (Class<?> operationClass : operationClasses) {
      if (configurationType.isAssignableFrom(operationClass) || operationClass.isAssignableFrom(configurationType)) {
        throw new IllegalConfigurationModelDefinitionException(format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                      configurationType.getName(), operationClass.getName()));
      }
    }
  }

  private void checkOperationIsNotAnExtension(Class<?> operationType) {
    if (operationType.isAssignableFrom(extensionType) || extensionType.isAssignableFrom(operationType)) {
      throw new IllegalOperationModelDefinitionException(format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
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

  private void addLayoutModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter,
                                      ExtensionParameter parameterGroupOwner) {
    LayoutModelProperty layoutModelProperty = parseLayoutAnnotations(extensionParameter, extensionParameter.getAlias());
    if (layoutModelProperty != null) {
      parameter.withModelProperty(layoutModelProperty);
    }
  }

  private void addXmlHintsModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    Optional<XmlHints> elementStyle = extensionParameter.getAnnotation(XmlHints.class);
    if (elementStyle.isPresent()) {
      parameter.withModelProperty(new XmlHintsModelProperty(elementStyle.get()));
    }
  }

  private void checkAnnotationsNotUsedMoreThanOnce(List<ExtensionParameter> parameters,
                                                   Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      final long count = parameters.stream().filter(param -> param.isAnnotatedWith(annotation)).count();
      if (count > 1) {
        throw new IllegalModelDefinitionException(format("The defined parameters %s from %s, uses the annotation @%s more than once",
                                                         parameters.stream().map(Named::getName).collect(toList()),
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
        : new ImplementingParameterModelProperty(((ParameterElement) extensionParameter).getParameter()));
  }

  private void addInterceptingCallbackModelProperty(MethodElement operationMethod, OperationDeclarer operation) {
    if (InterceptingCallback.class.isAssignableFrom(operationMethod.getReturnType())) {
      operation.withModelProperty(new InterceptingModelProperty());
    }
  }

  private interface ParameterDeclarerContributor {

    void contribute(ExtensionParameter parameter, ParameterDeclarer declarer);
  }

  private static class InfrastructureFieldContributor implements ParameterDeclarerContributor {

    @Override
    public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer) {
      if (InfrastructureTypeMapping.getMap().containsKey(parameter.getType().getDeclaredClass())) {
        declarer.withModelProperty(new InfrastructureParameterModelProperty());
        declarer.withExpressionSupport(ExpressionSupport.NOT_SUPPORTED);
      }
    }
  }
}
