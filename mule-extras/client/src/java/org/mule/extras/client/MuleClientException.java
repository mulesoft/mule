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
package org.mule.extras.client;

import org.mule.umo.UMOException;

/**
 * <code>MuleClientException</code> is thrown by the <code>MuleClient</code> usually
 * if a endpoint or endpointUri cannot be found.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MuleClientException extends UMOException
{
    public MuleClientException(String message)
    {
        super(message);
    }

    public MuleClientException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
