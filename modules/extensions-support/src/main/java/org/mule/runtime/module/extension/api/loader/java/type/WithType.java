/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
