/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.configuration;

import org.mule.runtime.api.component.AbstractComponent;

/**
 * POJO to parse "import" map entry element.
 *
 * @since 4.0
 */
public class ImportEntry extends AbstractComponent {

  String key;
  Class<?> value;

  public String getKey() {
    if (key != null) {
      return key;
    } else if (value != null) {
      return value.getSimpleName();
    } else {
      return null;
    }
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Class<?> getValue() {
    return value;
  }

  public void setValue(Class<?> value) {
    this.value = value;
  }
}
