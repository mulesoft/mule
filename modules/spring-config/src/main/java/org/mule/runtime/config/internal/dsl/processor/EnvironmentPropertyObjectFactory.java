/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.processor;

import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.Map;

public class EnvironmentPropertyObjectFactory extends AbstractComponentFactory<Map> {

  private Map map;

  public EnvironmentPropertyObjectFactory(Map map) {
    this.map = map;
  }


  @Override
  public Map doGetObject() throws Exception {
    return map;
  }
}
