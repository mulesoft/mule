/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

/**
 * {@link MethodElement} specification for Functions
 *
 * @since 4.1
 */
public interface FunctionElement extends MethodElement<FunctionContainerElement> {

  /**
   * {@inheritDoc}
   */
  FunctionContainerElement getEnclosingType();

}
