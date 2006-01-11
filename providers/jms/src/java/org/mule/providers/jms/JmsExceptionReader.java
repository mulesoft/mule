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
package org.mule.providers.jms;

import org.mule.config.ExceptionReader;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;

/**
 * This reader will ensure that the LinkedException and JMS code is not lost when printing the JMSException.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmsExceptionReader implements ExceptionReader {

    public String getMessage(Throwable t) {
        JMSException e = (JMSException)t;
        return e.getMessage() + "(JMS Code: " + e.getErrorCode() + ")";
    }

    public Throwable getCause(Throwable t) {
        JMSException e = (JMSException)t;
        Throwable cause = e.getLinkedException();
        if(cause==null) cause = e.getCause();
        return cause;
    }

    public Class getExceptionType() {
        return JMSException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     *
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t) {
        JMSException e = (JMSException)t;
        Map info = new HashMap();
        info.put("JMS Code", e.getErrorCode());
        return info;
    }
}
