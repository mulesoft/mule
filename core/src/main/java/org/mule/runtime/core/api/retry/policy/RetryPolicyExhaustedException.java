/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.retry.policy;

import org.mule.runtime.core.api.lifecycle.FatalException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * This exception is thrown when a Retry policy has made all the retry attempts it wants to make and is still failing.
 */
public final class RetryPolicyExhaustedException extends FatalException {

  /** Serial version */
  private static final long serialVersionUID = 3300563235465630597L;

  public RetryPolicyExhaustedException(I18nMessage message, Object component) {
    super(message, component);
  }

  public RetryPolicyExhaustedException(I18nMessage message, Throwable cause, Object component) {
    super(message, cause, component);
  }

  public RetryPolicyExhaustedException(Throwable cause, Object component) {
    super(cause, component);
  }
}
