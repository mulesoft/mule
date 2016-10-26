/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

/**
 * Instances of this classes represent a map entry defined in the configuration.
 *
 * It's possible to create map instances from a set of entries or receive a list of entries for any custom map processing.
 *
 * @since 4.0
 *
 * @param <KeyType> the key type
 * @param <ValueType> the value type
 */
public class MapEntry<KeyType, ValueType> {

  private KeyType key;
  private ValueType value;

  public MapEntry(KeyType key, ValueType value) {
    this.key = key;
    this.value = value;
  }

  /**
   * @return the entry key
   */
  public KeyType getKey() {
    return key;
  }

  /**
   * @return the entry value
   */
  public ValueType getValue() {
    return value;
  }
}
