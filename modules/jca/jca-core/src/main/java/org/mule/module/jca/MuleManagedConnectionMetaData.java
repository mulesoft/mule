/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
