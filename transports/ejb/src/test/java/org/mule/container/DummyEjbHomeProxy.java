/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
