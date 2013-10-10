/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
