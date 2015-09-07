/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

import org.mule.api.NamedObject;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.introspection.ConfigurationModel;

/**
 * A custom model property to link a {@link NamedObject} to the actual member in which it was defined.
 * <p/>
 * The most common use case for this is to support the {@link Parameter#alias()} attribute.
 * For example, consider a {@link ConfigurationModel} parameter which is obtained through
 * inspecting fields in a class. This property allows for the introspection model to list
 * the parameter by a given alias, while this parameter still provides the real name of the field
 * which is going to be needed for further operations
 *
 * @since 4.0
 */
public final class MemberNameModelProperty
{

    /**
     * A unique key that identifies this property type
     */
    public static final String KEY = MemberNameModelProperty.class.getName();

    private final String name;

    public MemberNameModelProperty(String name)
    {
        this.name = name;
    }

    /**
     * The name of the member in which an aliased
     * object is defined
     *
     * @return a {@link String}
     */
    public String getName()
    {
        return name;
    }
}
