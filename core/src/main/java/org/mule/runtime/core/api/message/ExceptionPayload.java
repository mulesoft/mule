/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.message;

import org.mule.runtime.core.api.event.CoreEvent;

import java.io.Serializable;
import java.util.Map;

/**
 * <code>ExceptionPayload</code> is a message payload that contains exception information that occurred during message processing.
 *
 * @deprecated Use {@link org.mule.runtime.api.message.Error} with {@link CoreEvent} instead.
 */
@Deprecated
public interface ExceptionPayload extends Serializable {

  String getMessage();

  Map getInfo();

  Throwable getException();

  Throwable getRootException();

}
