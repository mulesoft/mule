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
package org.mule.umo.endpoint;

import org.mule.umo.UMOException;

/**
 * <code>EndpointException</code> is an abstract exception extended by endpoint specific
 * exceptions.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class EndpointException extends UMOException
{
    public EndpointException(String message)
    {
        super(message);
    }

    public EndpointException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
