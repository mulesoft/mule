/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import org.mule.runtime.core.api.event.CoreEvent;

import java.io.Serializable;
import java.util.Map;

/**
 * <code>ExceptionPayload</code> is a message payload that contains exception information that occurred during message processing.
 *
 * @Deprecated Use {@link org.mule.runtime.api.message.Error} with {@link CoreEvent} instead.
 */
@Deprecated
public interface ExceptionPayload extends Serializable {

  String getMessage();

  Map getInfo();

  Throwable getException();

  Throwable getRootException();

}
