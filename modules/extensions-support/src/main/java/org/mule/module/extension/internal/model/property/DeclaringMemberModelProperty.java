/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ParameterModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * A custom model property to link a {@link ParameterModel} to the actual member it represents.
 * <p/>
 * The most common use case for this is to support the {@link Parameter#alias()} attribute.
 * For example, consider a {@link ConfigurationModel} parameter which is obtained through
 * inspecting fields in a class. This property allows for the introspection model to list
 * the parameter by a given alias, while this parameter still provides the real name of the field
 * which is going to be needed for further operations
 * <p/>
 * Another common use case is to get the field {@link Annotation}s in order to enrich the model with other properties.
 *
 * @since 4.0
 */
public final class DeclaringMemberModelProperty
{

    /**
     * A unique key that identifies this property type
     */
    public static final String KEY = DeclaringMemberModelProperty.class.getName();

    private final Field declaringField;

    public DeclaringMemberModelProperty(Field declaringField)
    {
        this.declaringField = declaringField;
    }

    /**
     * The field associated to the {@link ParameterModel}
     *
     * @return a {@link Field}
     */
    public Field getDeclaringField()
    {
        return declaringField;
    }
}
