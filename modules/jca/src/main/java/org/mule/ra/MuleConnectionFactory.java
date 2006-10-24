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

import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import java.io.Serializable;

/**
 * <code>MuleConnectionFactory</code> defines the connection factory interface that
 * the RA clients will obtain a reference to.
 * 
 * @version $Revision$
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
