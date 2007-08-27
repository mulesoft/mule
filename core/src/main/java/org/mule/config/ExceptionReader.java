/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

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

    Class getExceptionType();

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @param t the exception to extract the information from
     * @return a map of the non-stanard information stored on the exception
     */
    Map getInfo(Throwable t);

}
