/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

/**
 * Indicates that the instantiation of a {@link MuleLoggerContext} is not possible due to the same context being already
 * under construction (logging during the {@link MuleLoggerContext} construction triggers this recursive instantiation)
 *
 * @since 4.3.0
 */
public class RecursiveLoggerContextInstantiationException extends MuleRuntimeException {

  public RecursiveLoggerContextInstantiationException(String message) {
    super(I18nMessageFactory.createStaticMessage(message));
  }
}
