/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.credit;

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
