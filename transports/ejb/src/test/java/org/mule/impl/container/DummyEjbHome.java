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
import javax.ejb.EJBHome;

import java.rmi.RemoteException;

/**
 * <code>DummyHome</code> Test EJB
 */
public interface DummyEjbHome extends EJBHome
{
    public abstract DummyEjb create() throws RemoteException, CreateException;

}
