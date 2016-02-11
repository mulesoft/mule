/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.mule.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.WithParameters;
import org.mule.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Field;

/**
 * Default implementation of {@link FieldDescriber}, capable
 * of handling all {@link Field}s in a generic way.
 *
 * Although it can handle pretty much any field, it's not suitable
 * for those which need special treatment.
 *
 * @since 4.0
 */
final class DefaultFieldDescriber implements FieldDescriber
{

    /**
     * Always returns {@code true}
     * @return {@code true}
     */
    @Override
    public boolean accepts(Field field)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterDescriptor describe(Field field, WithParameters with)
    {
        Optional optional = field.getAnnotation(Optional.class);

        String parameterName = MuleExtensionAnnotationParser.getAliasName(field);
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
        parameterDescriptor.withExpressionSupport(getExpressionSupport(field));
        parameterDescriptor.withModelProperty(DeclaringMemberModelProperty.KEY, new DeclaringMemberModelProperty(field));

        return parameterDescriptor;
    }
}
