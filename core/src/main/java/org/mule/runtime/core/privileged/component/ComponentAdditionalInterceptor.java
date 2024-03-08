/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import static java.lang.Integer.toHexString;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.internal.component.AnnotatedObjectInvocationHandlerInterceptors;

public class ComponentAdditionalInterceptor {

  private Component obj;

  public Object writeReplace()
      throws Throwable {
    return AnnotatedObjectInvocationHandlerInterceptors.removeDynamicAnnotations(obj);
  }

  @Override
  public String toString() {
    String base = obj.getClass().getName() + "@" + toHexString(obj.hashCode()) + "; location: ";
    if (obj.getLocation() != null) {
      return base + obj.getLocation().getLocation();
    } else {
      return base + "(null)";
    }
  }

  public void setObj(Component obj) {
    this.obj = obj;
  }
}
