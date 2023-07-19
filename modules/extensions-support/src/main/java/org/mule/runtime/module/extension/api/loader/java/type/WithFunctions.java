/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

import java.util.List;

/**
 * A generic contract for any kind of component from which, a list of operations can be derived
 *
 * @since 4.0
 */
@NoImplement
interface WithFunctions {

  /**
   * @return a list of {@link MethodElement}
   */
  List<FunctionElement> getFunctions();
}
