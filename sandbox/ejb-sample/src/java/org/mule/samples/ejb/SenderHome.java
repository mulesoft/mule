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
package org.mule.samples.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * <code>SenderHome</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface SenderHome extends EJBHome
{
    public abstract Sender create() throws RemoteException, CreateException;

}
