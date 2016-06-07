/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.metadata.resolving.FailureCode.*;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataKeyParts;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides an instance of the annotated {@link MetadataKeyId} parameter type.
 * The instance will be populated with all the corresponding values of the passed {@link Map} key.
 *
 * @since 4.0
 */
final class MetadataKeyIdObjectResolver
{

    /**
     * Given a {@link ComponentModel} and a {@link Map} key, return the populated key in the Type that the {@link Component}
     * parameter requires.
     *
     * @param component the component model that contains the parameter annotated with {@link MetadataKeyId}
     * @param key       the {@link MetadataKey} associated to the {@link MetadataKeyId}
     * @return a new instance of the {@link MetadataKeyId} parameter {@code type} with the values of the passed {@link Map}
     * @throws MetadataResolvingException if:
     *                                    <ul>
     *                                    <li>Parameter types is not instantiable</li>
     *                                    <li>{@param key} does not provide the required levels</li>
     *                                    <li>{@link MetadataKeyId} is not found in the {@link ComponentModel}</li>
     *                                    </ul>
     */
    public static Object resolve(ComponentModel component, MetadataKey key) throws MetadataResolvingException
    {
        final List<ParameterModel> metadataKeyParts = getMetadataKeyParts(component);
        return isKeyLessComponent(metadataKeyParts) ? new NullMetadataKey().getId() : resolveMetadataKeyWhenPresent(key, metadataKeyParts, component);
    }

    private static Object resolveMetadataKeyWhenPresent(MetadataKey key, List<ParameterModel> metadataKeyParts, ComponentModel componentModel) throws MetadataResolvingException
    {
        return isSingleLevelKey(metadataKeyParts) ? key.getId() : resolveMultiLevelKey(componentModel, key);
    }

    /**
     * Resolves the KeyIdObject for a MultiLevel {@link MetadataKeyId}
     *
     * @param component model property of the {@link MetadataKeyId} parameter
     * @param key       key containing the values of each level
     * @return the KeyIdObject for the {@link MetadataKeyId} parameter
     * @throws MetadataResolvingException
     */
    private static Object resolveMultiLevelKey(ComponentModel component, MetadataKey key) throws MetadataResolvingException
    {
        final MetadataType metadataType = component
                .getModelProperty(MetadataKeyIdModelProperty.class)
                .map(MetadataKeyIdModelProperty::getType)
                .orElseThrow(() -> buildException(format("Component '%s' doesn't have a MetadataKeyId parameter associated", component.getName()), new Exception()));

        final Class<?> type = getType(metadataType);
        final Map<Field, String> fieldValueMap = fillFieldValueMap(type, key);

        Object metadataKeyId;
        try
        {
            metadataKeyId = type.newInstance();
        }
        catch (Exception e)
        {
            throw new MetadataResolvingException("Could not instantiate metadata key object", UNKNOWN, e);
        }

        fieldValueMap.entrySet().forEach(entry -> new FieldSetter<Object, String>(entry.getKey()).set(metadataKeyId, entry.getValue()));
        return metadataKeyId;
    }

    private static Map<Field, String> fillFieldValueMap(Class type, MetadataKey key) throws MetadataResolvingException
    {
        final Map<String, Field> metadataKeyParts = getAnnotatedFields(type, MetadataKeyPart.class).stream().collect(Collectors.toMap(Field::getName, Function.identity()));
        final Map<String, String> currentParts = getCurrentParts(key);
        final List<String> missingParts = metadataKeyParts.keySet().stream().filter(partName -> !currentParts.containsKey(partName)).collect(Collectors.toList());

        if (!missingParts.isEmpty())
        {
            throw new MetadataResolvingException(String.format("The given MetadataKey does not provide all the required levels. Missing levels: %s", missingParts), INVALID_METADATA_KEY);
        }

        return currentParts.entrySet()
                .stream()
                .filter(keyEntry -> metadataKeyParts.containsKey(keyEntry.getKey()))
                .collect(Collectors.toMap(keyEntry -> metadataKeyParts.get(keyEntry.getKey()), Map.Entry::getValue));
    }

    private static Map<String, String> getCurrentParts(MetadataKey key) throws MetadataResolvingException
    {
        Map<String, String> metadataKeyParts = new HashMap<>();
        metadataKeyParts.put(key.getPartName(), key.getId());

        while (!key.getChilds().isEmpty())
        {
            checkOneChildPerLevel(key);
            key = key.getChilds().iterator().next();
            metadataKeyParts.put(key.getPartName(), key.getId());
        }
        return metadataKeyParts;
    }

    private static void checkOneChildPerLevel(MetadataKey key) throws MetadataResolvingException
    {
        if (key.getChilds().size() > 1)
        {
            final List<String> keyNames = key.getChilds().stream().map(child -> child.getId()).collect(Collectors.toList());
            throw buildException(String.format("MetadataKey used for Metadata resolution must only have one child per level. Key '%s' has %s as children.", key.getId(), keyNames));
        }
    }

    private static MetadataResolvingException buildException(String message)
    {
        return buildException(message, null);
    }

    private static MetadataResolvingException buildException(String message, Exception cause)
    {
        return cause == null ? new MetadataResolvingException(message, INVALID_METADATA_KEY)
                             : new MetadataResolvingException(message, INVALID_METADATA_KEY, cause);
    }

    private static boolean isSingleLevelKey(List<ParameterModel> metadataKeyParts)
    {
        return metadataKeyParts.size() == 1;
    }

    private static boolean isKeyLessComponent(List<ParameterModel> metadataKeyParts)
    {
        return metadataKeyParts.isEmpty();
    }

}
