/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.utils;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Exception which indicates that an attempt to add a {@link org.mule.runtime.http.api.server.RequestMatcher} has resulted in a
 * collision with a previously stored matcher.
 *
 * @since 4.1.5
 */
public class MatcherCollisionException extends MuleRuntimeException {

  public MatcherCollisionException(I18nMessage message) {
    super(message);
  }
}
