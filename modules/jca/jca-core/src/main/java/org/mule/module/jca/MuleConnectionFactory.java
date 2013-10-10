/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jca;

import java.io.Serializable;

import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

/**
 * <code>MuleConnectionFactory</code> defines the connection factory interface that
 * the RA clients will obtain a reference to.
 */
public interface MuleConnectionFactory extends Serializable, Referenceable
{
    MuleConnection createConnection() throws ResourceException;

    MuleConnection createConnection(MuleConnectionRequestInfo info) throws ResourceException;

    ConnectionManager getManager();

    void setManager(ConnectionManager manager);

    MuleManagedConnectionFactory getFactory();

    void setFactory(MuleManagedConnectionFactory factory);

    MuleConnectionRequestInfo getInfo();

    void setInfo(MuleConnectionRequestInfo info);
}
