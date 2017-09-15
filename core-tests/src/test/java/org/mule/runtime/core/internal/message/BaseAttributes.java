/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Basic attributes implementation that defines a generic {@link #toString()} method.
 * 
 * @since 4.0
 */
public abstract class BaseAttributes implements Serializable {

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
  }
}
