/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.matcher;

import org.mule.extension.file.common.api.FileAttributes;

import java.util.function.Predicate;

/**
 * A {@link Predicate} of {@link FileAttributes} instances which accepts any value
 *
 * @since 4.0
 */
public final class NullFilePayloadPredicate<T extends FileAttributes> implements Predicate<T> {

  /**
   * @return {@code true}
   */
  @Override
  public boolean test(T fileAttributes) {
    return true;
  }
}
