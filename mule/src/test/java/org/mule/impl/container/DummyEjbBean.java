/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.impl.container;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import java.rmi.RemoteException;

/**
 * A fake ejb (and invalid) used for testing MUle ejb lookup
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DummyEjbBean implements SessionBean, DummyEjb
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1521532107372250896L;

    public void ejbActivate() throws EJBException
    {
        // nothing to do
    }

    public void ejbPassivate() throws EJBException
    {
        // nothing to do
    }

    public void ejbRemove() throws EJBException
    {
        // nothing to do
    }

    public void ejbCreate() throws EJBException
    {
        // nothing to do
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException
    {
        // nothing to do
    }

    public void dummy()
    {
        // nothing to do
    }

    public EJBHome getEJBHome() throws RemoteException
    {
        return null;
    }

    public Handle getHandle() throws RemoteException
    {
        return null;
    }

    public Object getPrimaryKey() throws RemoteException
    {
        return null;
    }

    public boolean isIdentical(EJBObject ejbObject) throws RemoteException
    {
        return false;
    }

    public void remove() throws RemoteException, RemoveException
    {
        // nothing to do
    }

}
