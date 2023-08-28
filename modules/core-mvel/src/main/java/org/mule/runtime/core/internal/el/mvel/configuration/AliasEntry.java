/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.configuration;

import org.mule.runtime.api.component.AbstractComponent;

/**
 * POJO to parse "alias" map entry element.
 *
 * @since 4.0
 */
public class AliasEntry extends AbstractComponent {

  String key;
  String value;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
