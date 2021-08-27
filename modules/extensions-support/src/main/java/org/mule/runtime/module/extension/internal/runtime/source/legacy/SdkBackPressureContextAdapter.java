/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import static org.mule.runtime.module.extension.internal.runtime.source.legacy.SdkBackPressureActionUtils.from;

import org.mule.runtime.api.event.Event;
import org.mule.sdk.api.runtime.source.BackPressureAction;
import org.mule.sdk.api.runtime.source.BackPressureContext;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.source.BackPressureContext} into a {@link BackPressureContext}
 *
 * @since 4.4.0
 */
public class SdkBackPressureContextAdapter implements BackPressureContext {

  private final org.mule.runtime.extension.api.runtime.source.BackPressureContext delegate;

  public SdkBackPressureContextAdapter(org.mule.runtime.extension.api.runtime.source.BackPressureContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public BackPressureAction getAction() {
    return from(delegate.getAction());
  }

  @Override
  public Event getEvent() {
    return delegate.getEvent();
  }

  @Override
  public SourceCallbackContext getSourceCallbackContext() {
    return new SdkSourceCallBackContextAdapter(delegate.getSourceCallbackContext());
  }
}
