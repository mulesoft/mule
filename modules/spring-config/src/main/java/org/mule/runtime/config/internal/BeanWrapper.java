/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Objects.requireNonNull;

/**
 * BeanWrapper helps the comparison/equality check among non-proxy vs. proxy or proxy vs. proxy when building a dependency graph.
 */
public class BeanWrapper {

  final Object wrappedObject;
  final String name;

  public BeanWrapper(String name, Object o) {
    requireNonNull(o, "bean must not be null");
    this.name = name;
    this.wrappedObject = o;
  }

  public Object getWrappedObject() {
    return wrappedObject;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (wrappedObject.equals(obj)) {
      return true;
    } else if (obj instanceof BeanWrapper) {
      return this.getWrappedObject().hashCode() == ((BeanWrapper) obj).getWrappedObject().hashCode();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return wrappedObject.hashCode();
  }
}
