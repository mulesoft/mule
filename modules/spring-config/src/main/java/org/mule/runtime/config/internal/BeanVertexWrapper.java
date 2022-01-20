/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import java.util.Objects;

public class BeanVertexWrapper {

  final Object wrappedObject;
  final String beanName;

  public BeanVertexWrapper(String beanName, Object o) {
    Objects.requireNonNull(o, "bean should not be null");
    this.beanName = beanName;
    this.wrappedObject = o;
  }

  public Object getWrappedObject() {
    return wrappedObject;
  }

  public String getBeanName() {
    return beanName;
  }

  @Override
  public boolean equals(Object obj) {
    if (wrappedObject.equals(obj)) {
      return true;
    } else if (obj instanceof BeanVertexWrapper) {
      return this.getWrappedObject().hashCode() == ((BeanVertexWrapper) obj).getWrappedObject().hashCode();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return wrappedObject.hashCode();
  }
}
