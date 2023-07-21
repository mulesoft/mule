/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

/**
 * A generic contract for any kind of component that could be of certain type
 *
 * @since 4.0
 */
@NoImplement
interface WithType {

  /**
   * @return The {@link TypeWrapper} of the represented component
   */
  Type getType();

}
