/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
