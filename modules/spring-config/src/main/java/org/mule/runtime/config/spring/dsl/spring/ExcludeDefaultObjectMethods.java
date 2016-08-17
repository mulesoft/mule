/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Object that represents the <exclude-object-methods/> configuration element
 * 
 * @since 4.0
 */
public class ExcludeDefaultObjectMethods {

  private static Set<String> excludedMethods = ImmutableSet.<String>builder()
      .add("toString")
      .add("hashCode")
      .add("wait")
      .add("notify")
      .add("notifyAll")
      .add("getClass")
      .build();

  public Set<String> getExcludedMethods() {
    return excludedMethods;
  }

}
