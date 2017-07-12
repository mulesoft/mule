/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import org.mule.runtime.api.metadata.DataType;

import java.io.Serializable;
import java.util.Set;

/**
 * Use to obtain message properties defined in two scopes, inbound and outbound.
 *
 * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
 */
@Deprecated
public interface MessageProperties {

  /**
   * Gets an outbound property from the message.
   *
   * @param name the name or key of the property. This must be non-null.
   * @return the property value or null if the property does not exist in the specified scope
   */
  <T extends Serializable> T getOutboundProperty(String name);

  /**
   * Gets an outbound property from the message and provides a default value if the property is not present on the message in the
   * scope specified. The method will also type check against the default value to ensure that the value is of the correct type.
   * If null is used for the default value no type checking is done.
   *
   * @param <T> the defaultValue type ,this is used to validate the property value type
   * @param name the name or key of the property. This must be non-null.
   * @param defaultValue the value to return if the property is not in the scope provided. Can be null
   * @return the property value or the defaultValue if the property does not exist in the specified scope
   * @throws IllegalArgumentException if the value for the property key is not assignable from the defaultValue type
   */
  <T extends Serializable> T getOutboundProperty(String name, T defaultValue);

  /**
   * Gets an outbound property data type from the message.
   *
   * @param name the name or key of the property. This must be non-null.
   * @return the property data type or null if the property does not exist in the specified scope
   */
  DataType getOutboundPropertyDataType(String name);

  /**
   * Gets all outbound property names.
   *
   * @return all outbound property keys of this message
   */
  Set<String> getOutboundPropertyNames();

  /**
   * Gets an inbound property from the message.
   *
   * @param name the name or key of the property. This must be non-null.
   * @return the property value or null if the property does not exist in the specified scope
   */
  <T extends Serializable> T getInboundProperty(String name);

  /**
   * Gets an inbound property from the message and provides a default value if the property is not present on the message in the
   * scope specified. The method will also type check against the default value to ensure that the value is of the correct type.
   * If null is used for the default value no type checking is done.
   *
   * @param <T> the defaultValue type ,this is used to validate the property value type
   * @param name the name or key of the property. This must be non-null.
   * @param defaultValue the value to return if the property is not in the scope provided. Can be null
   * @return the property value or the defaultValue if the property does not exist in the specified scope
   * @throws IllegalArgumentException if the value for the property key is not assignable from the defaultValue type
   */
  <T extends Serializable> T getInboundProperty(String name, T defaultValue);

  /**
   * Gets an inbound property data type from the message.
   *
   * @param name the name or key of the property. This must be non-null.
   * @return the property data type or null if the property does not exist in the specified scope
   */
  DataType getInboundPropertyDataType(String name);

  /**
   * Gets all inbound property names.
   *
   * @return all inbound property keys on this message.
   */
  Set<String> getInboundPropertyNames();

}
