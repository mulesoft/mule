/*
 * $Header$
 * $Revision$
 * $Date$
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
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DummyEjbBean implements SessionBean, DummyEjb {
    public void ejbActivate() throws EJBException {
    }

    public void ejbPassivate() throws EJBException {
    }

    public void ejbRemove() throws EJBException {
    }

    public void ejbCreate() throws EJBException {
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException {

    }

    public void dummy() {

    }

    public EJBHome getEJBHome() throws RemoteException {
        return null;
    }

    public Handle getHandle() throws RemoteException {
        return null;
    }

    public Object getPrimaryKey() throws RemoteException {
        return null;
    }

    public boolean isIdentical(EJBObject ejbObject) throws RemoteException {
        return false;
    }

    public void remove() throws RemoteException, RemoveException {

    }
}
