/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;

/**
 * A {@link ModelProperty} intended to be used in {@link ParameterModel parameters}
 * owned by a {@link SourceModel}.
 * <p>
 * It indicates that the given parameter is one that applies when a flow yields
 * the result from processing a message produced by the source.
 *
 * @since 4.0
 */
public class CallbackParameterModelProperty implements ModelProperty {

  /**
   * An enum which describes the different phases that a callback can belong to.
   */
  public enum CallbackPhase {
    ON_SUCCESS, ON_ERROR
  }


  private final CallbackPhase callbackPhase;

  /**
   * Creates a new instance
   *
   * @param callbackPhase a {@link CallbackPhase}
   */
  public CallbackParameterModelProperty(CallbackPhase callbackPhase) {
    this.callbackPhase = callbackPhase;
  }

  /**
   * @return The {@link CallbackPhase} for the enriched parameter
   */
  public CallbackPhase getCallbackPhase() {
    return callbackPhase;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "callbackParameter";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isExternalizable() {
    return false;
  }
}
