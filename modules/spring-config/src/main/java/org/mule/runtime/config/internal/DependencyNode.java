/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.util.Pair;

import java.util.LinkedList;
import java.util.List;

public class DependencyNode {

  private final Object value;
  private final List<DependencyNode> children = new LinkedList<>();
  private String key;

  public DependencyNode(Object value) {
    this.value = value;
  }

  public DependencyNode(Object value, String key) {
    this.value = value;
    this.key = key;
  }

  public DependencyNode addChild(DependencyNode child) {
    children.add(child);
    return this;
  }

  public List<DependencyNode> getChildren() {
    return children;
  }

  public Object getValue() {
    return value;
  }

  public String getKey() {
    return key;
  }

  public Pair<Object, String> getObjectKeyPair() {
    return new Pair<>(value, key);
  }
}
