/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

/**
 * A generic contract for any kind of component which is derived from a declaration inside a {@link Class}
 *
 * @since 4.0
 */
public interface WithDeclaringClass {

  /**
   * @return the class that this {@link Type} represents
   */
  // TODO MULE-10137 - Adapt logic to AST
  Class getDeclaringClass();
}
