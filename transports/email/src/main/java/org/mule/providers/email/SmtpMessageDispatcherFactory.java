/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

/**
 * <code>SmtpMessageDispatcherFactory</code> creates an instance of an SmtpMessage dispatcher used for sending email events
 * via an smtp gateway
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SmtpMessageDispatcherFactory implements UMOMessageDispatcherFactory
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSessionFactory#create(org.mule.umo.provider.UMOConnector)
     */
    public UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException
    {
        return new SmtpMessageDispatcher(endpoint);
    }
}
