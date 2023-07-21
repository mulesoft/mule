/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * {@link MethodElement} specification for Functions
 *
 * @since 4.1
 */
@NoImplement
public interface FunctionElement extends MethodElement<FunctionContainerElement> {

  /**
   * {@inheritDoc}
   */
  FunctionContainerElement getEnclosingType();

}
