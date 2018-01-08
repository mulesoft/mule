/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

/**
 * Describes a java bean property
 *
 * @since 4.1
 */
public interface PropertyElement extends WithType, WithName {

  /**
   * @return The accessibility level of this property
   */
  Accessibility getAccess();

  enum Accessibility {
    READ_ONLY, WRITE_ONLY, READ_WRITE
  }

}
