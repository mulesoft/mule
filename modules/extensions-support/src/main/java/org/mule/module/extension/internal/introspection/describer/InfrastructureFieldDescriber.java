/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import org.mule.api.config.ThreadingProfile;
import org.mule.api.tls.TlsContextFactory;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.WithParameters;
import org.mule.module.extension.internal.model.property.DeclaringMemberModelProperty;

import java.lang.reflect.Field;

/**
 * Implementation of {@link FieldDescriber}, capable
 * of handling fields of infrastructure classes like
 * {@link ThreadingProfile} or {@link TlsContextFactory}
 *
 * @since 4.0
 */
public class InfrastructureFieldDescriber implements FieldDescriber
{

    private String attributeName;
    private Class<?> clazz;

    /**
     * @param clazz   Class of the desired parameter
     * @param attributeName Name of the attribute for the generated xsd schema
     */
    public InfrastructureFieldDescriber(Class<?> clazz, String attributeName)
    {
        this.clazz = clazz;
        this.attributeName = attributeName;
    }

    /**
     * Only accepts fields which type is the same as the clazz
     *
     * @param field a {@link Field} from a class annotated with the SDK annotations
     * @return {@code true} if the {@code field}'s class is the same as the desired class
     */
    @Override
    public boolean accepts(Field field)
    {
        return clazz.isAssignableFrom(field.getType());
    }

    /**
     * Generates a {@link ParameterDescriptor} which represents the desired class
     * in a canonical way. It will not accept expressions and it will have the provided name
     * regardless of how the field was defined
     *
     * @param field the {@link Field} being processed
     * @param with  a {@link WithParameters} object used to create the descriptor
     * @return a {@link ParameterDescriptor} which represents the desired class
     */
    @Override
    public ParameterDescriptor describe(Field field, WithParameters with)
    {
        ParameterDescriptor descriptor = field.getAnnotation(Optional.class) != null
                                         ? with.optionalParameter(attributeName)
                                         : with.requiredParameter(attributeName);

        return descriptor.ofType(clazz)
                .withExpressionSupport(ExpressionSupport.NOT_SUPPORTED)
                .withModelProperty(new DeclaringMemberModelProperty(field));
    }
}
