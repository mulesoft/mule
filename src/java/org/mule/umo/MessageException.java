/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo;

/**
 * <code>MessageException</code> is a general message exception thrown when
 * errors specific to Message processing.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MessageException extends UMOException
{
    public MessageException(String message)
    {
        super(message);
    }

    public MessageException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
