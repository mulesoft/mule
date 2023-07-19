/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util;

import java.lang.reflect.Type;

/**
 * A supplier which returns a {@link Type}
 *
 * @since 4.2
 */
@FunctionalInterface
public interface TypeSupplier {

  /**
   * @return A {@link Type}
   */
  Type getType();
}
