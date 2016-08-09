/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataKeyParts;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.ValueHolder;
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

/**
 * Provides an instance of the annotated {@link MetadataKeyId} parameter type. The instance will be populated with all the
 * corresponding values of the passed {@link Map} key.
 *
 * @since 4.0
 */
final class MetadataKeyIdObjectResolver {

  /**
   * Given a {@link ComponentModel} and a {@link Map} key, return the populated key in the Type that the {@link Component}
   * parameter requires.
   *
   * @param component the component model that contains the parameter annotated with {@link MetadataKeyId}
   * @param key the {@link MetadataKey} associated to the {@link MetadataKeyId}
   * @return a new instance of the {@link MetadataKeyId} parameter {@code type} with the values of the passed {@link MetadataKey}
   * @throws MetadataResolvingException if:
   *         <ul>
   *         <li>Parameter types is not instantiable</li>
   *         <li>{@param key} does not provide the required levels</li>
   *         <li>{@link MetadataKeyId} is not found in the {@link ComponentModel}</li>
   *         </ul>
   */
  public Object resolve(ComponentModel component, MetadataKey key) throws MetadataResolvingException {
    final List<ParameterModel> metadataKeyParts = getMetadataKeyParts(component);
    return isKeyLessComponent(metadataKeyParts) ? new NullMetadataKey().getId() : resolveMetadataKeyWhenPresent(key, component);
  }

  private Object resolveMetadataKeyWhenPresent(MetadataKey key, ComponentModel componentModel) throws MetadataResolvingException {

    final MetadataType metadataType =
        componentModel.getModelProperty(MetadataKeyIdModelProperty.class).map(MetadataKeyIdModelProperty::getType)
            .orElseThrow(() -> buildException(format("Component '%s' doesn't have a MetadataKeyId parameter associated",
                                                     componentModel.getName())));

    final Class<?> metadataKeyType = getType(metadataType);
    final ValueHolder<Object> keyValueHolder = new ValueHolder<>();
    final ValueHolder<MetadataResolvingException> exceptionValueHolder = new ValueHolder<>();

    metadataType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        exceptionValueHolder.set(buildException(String.format(
                                                              "'%s' type is invalid for MetadataKeyId parameters, use String type instead. Affecting component: '%s'",
                                                              metadataKeyType.getSimpleName(), componentModel.getName())));
      }

      @Override
      public void visitString(StringType stringType) {
        keyValueHolder.set(key.getId());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        try {
          keyValueHolder.set(resolveMultiLevelKey(componentModel, key, metadataKeyType));
        } catch (MetadataResolvingException e) {
          exceptionValueHolder.set(e);
        }
      }
    });

    if (exceptionValueHolder.get() != null) {
      throw exceptionValueHolder.get();
    }

    return keyValueHolder.get();
  }


  /**
   * Resolves the KeyIdObject for a MultiLevel {@link MetadataKeyId}
   *
   * @param componentModel model property of the {@link MetadataKeyId} parameter
   * @param key key containing the values of each level
   * @return the KeyIdObject for the {@link MetadataKeyId} parameter
   * @throws MetadataResolvingException
   */
  private Object resolveMultiLevelKey(ComponentModel componentModel, MetadataKey key, Class metadataKeyType)
      throws MetadataResolvingException {
    final Map<Field, String> fieldValueMap = toFieldValueMap(metadataKeyType, key);

    Object metadataKeyId;
    try {
      metadataKeyId = ClassUtils.instanciateClass(metadataKeyType);
    } catch (Exception e) {
      throw buildException(String.format("MetadataKey object of type '%s' from the component '%s' could not be instantiated",
                                         metadataKeyType.getSimpleName(), componentModel.getName()),
                           e);
    }

    fieldValueMap.entrySet()
        .forEach(entry -> new FieldSetter<Object, String>(entry.getKey()).set(metadataKeyId, entry.getValue()));
    return metadataKeyId;
  }


  private Map<Field, String> toFieldValueMap(Class type, MetadataKey key) throws MetadataResolvingException {
    final Map<String, Field> metadataKeyParts =
        getAnnotatedFields(type, MetadataKeyPart.class).stream().collect(toMap(Field::getName, identity()));
    final Map<String, String> currentParts = getCurrentParts(key);
    final List<String> missingParts =
        metadataKeyParts.keySet().stream().filter(partName -> !currentParts.containsKey(partName)).collect(toList());

    if (!missingParts.isEmpty()) {
      throw new MetadataResolvingException(String
          .format("The given MetadataKey does not provide all the required levels. Missing levels: %s", missingParts),
                                           INVALID_METADATA_KEY);
    }

    return currentParts.entrySet().stream().filter(keyEntry -> metadataKeyParts.containsKey(keyEntry.getKey()))
        .collect(toMap(keyEntry -> metadataKeyParts.get(keyEntry.getKey()), Map.Entry::getValue));
  }

  private Map<String, String> getCurrentParts(MetadataKey key) throws MetadataResolvingException {
    Map<String, String> metadataKeyParts = new HashMap<>();
    metadataKeyParts.put(key.getPartName(), key.getId());

    while (!key.getChilds().isEmpty()) {
      checkOneChildPerLevel(key);
      key = key.getChilds().iterator().next();
      metadataKeyParts.put(key.getPartName(), key.getId());
    }
    return metadataKeyParts;
  }

  private void checkOneChildPerLevel(MetadataKey key) throws MetadataResolvingException {
    if (key.getChilds().size() > 1) {
      final List<String> keyNames = key.getChilds().stream().map(MetadataKey::getId).collect(toList());
      throw buildException(String.format(
                                         "MetadataKey used for Metadata resolution must only have one child per level. Key '%s' has %s as children.",
                                         key.getId(), keyNames));
    }
  }

  private MetadataResolvingException buildException(String message) {
    return buildException(message, null);
  }

  private MetadataResolvingException buildException(String message, Exception cause) {
    return cause == null ? new MetadataResolvingException(message, INVALID_METADATA_KEY)
        : new MetadataResolvingException(message, INVALID_METADATA_KEY, cause);
  }

  private boolean isKeyLessComponent(List<ParameterModel> metadataKeyParts) {
    return metadataKeyParts.isEmpty();
  }

}
