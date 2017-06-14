/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Describes the the class in which a parameter group is being implemented and the
 * element which contains it
 *
 * @since 4.0
 */
public final class ParameterGroupDescriptor {

  private final String name;

  /**
   * The type of the pojo which implements the group
   */
  private final Type type;

  /**
   * The {@link MetadataType} of the pojo which implements the group
   */
  private final MetadataType metadataType;

  /**
   * The member in which the generated value of {@link #type} is to be assigned. For {@link ParameterGroupDescriptor}
   * used as fields of a class, this container should be parameterized as a {@link Field}. And if it is used
   * as an argument of an operation it should the corresponding {@link Method}'s {@link Parameter}.
   */
  private final AnnotatedElement container;

  public ParameterGroupDescriptor(String name, Type type, MetadataType metadataType, AnnotatedElement container) {
    checkArgument(!isBlank(name), "name cannot be blank");
    checkArgument(type != null, "type cannot be null");
    this.name = name;
    this.type = type;
    this.container = container;
    this.metadataType = metadataType;
  }

  public ParameterGroupDescriptor(String name, Type type) {
    this(name, type, null, null);
  }

  /**
   * @return parameterized container of the {@link ParameterGroupDescriptor}
   */
  public AnnotatedElement getContainer() {
    return container;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public MetadataType getMetadataType() {
    return metadataType;
  }

}
