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
import static org.mule.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getInterfaceGenerics;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getOperationMethods;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterGroupFields;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.annotation.api.Configuration;
import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extensible;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.ExtensionOf;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.connector.Provider;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.api.connection.ConnectionProvider;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.extension.api.introspection.declaration.fluent.OperationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.WithParameters;
import org.mule.extension.api.introspection.declaration.spi.Describer;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.model.property.MemberNameModelProperty;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.CollectionUtils;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
                .describedAs(extension.description())
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(extensionType));


        declareConfigurations(declaration, extensionType);
        declareOperations(declaration, extensionType);
        declareConnectionProviders(declaration, extensionType);

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

    private void declareConfiguration(DeclarationDescriptor declaration, Class<?> configurationType)
    {
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

    private void declareAnnotatedParameters(Class<?> annotatedType, Descriptor descriptor, WithParameters with)
    {
        declareSingleParameters(annotatedType, with);
        List<ParameterGroup> groups = declareConfigurationParametersGroups(annotatedType, with);
        if (!CollectionUtils.isEmpty(groups) && descriptor instanceof HasModelProperties)
        {
            ((HasModelProperties) descriptor).withModelProperty(ParameterGroupModelProperty.KEY, new ParameterGroupModelProperty(groups));
        }
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> annotatedType, WithParameters with)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterGroupFields(annotatedType))
        {
            Set<ParameterDescriptor> parameters = declareSingleParameters(field.getType(), with);

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

    private Set<ParameterDescriptor> declareSingleParameters(Class<?> extensionType, WithParameters with)
    {
        ImmutableSet.Builder<ParameterDescriptor> parameters = ImmutableSet.builder();

        for (Field field : getParameterFields(extensionType))
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
            parameterDescriptor.withExpressionSupport(parameter.expressionSupport());
            parameterDescriptor.withModelProperty(MemberNameModelProperty.KEY, new MemberNameModelProperty(field.getName()));

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
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, method));

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

        Provider providerAnnotation = providerClass.getAnnotation(Provider.class);
        if (providerAnnotation != null)
        {
            name = providerAnnotation.name() + CUSTOM_CONNECTION_PROVIDER_SUFFIX;
            description = providerAnnotation.description();
        }

        List<Class<?>> providerGenerics = getInterfaceGenerics(providerClass, ConnectionProvider.class);

        if (providerGenerics.size() != 2)
        {
            throw new IllegalModelDefinitionException(String.format("Connection provider class '%s' was expected to have 2 generic types " +
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

        for (ParsedParameter parsedParameter : descriptors)
        {
            if (parsedParameter.isAdvertised())
            {
                ParameterDescriptor parameter = parsedParameter.isRequired()
                                                ? operation.with().requiredParameter(parsedParameter.getName())
                                                : operation.with().optionalParameter(parsedParameter.getName()).defaultingTo(parsedParameter.getDefaultValue());

                parameter.describedAs(EMPTY).ofType(parsedParameter.getType());
                addTypeRestrictions(parameter, parsedParameter);
            }

            Connection connectionAnnotation = parsedParameter.getAnnotation(Connection.class);
            if (connectionAnnotation != null)
            {
                operation.withModelProperty(ConnectionTypeModelProperty.KEY, new ConnectionTypeModelProperty(parsedParameter.getType().getRawType()));
            }
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
