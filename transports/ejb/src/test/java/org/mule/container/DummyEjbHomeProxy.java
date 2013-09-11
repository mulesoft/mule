/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.container;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;

/**
 * Test EJB Home impl
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
