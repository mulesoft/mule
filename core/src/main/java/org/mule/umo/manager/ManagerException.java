/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.manager;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * <code>ManagerException</code> is thrown when an exception occurs with
 * Manager objects
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ManagerException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1526680893293714180L;

    /**
     * @param message the exception message
     */
    public ManagerException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public ManagerException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
