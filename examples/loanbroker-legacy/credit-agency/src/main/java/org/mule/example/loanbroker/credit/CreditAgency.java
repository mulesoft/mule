/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.credit;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * <code>CreditAgency</code> defines the interface for the credit agency service
 */
public interface CreditAgency extends EJBObject
{
    public String getCreditProfile(String name, Integer ssn) throws RemoteException;
}
