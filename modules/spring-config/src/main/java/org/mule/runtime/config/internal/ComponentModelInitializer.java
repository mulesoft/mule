/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.internal.model.ComponentModel;

import java.util.function.Predicate;

/**
 * Internal class to be used to initialize {@link ComponentModel} lazily.
 *
 * @since 4.1
 */
public interface ComponentModelInitializer {

  /**
   * Initializes the {@link ComponentModel componentModels} that match for the predicate.
   *
   * @param componentModelPredicate a {@link Predicate} for {@link ComponentModel} to be initialized.
   */
  void initialize(Predicate<ComponentModel> componentModelPredicate);

}
