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
package org.mule.ra;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;

/**
 * <code>MuleManagedConnectionMetaData</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MuleManagedConnectionMetaData implements ManagedConnectionMetaData
{
    private MuleManagedConnection managedConnection;

    public MuleManagedConnectionMetaData(MuleManagedConnection mc)
    {
        this.managedConnection = mc;
    }

    public String getEISProductName() throws ResourceException {
        return MuleManager.getConfiguration().getVendorName();
    }

    public String getEISProductVersion() throws ResourceException {
        return MuleManager.getConfiguration().getProductVersion();
    }

    //todo
    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    public String getUserName() throws ResourceException {
        if (managedConnection.isDestroyed())
	{
            throw new IllegalStateException(new Message(Messages.X_IS_DISPOSED, managedConnection).toString());
        }
        return managedConnection.getUsername();
    }
}