/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.module.extension.internal.introspection.describer.model.Type;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public final class ParameterGroupDescriptor {

  private final String name;

  /**
   * The type of the pojo which implements the group
   */
  private final Type type;

  /**
   * The member in which the generated value of {@link #type} is to be assigned. For {@link ParameterGroupDescriptor}
   * used as fields of a class, this container should be parameterized as a {@link Field}. And if it is used
   * as an argument of an operation it should the corresponding {@link Method}'s {@link Parameter}.
   */
  private final AnnotatedElement container;

  public ParameterGroupDescriptor(String name, Type type, AnnotatedElement container) {
    checkArgument(!isBlank(name), "name cannot be blank");
    checkArgument(type != null, "type cannot be null");
    checkArgument(container != null, "container cannot be null");

    this.name = name;
    this.type = type;
    this.container = container;
  }

  /**
   * @return parameterized container of the {@link ParameterGroupDescriptor}
   */
  public AnnotatedElement getContainer() {
    return container;
  }

  public String getContainerName() {
    if (container instanceof Member) {
      return ((Member) container).getName();
    } else if (container instanceof Parameter) {
      return ((Parameter) container).getName();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }
}
