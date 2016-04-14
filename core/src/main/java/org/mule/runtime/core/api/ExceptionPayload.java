/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import java.io.Serializable;
import java.util.Map;

/**
 * <code>ExceptionPayload</code> is a message payload that contains exception
 * information that occurred during message processing.
 */
public interface ExceptionPayload extends Serializable
{

    int getCode();

    String getMessage();

    Map getInfo();

    Throwable getException();

    Throwable getRootException();

}
