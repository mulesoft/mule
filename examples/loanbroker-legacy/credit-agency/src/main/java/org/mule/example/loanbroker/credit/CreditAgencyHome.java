/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.credit;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * <code>CreditAgencyHome</code> the home inteface for the credit agency service
 */
public interface CreditAgencyHome extends EJBHome
{
    public abstract CreditAgency create() throws RemoteException, CreateException;

}
