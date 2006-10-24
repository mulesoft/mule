/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.axis;

/**
 * TODO document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class InvalidTradeException extends Exception
{
    private static final long serialVersionUID = -997233549872918131L;

    public InvalidTradeException()
    {
        super();
    }

    public InvalidTradeException(String message)
    {
        super(message);
    }

    public InvalidTradeException(Throwable cause)
    {
        super(cause);
    }

    public InvalidTradeException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
