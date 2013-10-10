/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jca;

import org.mule.config.MuleManifest;
import org.mule.module.jca.i18n.JcaMessages;

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
        return MuleManifest.getVendorName();
    }

    public String getEISProductVersion() throws ResourceException
    {
        return MuleManifest.getProductVersion();
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
            throw new IllegalStateException(
                JcaMessages.objectIsDisposed(managedConnection).toString());
        }
        return managedConnection.getUsername();
    }
}
