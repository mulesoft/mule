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
 *
 */
 
package org.mule.umo.lifecycle;

import org.mule.config.i18n.Message;

/**
 *  <code>FatalException</code> can be thorwn during initialisation or
 * during execution to indicate that something fatal has occurred and the
 * MuleManage must shutdown
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FatalException extends LifecycleException
{
    /**
     * @param message the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public FatalException(Message message, Object component)
    {
        super(message, component);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public FatalException(Message message, Throwable cause, Object component)
    {
        super(message, cause, component);
    }

    /**
     * @param cause   the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public FatalException(Throwable cause, Object component)
    {
        super(cause, component);
    }
}
