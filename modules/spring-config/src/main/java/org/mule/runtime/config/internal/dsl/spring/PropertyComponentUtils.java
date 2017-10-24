/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.internal.model.ComponentModel;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedMap;

/**
 * Utility class to parse spring:property, spring:properties or property components in the configuration.
 *
 * @since 4.0
 */
public class PropertyComponentUtils {

  private static final String VALUE_PARAMETER_NAME = "value";
  private static final String PROPERTY_NAME_MULE_PROPERTY_ATTRIBUTE = "key";
  private static final String PROPERTY_NAME_SPRING_PROPERTY_ATTRIBUTE = "name";
  private static final String PROPERTY_NAME_PROPERTY_ATTRIBUTE = "properties";
  private static final String REFERENCE_MULE_PROPERTY_ATTRIBUTE = "value-ref";
  private static final String REFERENCE_SPRING_PROPERTY_ATTRIBUTE = "ref";

  /**
   * Creates a {@link Pair} from a generic property/ies component in the configuration.
   *
   * @param propertyComponentModel the component model for spring:property, spring:properties or property.
   * @return a {@code PropertyValue} with the parsed content of the component.
   */
  public static Pair<String, Object> getPropertyValueFromPropertyComponent(ComponentModel propertyComponentModel) {
    Pair<String, Object> propertyValue;
    String refKey = getReferenceAttributeName(propertyComponentModel.getIdentifier());
    String nameKey = getNameAttributeName(propertyComponentModel.getIdentifier());
    if (propertyComponentModel.getInnerComponents().isEmpty()) {
      Object value;
      if (propertyComponentModel.getParameters().containsKey(refKey)) {
        value = new RuntimeBeanReference(propertyComponentModel.getParameters().get(refKey));
      } else {
        value = propertyComponentModel.getParameters().get(VALUE_PARAMETER_NAME);
      }
      if (!propertyComponentModel.getParameters().containsKey(nameKey)) {
        propertyValue = new Pair<>(PROPERTY_NAME_PROPERTY_ATTRIBUTE,
                                   new RuntimeBeanReference(propertyComponentModel.getParameters().get("ref")));
      } else {
        propertyValue = new Pair<>(propertyComponentModel.getParameters().get(nameKey), value);
      }
    } else if (propertyComponentModel.getInnerComponents().get(0).getIdentifier().getName().equals("map")) {
      ComponentModel springMap = propertyComponentModel.getInnerComponents().get(0);
      ManagedMap<String, Object> propertiesMap = new ManagedMap<>();
      springMap.getInnerComponents().stream().forEach(mapEntry -> {
        Object value;
        if (mapEntry.getParameters().containsKey(VALUE_PARAMETER_NAME)) {
          value = mapEntry.getParameters().get(VALUE_PARAMETER_NAME);
        } else {
          value = new RuntimeBeanReference(mapEntry.getParameters().get(REFERENCE_MULE_PROPERTY_ATTRIBUTE));
        }
        propertiesMap.put(mapEntry.getParameters().get(PROPERTY_NAME_MULE_PROPERTY_ATTRIBUTE), value);
      });
      propertyValue = new Pair<>(propertyComponentModel.getNameAttribute(), propertiesMap);
    } else {
      throw new MuleRuntimeException(createStaticMessage("Unrecognized property model identifier: "
          + propertyComponentModel.getInnerComponents().get(0).getIdentifier()));
    }
    return propertyValue;
  }

  private static String getNameAttributeName(ComponentIdentifier identifier) {
    if (identifier.equals(MULE_PROPERTY_IDENTIFIER)) {
      return PROPERTY_NAME_MULE_PROPERTY_ATTRIBUTE;
    } else {
      return PROPERTY_NAME_SPRING_PROPERTY_ATTRIBUTE;
    }
  }

  private static String getReferenceAttributeName(ComponentIdentifier identifier) {
    if (identifier.equals(MULE_PROPERTY_IDENTIFIER)) {
      return REFERENCE_MULE_PROPERTY_ATTRIBUTE;
    } else {
      return REFERENCE_SPRING_PROPERTY_ATTRIBUTE;
    }
  }
}
