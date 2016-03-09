/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.api.introspection.EnrichableModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * A metadata class that groups a set of parameters together.
 * It caches reflection objects necessary for handling those parameters
 * so that introspection is not executed every time, resulting in a performance gain.
 * <p/>
 * Because groups can be nested, this class also implements {@link EnrichableModel},
 * allowing for this group to have a {@link ParameterGroupModelProperty} which
 * describes the nested group.
 * <p/>
 * To decouple this class from the representation model (which depending on the
 * context could be a {@link Declaration} or an actual {@link ParameterModel}, this class
 * references parameters by name
 *
 * @since 3.7.0
 */
public class ParameterGroup implements EnrichableModel
{

    /**
     * The type of the pojo which implements the group
     */
    private final Class<?> type;

    /**
     * The {@link Field} in which the generated value of
     * {@link #type} is to be assigned
     */
    private final Field field;

    /**
     * A {@link Map} in which the keys are parameter names
     * and the values are their corresponding setter methods
     */
    private final Map<String, Field> parameters = new HashMap<>();

    /**
     * The model properties per the {@link EnrichableModel} interface
     */
    private Map<String, Object> modelProperties = new HashMap<>();


    public ParameterGroup(Class<?> type, Field field)
    {
        checkArgument(type != null, "type cannot be null");
        checkArgument(field != null, "field cannot be null");

        this.type = type;
        this.field = field;
        field.setAccessible(true);
    }

    /**
     * Adds a parameter to the group
     *
     * @param name  the name of the parameter
     * @param field the parameter's {@link Field}
     * @return {@code this}
     */
    public ParameterGroup addParameter(String name, Field field)
    {
        parameters.put(name, field);
        return this;
    }

    public Class<?> getType()
    {
        return type;
    }

    public Field getField()
    {
        return field;
    }

    public Map<String, Field> getParameters()
    {
        return ImmutableMap.copyOf(parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getModelProperty(String key)
    {
        return (T) modelProperties.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getModelProperties()
    {
        return ImmutableMap.copyOf(modelProperties);
    }

    public void addModelProperty(String key, Object value)
    {
        modelProperties.put(key, value);
    }
}
