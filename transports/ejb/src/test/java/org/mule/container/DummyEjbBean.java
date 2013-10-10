/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.container;

import org.mule.util.StringUtils;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * A fake (and invalid) EJB used for testing MUle ejb lookup
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

    public String reverseString(String string)
    {
        return StringUtils.reverse(string);
    }

    public String upperCaseString(String string)
    {
        return string.toUpperCase();
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
