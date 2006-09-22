/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.container;

import javax.ejb.CreateException;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;

import java.rmi.RemoteException;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DummyEjbHomeProxy implements DummyEjbHome
{
    public DummyEjb create() throws RemoteException, CreateException
    {
        return new DummyEjbBean();
    }

    public EJBMetaData getEJBMetaData() throws RemoteException
    {
        return null;
    }

    public HomeHandle getHomeHandle() throws RemoteException
    {
        return null;
    }

    public void remove(Handle handle) throws RemoteException, RemoveException
    {
        // nothing to do
    }

    public void remove(Object object) throws RemoteException, RemoveException
    {
        // nothing to do
    }

}
