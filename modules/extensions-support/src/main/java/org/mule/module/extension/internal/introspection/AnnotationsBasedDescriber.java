/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getDefaultValue;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getExtension;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getParameterGroupFields;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getOperationMethods;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getSetter;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.SPIServiceRegistry;
import org.mule.extension.annotations.Configuration;
import org.mule.extension.annotations.Configurations;
import org.mule.extension.annotations.Extension;
import org.mule.extension.annotations.ImplementationOf;
import org.mule.extension.annotations.Operations;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.Describer;
import org.mule.extension.introspection.declaration.ConfigurationConstruct;
import org.mule.extension.introspection.declaration.Construct;
import org.mule.extension.introspection.declaration.DeclarationConstruct;
import org.mule.extension.introspection.declaration.OperationConstruct;
import org.mule.extension.introspection.declaration.ParameterConstruct;
import org.mule.extension.introspection.declaration.ParameterDeclaration;
import org.mule.extension.introspection.declaration.WithParameters;
import org.mule.module.extension.internal.capability.metadata.HiddenCapability;
import org.mule.module.extension.internal.capability.metadata.ImplementedTypeCapability;
import org.mule.module.extension.internal.capability.metadata.ParameterGroupCapability;
import org.mule.module.extension.internal.capability.metadata.TypeRestrictionCapability;
import org.mule.module.extension.internal.runtime.ReflectiveDelegateFactory;
import org.mule.module.extension.internal.runtime.ReflectiveOperationExecutorFactory;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.CollectionUtils;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link Describer} which generates a {@link Construct} by
 * scanning annotations on a type provided in the constructor
 *
 * @since 3.7.0
 */
public final class AnnotationsBasedDescriber implements Describer
{

    private CapabilitiesResolver capabilitiesResolver = new DefaultCapabilitiesResolver(new SPIServiceRegistry());
    private final Class<?> extensionType;
    private final ReflectiveDelegateFactory delegateFactory = new ReflectiveDelegateFactory();

    public AnnotationsBasedDescriber(Class<?> extensionType)
    {
        this.extensionType = extensionType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Construct describe()
    {
        checkArgument(extensionType != null, String.format("describer %s does not specify an extension type", getClass().getName()));

        Extension extension = getExtension(extensionType);
        DeclarationConstruct declaration = newDeclarationConstruct(extension);
        declareConfigurations(declaration, extensionType);
        declareOperations(declaration, extensionType);
        describeCapabilities(declaration, extensionType);

        return declaration;
    }

    private DeclarationConstruct newDeclarationConstruct(Extension extension)
    {
        return new DeclarationConstruct(extension.name(), extension.version()).describedAs(extension.description());
    }

    private void declareConfigurations(DeclarationConstruct declaration, Class<?> extensionType)
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

    private void declareConfiguration(DeclarationConstruct declaration, Class<?> extensionType)
    {
        checkArgument(CollectionUtils.isEmpty(getOperationMethods(extensionType)), String.format("Class %s can't declare a configuration and operations at the same time", extensionType.getName()));

        ConfigurationConstruct configuration;

        Configuration configurationAnnotation = extensionType.getAnnotation(Configuration.class);
        if (configurationAnnotation != null)
        {
            configuration = declaration.withConfig(configurationAnnotation.name()).describedAs(configurationAnnotation.description());
        }
        else
        {
            configuration = declaration.withConfig(Extension.DEFAULT_CONFIG_NAME).describedAs(Extension.DEFAULT_CONFIG_DESCRIPTION);
        }

        configuration.instantiatedWith(new TypeAwareConfigurationInstantiator(extensionType));

        declareConfigurationParameters(extensionType, configuration);
    }

    private void declareConfigurationParameters(Class<?> extensionType, ConfigurationConstruct configuration)
    {
        declareSingleParameters(extensionType, configuration.with());
        List<ParameterGroup> groups = declareConfigurationParametersGroups(extensionType, configuration);
        if (!CollectionUtils.isEmpty(groups))
        {
            configuration.withCapability(new ParameterGroupCapability(groups));
        }
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> extensionType, ConfigurationConstruct configuration)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterGroupFields(extensionType))
        {
            Set<ParameterConstruct> parameters = declareSingleParameters(field.getType(), configuration.with());

            if (!parameters.isEmpty())
            {
                ParameterGroup group = new ParameterGroup(field.getType(), getSetter(extensionType, field.getName(), field.getType()));
                groups.add(group);

                for (ParameterConstruct construct : parameters)
                {
                    ParameterDeclaration parameter = construct.getDeclaration();
                    group.addParameter(construct.getDeclaration().getName(), getSetter(field.getType(), parameter.getName(), parameter.getType().getRawType()));
                }

                List<ParameterGroup> childGroups = declareConfigurationParametersGroups(field.getType(), configuration);
                if (!CollectionUtils.isEmpty(childGroups))
                {
                    group.addCapability(new ParameterGroupCapability(childGroups));
                }
            }
        }

        return groups;
    }

    private Set<ParameterConstruct> declareSingleParameters(Class<?> extensionType, WithParameters with)
    {
        ImmutableSet.Builder<ParameterConstruct> parameters = ImmutableSet.builder();

        for (Field field : getParameterFields(extensionType))
        {
            Parameter parameter = field.getAnnotation(Parameter.class);
            Optional optional = field.getAnnotation(Optional.class);

            ParameterConstruct parameterConstruct;
            DataType dataType = IntrospectionUtils.getFieldDataType(field);
            if (optional == null)
            {
                parameterConstruct = with.requiredParameter(field.getName());
            }
            else
            {
                parameterConstruct = with.optionalParameter(field.getName()).defaultingTo(getDefaultValue(optional, dataType));
            }

            parameterConstruct.ofType(dataType);

            if (!parameter.isDynamic())
            {
                parameterConstruct.whichIsNotDynamic();
            }

            parameters.add(parameterConstruct);
        }

        return parameters.build();
    }

    private void declareOperations(DeclarationConstruct declaration, Class<?> extensionType)
    {
        Operations operations = extensionType.getAnnotation(Operations.class);
        if (operations != null)
        {
            for (Class<?> actingClass : operations.value())
            {
                declareOperation(declaration, actingClass);
            }
        }
        else
        {
            declareOperation(declaration, extensionType);
        }
    }

    private <T> void declareOperation(DeclarationConstruct declaration, Class<T> actingClass)
    {
        for (Method method : getOperationMethods(actingClass))
        {
            OperationConstruct operation = declaration.withOperation(method.getName())
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, method, delegateFactory));

            declareOperationParameters(method, operation);

            calculateImplementedTypes(actingClass, method, operation);
        }
    }

    private void calculateImplementedTypes(Class<?> actingClass, Method method, OperationConstruct operation)
    {
        ImplementationOf implementation = method.getAnnotation(ImplementationOf.class);
        if (implementation == null)
        {
            implementation = actingClass.getAnnotation(ImplementationOf.class);
        }

        if (implementation != null)
        {
            operation.withCapability(new ImplementedTypeCapability(implementation.value()));
        }
    }

    private void declareOperationParameters(Method method, OperationConstruct operation)
    {
        List<ParameterDescriptor> descriptors = MuleExtensionAnnotationParser.parseParameters(method);

        for (ParameterDescriptor parameterDescriptor : descriptors)
        {
            ParameterConstruct parameter = parameterDescriptor.isRequired()
                                           ? operation.with().requiredParameter(parameterDescriptor.getName())
                                           : operation.with().optionalParameter(parameterDescriptor.getName()).defaultingTo(parameterDescriptor.getDefaultValue());

            parameter.describedAs(EMPTY).ofType(parameterDescriptor.getType());

            hideIfNecessary(parameterDescriptor, parameter);
            addTypeRestrictions(parameter, parameterDescriptor);
        }

    }

    private void hideIfNecessary(ParameterDescriptor parameterDescriptor, ParameterConstruct parameter)
    {
        if (parameterDescriptor.isHidden())
        {
            parameter.withCapability(new HiddenCapability());
        }
    }

    private void addTypeRestrictions(ParameterConstruct parameter, ParameterDescriptor descriptor)
    {
        Class<?> restriction = descriptor.getTypeRestriction();
        if (restriction != null)
        {
            parameter.withCapability(new TypeRestrictionCapability<>(restriction));
        }
    }

    private void describeCapabilities(DeclarationConstruct declaration, Class<?> extensionType)
    {
        capabilitiesResolver.resolveCapabilities(declaration, extensionType, declaration);
    }
}
