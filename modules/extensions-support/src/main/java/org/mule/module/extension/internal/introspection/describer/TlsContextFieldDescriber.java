/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.mule.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import org.mule.api.tls.TlsContextFactory;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.WithParameters;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.model.property.DeclaringMemberModelProperty;

import java.lang.reflect.Field;

/**
 * Implementation of {@link FieldDescriber} used to parse instances
 * of {@link TlsContextFactory}.
 *
 * @since 4.0
 */
final class TlsContextFieldDescriber implements FieldDescriber
{

    /**
     * Only accepts fields which type is {@link TlsContextFactory}
     *
     * @param field a {@link Field} from a class annotated with the SDK annotations
     * @return {@code true} if the {@code field}'s type is {@link TlsContextFactory}
     */
    @Override
    public boolean accepts(Field field)
    {
        return TlsContextFactory.class.isAssignableFrom(field.getType());
    }

    /**
     * Generates a {@link ParameterDescriptor} which reperesents a {@link TlsContextFactory}
     * in a canonical way. It will not accept expressions and the name will be {@link ExtensionProperties#TLS_ATTRIBUTE_NAME}
     * regardless of how the field was defined
     *
     * @param field the {@link Field} being processed
     * @param with  a {@link WithParameters} object used to create the descriptor
     * @return a {@link ParameterDescriptor} which represents a {@link TlsContextFactory}
     */
    @Override
    public ParameterDescriptor describe(Field field, WithParameters with)
    {
        ParameterDescriptor descriptor = field.getAnnotation(Optional.class) != null
                                         ? with.optionalParameter(TLS_ATTRIBUTE_NAME)
                                         : with.requiredParameter(TLS_ATTRIBUTE_NAME);

        return descriptor.ofType(TlsContextFactory.class)
                .withExpressionSupport(ExpressionSupport.NOT_SUPPORTED)
                .withModelProperty(DeclaringMemberModelProperty.KEY, new DeclaringMemberModelProperty(field));
    }
}
