/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
