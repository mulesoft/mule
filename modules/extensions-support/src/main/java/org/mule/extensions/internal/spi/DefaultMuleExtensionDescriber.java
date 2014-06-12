/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal.spi;

import org.mule.extensions.api.annotation.Configurable;
import org.mule.extensions.api.annotation.Operation;
import org.mule.extensions.api.annotation.param.Optional;
import org.mule.extensions.introspection.api.ExtensionConfiguration;
import org.mule.extensions.introspection.spi.MuleExtensionBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionConfigurationBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionDescriber;
import org.mule.extensions.introspection.spi.MuleExtensionOperationBuilder;
import org.mule.util.Preconditions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.StringUtils;

final class DefaultMuleExtensionDescriber implements MuleExtensionDescriber
{

    private final Class<?> extensionType;

    public DefaultMuleExtensionDescriber(Class<?> extensionType)
    {
        Preconditions.checkArgument(extensionType != null, "extensionType cannot be null");
        this.extensionType = extensionType;
    }

    @Override
    public void describe(MuleExtensionBuilder builder)
    {
        ExtensionDescriptor descriptor = MuleExtensionAnnotationParser.parseExtensionDescriptor(extensionType);
        describeExtension(builder, descriptor);
        describeConfigurations(builder, descriptor);
        describeOperations(builder, descriptor);
    }

    private void describeExtension(MuleExtensionBuilder builder, ExtensionDescriptor descriptor)
    {
        builder.setName(descriptor.getName())
                .setDescription(descriptor.getDescription())
                .setVersion(descriptor.getVersion())
                .setMinMuleVersion(descriptor.getMinMuleVersion())
                .setExtensionType(descriptor.getExtensionType());
    }


    private void describeConfigurations(MuleExtensionBuilder builder, ExtensionDescriptor descriptor)
    {
        // TODO: for now we add only one configuration, when we do OAuth or when we resolve the question around config representations this has to change
        MuleExtensionConfigurationBuilder configuration = builder.newConfiguration()
                .setName(descriptor.getConfigElementName())
                .setDescription(ExtensionConfiguration.DEFAULT_DESCRIPTION);

        builder.addConfiguration(configuration);

        for (Field field : descriptor.getConfigurableFields())
        {
            Configurable configurable = field.getAnnotation(Configurable.class);
            Optional optional = field.getAnnotation(Optional.class);

            configuration.addParameter(builder.newParameter()
                                               .setName(field.getName())
                                               .setDescription(configurable.description())
                                               .setType(field.getType())
                                               .setDynamic(configurable.isDynamic())
                                               .setRequired(optional == null)
                                               .setDefaultValue(optional != null ? optional.defaultValue() : null));
        }
    }

    private void describeOperations(MuleExtensionBuilder builder, ExtensionDescriptor extension)
    {
        for (Method method : extension.getOperationMethods())
        {
            Operation annotation = method.getAnnotation(Operation.class);
            MuleExtensionOperationBuilder operation = builder.newOperation();
            builder.addOperation(operation);

            operation.setName(resolveOperationName(method, annotation))
                    .setDescription(annotation.description())
                    .setInputTypes(resolveOperationInputTypes(annotation))
                    .setOutputTypes(method.getReturnType());

            parseOperationParameters(method, builder, operation, extension);
        }
    }

    private void parseOperationParameters(Method method,
                                          MuleExtensionBuilder builder,
                                          MuleExtensionOperationBuilder operation,
                                          ExtensionDescriptor extension)
    {
        List<ParameterDescriptor> descriptors = MuleExtensionAnnotationParser.parseParameter(method, extension);

        for (ParameterDescriptor parameterDescriptor : descriptors)
        {
            operation.addParameter(builder.newParameter()
                                           .setType(parameterDescriptor.getType())
                                           .setDynamic(true) //TODO: Add logic to determine this rather than hardcoding true
                                           .setName(parameterDescriptor.getName())
                                           .setDescription(StringUtils.EMPTY)
                                           .setRequired(parameterDescriptor.isRequired())
                                           .setDefaultValue(parameterDescriptor.getDefaultValue())
            );
        }
    }

    private String resolveOperationName(Method method, Operation operation)
    {
        return StringUtils.isBlank(operation.name()) ? method.getName() : operation.name();
    }

    private Class<?>[] resolveOperationInputTypes(Operation operation)
    {
        Class<?>[] input = operation.acceptedPayloadTypes();
        return input != null ? input : new Class<?>[] {};
    }
}
