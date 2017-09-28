/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
