/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.lifecycle;

import org.mule.config.i18n.Message;

/**
 * <code>RecoverableException</code> can be thrown during initialisation to
 * indicate that the error occurred is not fatal and a reactive action can be
 * performed to try and remedy the error. The most common example would be a
 * Connector failing to connect due to a JVM_BIND exception.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RecoverableException extends InitialisationException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5799024626172482665L;

    /**
     * @param message the exception message
     */
    public RecoverableException(Message message, Object object)
    {
        super(message, object);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public RecoverableException(Message message, Object object, Throwable cause)
    {
        super(message, cause, object);
    }
}
