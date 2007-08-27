/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.container;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * A test EJB object
 */
public interface DummyEjb extends EJBObject
{
    public void dummy() throws RemoteException;

    public String reverseString(String string) throws RemoteException;

    public String upperCaseString(String string) throws RemoteException;
}
