/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getExtension;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getMemberName;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getParameterName;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getInterfaceGenerics;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getOperationMethods;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterGroupFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getSuperClassGenerics;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.connection.ConnectionProvider;
import org.mule.extension.annotation.api.Alias;
import org.mule.extension.annotation.api.Configuration;
import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extensible;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.ExtensionOf;
import org.mule.extension.annotation.api.OnException;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.Sources;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.extension.annotation.api.param.display.Password;
import org.mule.extension.annotation.api.param.display.Text;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.extension.api.introspection.declaration.fluent.OperationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.SourceDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.WithParameters;
import org.mule.extension.api.introspection.declaration.spi.Describer;
import org.mule.extension.api.introspection.property.PasswordModelProperty;
import org.mule.extension.api.introspection.property.TextModelProperty;
import org.mule.extension.api.runtime.source.Source;
import org.mule.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.module.extension.internal.model.property.ConfigTypeModelProperty;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.module.extension.internal.runtime.exception.DefaultExceptionEnricherFactory;
import org.mule.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.module.extension.internal.runtime.source.DefaultSourceFactory;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.CollectionUtils;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link Describer} which generates a {@link Descriptor} by
 * scanning annotations on a type provided in the constructor
 *
 * @since 3.7.0
 */
public final class AnnotationsBasedDescriber implements Describer
{

    public static final String DEFAULT_CONNECTION_PROVIDER_NAME = "connection";
    public static final String CUSTOM_CONNECTION_PROVIDER_SUFFIX = "-" + DEFAULT_CONNECTION_PROVIDER_NAME;

    private final Class<?> extensionType;
    private final VersionResolver versionResolver;

    public AnnotationsBasedDescriber(Class<?> extensionType)
    {
        this(extensionType, new ManifestBasedVersionResolver(extensionType));
    }

    public AnnotationsBasedDescriber(Class<?> extensionType, VersionResolver versionResolver)
    {
        checkArgument(extensionType != null, String.format("describer %s does not specify an extension type", getClass().getName()));
        this.extensionType = extensionType;
        this.versionResolver = versionResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Descriptor describe(DescribingContext context)
    {
        Extension extension = getExtension(extensionType);
        DeclarationDescriptor declaration = context.getDeclarationDescriptor()
                .named(extension.name())
                .onVersion(getVersion(extension))
                .fromVendor(extension.vendor())
                .describedAs(extension.description())
                .withExceptionEnricherFactory(getExceptionEnricherFactory(extensionType))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(extensionType));

        declareConfigurations(declaration, extensionType);
        declareOperations(declaration, extensionType);
        declareConnectionProviders(declaration, extensionType);
        declareMessageSources(declaration, extensionType);

        return declaration;
    }

    private String getVersion(Extension extension)
    {
        return versionResolver.resolveVersion(extension);
    }

    private void declareConfigurations(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Configurations configurations = extensionType.getAnnotation(Configurations.class);
        if (configurations != null)
        {
            for (Class<?> declaringClass : configurations.value())
            {
                declareConfiguration(declaration, declaringClass);
            }
        }
        else
        {
            declareConfiguration(declaration, extensionType);
        }
    }

    private void declareMessageSources(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Sources sources = extensionType.getAnnotation(Sources.class);
        if (sources != null)
        {
            for (Class<? extends Source> declaringClass : sources.value())
            {
                declareMessageSource(declaration, declaringClass);
            }
        }
    }

    private void declareConfiguration(DeclarationDescriptor declaration, Class<?> configurationType)
    {
        //TODO: MULE-9220
        checkArgument(CollectionUtils.isEmpty(getOperationMethods(configurationType)), String.format("Class %s can't declare a configuration and operations at the same time", configurationType.getName()));

        ConfigurationDescriptor configuration;

        Configuration configurationAnnotation = configurationType.getAnnotation(Configuration.class);
        if (configurationAnnotation != null)
        {
            configuration = declaration.withConfig(configurationAnnotation.name()).describedAs(configurationAnnotation.description());
        }
        else
        {
            configuration = declaration.withConfig(Extension.DEFAULT_CONFIG_NAME).describedAs(Extension.DEFAULT_CONFIG_DESCRIPTION);
        }

        configuration.createdWith(new TypeAwareConfigurationFactory(configurationType))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(configurationType));

        declareAnnotatedParameters(configurationType, configuration, configuration.with());
    }

    private void declareMessageSource(DeclarationDescriptor declaration, Class<? extends Source> sourceType)
    {
        //TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
        SourceDescriptor source = declaration.withMessageSource(getSourceName(sourceType));

        List<Class<?>> sourceGenerics = getSuperClassGenerics(sourceType, Source.class);

        if (sourceGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalModelDefinitionException(String.format("Message source class '%s' was expected to have 2 generic types " +
                                                                    "(one for the Payload type and another for the Attributes type) but %d were found",
                                                                    sourceType.getName(), sourceGenerics.size()));
        }

        source.sourceCreatedBy(new DefaultSourceFactory(sourceType))
                .whichReturns(DataType.of(sourceGenerics.get(0)))
                .withAttributesOfType(DataType.of(sourceGenerics.get(1)))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(sourceType));

        declareAnnotatedParameters(sourceType, source, source.with());
    }

    private void declareAnnotatedParameters(Class<?> annotatedType, Descriptor descriptor, WithParameters with)
    {
        declareSingleParameters(getParameterFields(annotatedType), with);
        List<ParameterGroup> groups = declareConfigurationParametersGroups(annotatedType, with);
        if (!CollectionUtils.isEmpty(groups) && descriptor instanceof HasModelProperties)
        {
            ((HasModelProperties) descriptor).withModelProperty(ParameterGroupModelProperty.KEY, new ParameterGroupModelProperty(groups));
        }
    }

    private java.util.Optional<ExceptionEnricherFactory> getExceptionEnricherFactory(AnnotatedElement element)
    {
        OnException onExceptionAnnotation = element.getAnnotation(OnException.class);
        if (onExceptionAnnotation != null)
        {
            return java.util.Optional.of(new DefaultExceptionEnricherFactory(onExceptionAnnotation.value()));
        }
        return java.util.Optional.empty();
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> annotatedType, WithParameters with)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterGroupFields(annotatedType))
        {
            //TODO: MULE-9220
            if (field.isAnnotationPresent(Optional.class))
            {
                throw new IllegalParameterModelDefinitionException(String.format("@%s can not be applied along with @%s. Affected field [%s] in [%s].", Optional.class.getSimpleName(), org.mule.extension.annotation.api.ParameterGroup.class.getSimpleName(), field.getName(), annotatedType));
            }
            Set<ParameterDescriptor> parameters = declareSingleParameters(getExposedFields(field.getType()), with);

            if (!parameters.isEmpty())
            {
                ParameterGroup group = new ParameterGroup(field.getType(), field);
                groups.add(group);

                for (ParameterDescriptor descriptor : parameters)
                {
                    ParameterDeclaration parameter = descriptor.getDeclaration();
                    group.addParameter(parameter.getName(), getField(field.getType(),
                                                                     getMemberName(parameter, parameter.getName()),
                                                                     parameter.getType().getRawType()));
                }

                List<ParameterGroup> childGroups = declareConfigurationParametersGroups(field.getType(), with);
                if (!CollectionUtils.isEmpty(childGroups))
                {
                    group.addModelProperty(ParameterGroupModelProperty.KEY, new ParameterGroupModelProperty(childGroups));
                }
            }
        }

        return groups;
    }

    private Set<ParameterDescriptor> declareSingleParameters(Collection<Field> parameterFields, WithParameters with)
    {
        ImmutableSet.Builder<ParameterDescriptor> parameters = ImmutableSet.builder();

        for (Field field : parameterFields)
        {
            Parameter parameter = field.getAnnotation(Parameter.class);
            Optional optional = field.getAnnotation(Optional.class);

            String parameterName = getParameterName(field, parameter);
            ParameterDescriptor parameterDescriptor;
            DataType dataType = IntrospectionUtils.getFieldDataType(field);
            if (optional == null)
            {
                parameterDescriptor = with.requiredParameter(parameterName);
            }
            else
            {
                parameterDescriptor = with.optionalParameter(parameterName).defaultingTo(getDefaultValue(optional));
            }

            parameterDescriptor.ofType(dataType);
            parameterDescriptor.withExpressionSupport(IntrospectionUtils.getExpressionSupport(field));
            parameterDescriptor.withModelProperty(DeclaringMemberModelProperty.KEY, new DeclaringMemberModelProperty(field));

            parameters.add(parameterDescriptor);
        }

        return parameters.build();
    }

    private void declareOperations(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Operations operations = extensionType.getAnnotation(Operations.class);
        if (operations != null)
        {
            for (Class<?> actingClass : operations.value())
            {
                declareOperation(declaration, actingClass);
            }
        }
    }

    private <T> void declareOperation(DeclarationDescriptor declaration, Class<T> actingClass)
    {
        for (Method method : getOperationMethods(actingClass))
        {
            OperationDescriptor operation = declaration.withOperation(method.getName())
                    .withModelProperty(ImplementingMethodModelProperty.KEY, new ImplementingMethodModelProperty(method))
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, method))
                    .whichReturns(IntrospectionUtils.getMethodReturnType(method))
                    .withExceptionEnricherFactory(getExceptionEnricherFactory(method));

            declareOperationParameters(method, operation);
            calculateExtendedTypes(actingClass, method, operation);
        }
    }

    private void declareConnectionProviders(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Providers providers = extensionType.getAnnotation(Providers.class);
        if (providers != null)
        {
            for (Class<?> providerClass : providers.value())
            {
                declareConnectionProvider(declaration, providerClass);
            }
        }
    }

    private <T> void declareConnectionProvider(DeclarationDescriptor declaration, Class<T> providerClass)
    {
        String name = DEFAULT_CONNECTION_PROVIDER_NAME;
        String description = EMPTY;

        Alias aliasAnnotation = providerClass.getAnnotation(Alias.class);
        if (aliasAnnotation != null)
        {
            name = aliasAnnotation.value() + CUSTOM_CONNECTION_PROVIDER_SUFFIX;
            description = aliasAnnotation.description();
        }

        List<Class<?>> providerGenerics = getInterfaceGenerics(providerClass, ConnectionProvider.class);

        if (providerGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalConnectionProviderModelDefinitionException(String.format("Connection provider class '%s' was expected to have 2 generic types " +
                                                                                      "(one for the config type and another for the connection type) but %d were found",
                                                                                      providerClass.getName(), providerGenerics.size()));
        }

        ConnectionProviderDescriptor providerDescriptor = declaration.withConnectionProvider(name)
                .describedAs(description)
                .createdWith(new DefaultConnectionProviderFactory<>(providerClass))
                .forConfigsOfType(providerGenerics.get(0))
                .whichGivesConnectionsOfType(providerGenerics.get(1))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(providerClass));

        declareAnnotatedParameters(providerClass, providerDescriptor, providerDescriptor.with());
    }

    private void calculateExtendedTypes(Class<?> actingClass, Method method, OperationDescriptor operation)
    {
        ExtensionOf extensionOf = method.getAnnotation(ExtensionOf.class);
        if (extensionOf == null)
        {
            extensionOf = actingClass.getAnnotation(ExtensionOf.class);
        }

        if (extensionOf != null)
        {
            operation.withModelProperty(ExtendingOperationModelProperty.KEY, new ExtendingOperationModelProperty(extensionOf.value()));
        }
        else if (isExtensible())
        {
            operation.withModelProperty(ExtendingOperationModelProperty.KEY, new ExtendingOperationModelProperty(extensionType));
        }
    }

    private boolean isExtensible()
    {
        return extensionType.getAnnotation(Extensible.class) != null;
    }

    private void declareOperationParameters(Method method, OperationDescriptor operation)
    {
        List<ParsedParameter> descriptors = MuleExtensionAnnotationParser.parseParameters(method);

        //TODO: MULE-9220
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, UseConfig.class);
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, Connection.class);

        for (ParsedParameter parsedParameter : descriptors)
        {
            if (parsedParameter.isAdvertised())
            {
                ParameterDescriptor parameter = parsedParameter.isRequired()
                                                ? operation.with().requiredParameter(parsedParameter.getName())
                                                : operation.with().optionalParameter(parsedParameter.getName()).defaultingTo(parsedParameter.getDefaultValue());

                parameter.withExpressionSupport(IntrospectionUtils.getExpressionSupport(parsedParameter));
                parameter.describedAs(EMPTY).ofType(parsedParameter.getType());
                addTypeRestrictions(parameter, parsedParameter);
                addModelPropertiesToParameter(parsedParameter, parameter);
            }

            Connection connectionAnnotation = parsedParameter.getAnnotation(Connection.class);
            if (connectionAnnotation != null)
            {
                operation.withModelProperty(ConnectionTypeModelProperty.KEY, new ConnectionTypeModelProperty(parsedParameter.getType().getRawType()));
            }

            UseConfig useConfig = parsedParameter.getAnnotation(UseConfig.class);
            if (useConfig != null)
            {
                operation.withModelProperty(ConfigTypeModelProperty.KEY, new ConfigTypeModelProperty(parsedParameter.getType().getRawType()));
            }
        }
    }

    private void addModelPropertiesToParameter(ParsedParameter parsedParameter, ParameterDescriptor parameter)
    {
        Password passwordAnnotation = parsedParameter.getAnnotation(Password.class);
        if (passwordAnnotation != null)
        {
            parameter.withModelProperty(PasswordModelProperty.KEY, new ImmutablePasswordModelProperty());
        }
        Text textAnnotation = parsedParameter.getAnnotation(Text.class);
        if (textAnnotation != null)
        {
            parameter.withModelProperty(TextModelProperty.KEY, new ImmutableTextModelProperty());
        }
    }

    private void checkAnnotationIsNotUsedMoreThanOnce(Method method, OperationDescriptor operation, Class annotationClass)
    {
        Stream<java.lang.reflect.Parameter> parametersStream = Arrays
                .stream(method.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(annotationClass));

        List<java.lang.reflect.Parameter> parameterList = parametersStream.collect(Collectors.toList());

        if (parameterList.size() > 1)
        {
            throw new IllegalModelDefinitionException(String.format("Method [%s] defined in Class [%s] of extension [%s] uses the annotation @%s more than once", method.getName(), method.getDeclaringClass(), operation.getRootDeclaration().getDeclaration().getName(), annotationClass.getSimpleName()));
        }
    }

    private void addTypeRestrictions(ParameterDescriptor parameter, ParsedParameter descriptor)
    {
        Class<?> restriction = descriptor.getTypeRestriction();
        if (restriction != null)
        {
            parameter.withModelProperty(TypeRestrictionModelProperty.KEY, new TypeRestrictionModelProperty<>(restriction));
        }
    }
}
