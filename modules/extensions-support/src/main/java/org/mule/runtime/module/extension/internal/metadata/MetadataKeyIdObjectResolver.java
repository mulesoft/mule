/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.core.util.collection.ImmutableMapCollector;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Provides an instance of the annotated {@link MetadataKeyId} parameter's complex type.
 * The instance will be populated with all the corresponding values of the passed {@link MetadataKey}.
 *
 * @since 4.0
 */
final class MetadataKeyIdObjectResolver
{

    private final Class type;
    private final Map<Field, String> fieldValueMap = new HashMap<>();

    /**
     * Creates a new instance.
     *
     * @param component the component model that contains the parameter annotated with {@link MetadataKeyId}.
     * @param key       the {@link MetadataKey} associated to the POJO that its going to be instantiated.
     */
    MetadataKeyIdObjectResolver(ComponentModel component, MetadataKey key) throws MetadataResolvingException
    {
        type = component.getModelProperty(MetadataKeyIdModelProperty.class)
                .orElseThrow(() -> buildException(format("Component '%s' doesn't have a MetadataKeyId parameter assosiated", component.getName())))
                .getType();
        checkInstantiable(type);
        fillFieldValueMap(key);
    }

    private void fillFieldValueMap(MetadataKey key) throws MetadataResolvingException
    {
        Map<Integer, Field> metadataKeyPartMap = getAnnotatedFields(type, MetadataKeyPart.class)
                .stream().collect(new ImmutableMapCollector<>(f -> f.getAnnotation(MetadataKeyPart.class).order(), Function.identity()));

        int order = 1;
        while (!key.getChilds().isEmpty())
        {
            fieldValueMap.put(metadataKeyPartMap.get(order), key.getId());
            key = key.getChilds().iterator().next();
            order++;
        }
        fieldValueMap.put(metadataKeyPartMap.get(order), key.getId());
    }

    /**
     * @return a new instance of the {@link MetadataKeyId} parameter {@code type} with the values of the
     * passed {@link MetadataKey}.
     * @throws MetadataResolvingException if the instantiation fails.
     */
    public Object resolve() throws MetadataResolvingException
    {
        try
        {
            Object metadataKeyId = type.newInstance();
            fieldValueMap.keySet().forEach(field -> new FieldSetter<Object, String>(field).set(metadataKeyId, fieldValueMap.get(field)));
            return metadataKeyId;
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw buildException(format("Could not instantiate MetadataKeyId of type '%s'", type.toString()), e);
        }
    }

    private MetadataResolvingException buildException(String message)
    {
        return buildException(message, null);
    }

    private MetadataResolvingException buildException(String message, Exception cause)
    {
        return cause == null ? new MetadataResolvingException(message, INVALID_METADATA_KEY)
                             : new MetadataResolvingException(message, INVALID_METADATA_KEY, cause);
    }

}
