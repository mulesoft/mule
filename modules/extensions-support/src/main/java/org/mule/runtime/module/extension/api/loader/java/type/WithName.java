/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * A generic contract for any kind of component from which, a name can be derived
 *
 * @since 4.0
 */
@NoImplement
interface WithName {

  /**
   * Returns the component's name
   *
   * @return a non blank {@link String}
   */
  String getName();
}
