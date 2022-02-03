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

  private final Object object;
  private final List<DependencyNode> children = new LinkedList<>();
  private String name;

  public DependencyNode(Object object) {
    this.object = object;
  }

  public DependencyNode(String name, Object object) {
    this.name = name;
    this.object = object;
  }

  public DependencyNode addChild(DependencyNode child) {
    children.add(child);
    return this;
  }

  public List<DependencyNode> getChildren() {
    return children;
  }

  public Object getObject() {
    return object;
  }

  public String getName() {
    return name;
  }

  public Pair<String, Object> getKeyObjectPair() {
    return new Pair<>(name, object);
  }
}
