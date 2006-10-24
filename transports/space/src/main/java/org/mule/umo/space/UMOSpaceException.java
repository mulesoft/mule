/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.space;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * A base exception for all space related exceptions
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UMOSpaceException extends UMOException
{
    /**
     *
     */
    private static final long serialVersionUID = 6618481719989683929L;

    public UMOSpaceException(Message message)
    {
        super(message);
    }

    public UMOSpaceException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
