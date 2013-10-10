/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.config;

import java.util.Map;

/**
 * Provides a strategy interface for reading information from an exception in a
 * consistent way. For example JMS 1.0.2b uses linkedExceptions rather that 'cause'
 * and SQLExceptions hold additional information that can be extracted using this
 * interface.
 */
public interface ExceptionReader
{

    String getMessage(Throwable t);

    Throwable getCause(Throwable t);

    Class<?> getExceptionType();

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @param t the exception to extract the information from
     * @return a map of the non-stanard information stored on the exception
     */
    Map<?, ?> getInfo(Throwable t);

}
