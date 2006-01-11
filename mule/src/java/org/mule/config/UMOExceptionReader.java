/*
 * $Header$
 * $Revision$
 * $Date$
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

import org.mule.umo.UMOException;

import java.util.Map;

/**
 * Grabs all information from the UMOexception type
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public final class UMOExceptionReader implements ExceptionReader {

    public String getMessage(Throwable t) {
        return t.getMessage();
    }

    public Throwable getCause(Throwable t) {
        return t.getCause();
    }

    public Class getExceptionType() {
        return UMOException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     *
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t) {
        return ((UMOException)t).getInfo();
    }
}
