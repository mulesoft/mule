/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * <code>MuleManagedConnectionMetaData</code> TODO
 */
public class MuleManagedConnectionMetaData implements ManagedConnectionMetaData
{
    private final MuleManagedConnection managedConnection;

    public MuleManagedConnectionMetaData(MuleManagedConnection mc)
    {
        this.managedConnection = mc;
    }

    public String getEISProductName() throws ResourceException
    {
        //TODO RM* return RegistryContext.getConfiguration().getVendorName();
        return null;
    }

    public String getEISProductVersion() throws ResourceException
    {
        //TODO return RegistryContext.getConfiguration().getProductVersion();
        return null;
    }

    // TODO
    public int getMaxConnections() throws ResourceException
    {
        return 0;
    }

    public String getUserName() throws ResourceException
    {
        if (managedConnection.isDestroyed())
        {
            throw new IllegalStateException(new Message(Messages.X_IS_DISPOSED, managedConnection).toString());
        }
        return managedConnection.getUsername();
    }
}
