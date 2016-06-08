/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;

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
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public static Object resolve(ComponentModel component, Map<String, String> key) throws MetadataResolvingException
    {
        final List<ParameterModel> metadataKeyParts = IntrospectionUtils.getMetadataKeyParts(component);
        final Optional<MetadataKeyIdModelProperty> keyIdModelProperty = component.getModelProperty(MetadataKeyIdModelProperty.class);

        Object keyIdObject;

        if (isKeyLessComponent(metadataKeyParts))
        {
            keyIdObject = new NullMetadataKey().getId();
        }
        else
        {
            checkKeyIdModelIsPresent(component, keyIdModelProperty);
            keyIdObject = resolveMetadataKeyWhenPresent(key, metadataKeyParts, keyIdModelProperty.get());
        }

        return keyIdObject;
    }

    private static Object resolveMetadataKeyWhenPresent(Map<String, String> key, List<ParameterModel> metadataKeyParts, MetadataKeyIdModelProperty keyIdModelProperty) throws MetadataResolvingException
    {
        Object keyIdObject;

        if (isSingleLevelKey(metadataKeyParts))
        {
            keyIdObject = resolveSingleLevelKey(keyIdModelProperty, key);
        }
        else
        {
            keyIdObject = resolveMultiLevelKey(keyIdModelProperty, key);
        }
        return keyIdObject;
    }

    /**
     * Resolves the KeyIdObject for a MultiLevel {@link MetadataKeyId}
     *
     * @param keyIdModelProperty model property of the {@link MetadataKeyId} parameter
     * @param key                key containing the values of each level
     * @return the KeyIdObject for the {@link MetadataKeyId} parameter
     * @throws MetadataResolvingException
     */
    private static Object resolveMultiLevelKey(MetadataKeyIdModelProperty keyIdModelProperty, Map<String, String> key) throws MetadataResolvingException
    {
        final Class<?> type = getType(keyIdModelProperty.getType());
        checkInstantiable(type);

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

    /**
     * Resolves the KeyIdObject for a simple {@link MetadataKeyId}
     *
     * @param keyIdModelProperty model property of the {@link MetadataKeyId} parameter
     * @param key                key containing the value of the key
     * @return the {@link String} key for the {@link MetadataKeyId} parameter
     * @throws MetadataResolvingException
     */
    private static String resolveSingleLevelKey(MetadataKeyIdModelProperty keyIdModelProperty, Map<String, String> key) throws MetadataResolvingException
    {
        final Class<?> type = getType(keyIdModelProperty.getType());

        if (!type.isAssignableFrom(String.class))
        {
            //TODO REVIEW THIS SHOULD BE A DESIGN TIME VALIDATOR
            throw new MetadataResolvingException("Single level key should be of String type.", FailureCode.INVALID_METADATA_KEY);
        }

        return Optional.ofNullable(key.getOrDefault(keyIdModelProperty.getName(), key.get("key")))
                .orElseThrow(() -> new MetadataResolvingException(String.format("MetadataKey for parameter [%s] has not been provided", keyIdModelProperty.getName()), FailureCode.INVALID_METADATA_KEY));

    }

    private static Map<Field, String> fillFieldValueMap(Class type, Map<String, String> key) throws MetadataResolvingException
    {

        Map<String, Field> metadataKeyParts = getAnnotatedFields(type, MetadataKeyPart.class)
                .stream().collect(Collectors.toMap(Field::getName, Function.identity()));

        final List<String> missingParts = metadataKeyParts.keySet().stream().filter(partName -> !key.containsKey(partName)).collect(Collectors.toList());

        if (!missingParts.isEmpty())
        {
            throw new MetadataResolvingException(String.format("The given MetadataKey does not provide all the required levels. Missing levels: %s", missingParts), FailureCode.INVALID_METADATA_KEY);
        }

        return key.entrySet()
                .stream()
                .filter(keyEntry -> metadataKeyParts.containsKey(keyEntry.getKey()))
                .collect(Collectors.toMap(keyEntry -> metadataKeyParts.get(keyEntry.getKey()), Map.Entry::getValue));
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

    private static void checkKeyIdModelIsPresent(ComponentModel component, Optional<MetadataKeyIdModelProperty> keyIdModelProperty) throws MetadataResolvingException
    {
        if (!keyIdModelProperty.isPresent())
        {
            throw buildException(format("Component '%s' doesn't have a MetadataKeyId parameter associated", component.getName()));
        }
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
