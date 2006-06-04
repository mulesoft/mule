/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config;

import java.util.Map;

/**
 * Provides a strategy interface for reading information from an exception in a consistent way. For example JMS 1.0.2b
 * Uses linkedExceptions rather that 'cause' and SQLExceptions hold additional information that can be extracted using this
 * interface
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface ExceptionReader {

    public String getMessage(Throwable t);

    public Throwable getCause(Throwable t);

    public Class getExceptionType();

    /**
     * Returns a map of the non-stanard information stored on the exception
     * @param t the exception to extract the information from
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t);
}
