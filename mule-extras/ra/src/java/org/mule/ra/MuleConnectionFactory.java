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

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionManager;

/**
 * <code>MuleConnectionFactory</code> defines the connection factory interface that
 * the RA clients will obtain a reference to.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface MuleConnectionFactory extends ConnectionFactory
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
