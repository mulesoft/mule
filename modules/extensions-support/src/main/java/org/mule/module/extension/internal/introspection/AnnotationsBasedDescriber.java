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
import static org.mule.module.extension.internal.util.IntrospectionUtils.getOperationMethods;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterGroupFields;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.annotations.Configuration;
import org.mule.extension.annotations.Configurations;
import org.mule.extension.annotations.Extensible;
import org.mule.extension.annotations.Extension;
import org.mule.extension.annotations.ExtensionOf;
import org.mule.extension.annotations.Operations;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.introspection.declaration.fluent.Descriptor;
import org.mule.extension.introspection.declaration.fluent.OperationDescriptor;
import org.mule.extension.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.introspection.declaration.fluent.WithParameters;
import org.mule.extension.introspection.declaration.spi.Describer;
import org.mule.module.extension.internal.model.property.ExtendingOperationModelProperty;
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

        configuration.instantiatedWith(new TypeAwareConfigurationInstantiator(configurationType))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(configurationType));

        declareConfigurationParameters(configurationType, configuration);
    }

    private void declareConfigurationParameters(Class<?> extensionType, ConfigurationDescriptor configuration)
    {
        declareSingleParameters(extensionType, configuration.with());
        List<ParameterGroup> groups = declareConfigurationParametersGroups(extensionType, configuration);
        if (!CollectionUtils.isEmpty(groups))
        {
            configuration.withModelProperty(ParameterGroupModelProperty.KEY, new ParameterGroupModelProperty(groups));
        }
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> extensionType, ConfigurationDescriptor configuration)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterGroupFields(extensionType))
        {
            Set<ParameterDescriptor> parameters = declareSingleParameters(field.getType(), configuration.with());

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

                List<ParameterGroup> childGroups = declareConfigurationParametersGroups(field.getType(), configuration);
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

            if (!parameter.isDynamic())
            {
                parameterDescriptor.whichIsStatic();
            }

            parameterDescriptor.withModelProperty(MemberNameModelProperty.KEY, new MemberNameModelProperty(field.getName()));

            parameters.add(parameterDescriptor);
        }

        return parameters.build();
    }


    private void declareOperations(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Operations operations = extensionType.getAnnotation(Operations.class);
        if (operations == null)
        {
            throw new IllegalArgumentException(String.format("Extension of type %s does not declare operations", extensionType.getName()));
        }

        for (Class<?> actingClass : operations.value())
        {
            declareOperation(declaration, actingClass);
        }
    }

    private <T> void declareOperation(DeclarationDescriptor declaration, Class<T> actingClass)
    {
        for (Method method : getOperationMethods(actingClass))
        {
            OperationDescriptor operation = declaration.withOperation(method.getName())
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, method));

            declareOperationParameters(method, operation);
            calculateExtendedTypes(actingClass, method, operation);
        }
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
        List<org.mule.module.extension.internal.introspection.ParameterDescriptor> descriptors = MuleExtensionAnnotationParser.parseParameters(method);

        for (org.mule.module.extension.internal.introspection.ParameterDescriptor parameterDescriptor : descriptors)
        {
            ParameterDescriptor parameter = parameterDescriptor.isRequired()
                                            ? operation.with().requiredParameter(parameterDescriptor.getName())
                                            : operation.with().optionalParameter(parameterDescriptor.getName()).defaultingTo(parameterDescriptor.getDefaultValue());

            parameter.describedAs(EMPTY).ofType(parameterDescriptor.getType());
            addTypeRestrictions(parameter, parameterDescriptor);
        }

    }

    private void addTypeRestrictions(ParameterDescriptor parameter, org.mule.module.extension.internal.introspection.ParameterDescriptor descriptor)
    {
        Class<?> restriction = descriptor.getTypeRestriction();
        if (restriction != null)
        {
            parameter.withModelProperty(TypeRestrictionModelProperty.KEY, new TypeRestrictionModelProperty<>(restriction));
        }
    }
}
