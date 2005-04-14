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
package org.mule.impl.message;

import org.mule.config.ExceptionHelper;
import org.mule.umo.UMOException;
import org.mule.umo.UMOExceptionPayload;

import java.util.Map;

/**
 * <code>ExceptionPayload</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ExceptionPayload implements UMOExceptionPayload
{
    private int code = 0;
    private String message = null;
    private Map info = null;
    private Throwable exception;

    public ExceptionPayload(Throwable exception) {
        this.exception = ExceptionHelper.getRootException(exception);
        UMOException muleRoot = ExceptionHelper.getRootMuleException(exception);
        if(muleRoot!=null) {
            message = muleRoot.getMessage();
            code = muleRoot.getExceptionCode();
            info = muleRoot.getInfo();
        } else {
            message = exception.getMessage();
        }
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map getInfo() {
        return info;
    }

    public Throwable getException() {
        return exception;
    }

}
