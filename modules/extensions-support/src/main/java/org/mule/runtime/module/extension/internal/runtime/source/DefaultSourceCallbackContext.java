/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Optional.ofNullable;

import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link SourceCallbackContext}
 *
 * @since 4.0
 */
class DefaultSourceCallbackContext implements SourceCallbackContext {

  private final SourceCallback sourceCallback;
  private final Map<String, Object> variables = new HashMap<>();

  /**
   * Creates a new instance
   *
   * @param sourceCallback the owning {@link SourceCallbackContext}
   */
  DefaultSourceCallbackContext(SourceCallback sourceCallback) {
    this.sourceCallback = sourceCallback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasVariable(String variableName) {
    return variables.containsKey(variableName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Optional<T> getVariable(String variableName) {
    return ofNullable((T) variables.get(variableName));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void addVariable(String variableName, Object value) {
    variables.put(variableName, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> SourceCallback<T, A> getSourceCallback() {
    return sourceCallback;
  }
}
