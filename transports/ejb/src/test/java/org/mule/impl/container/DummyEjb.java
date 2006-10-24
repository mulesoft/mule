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

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * A test EJB object
 */
public interface DummyEjb extends EJBObject
{
    public void dummy() throws RemoteException;

    public String reverseString(String string) throws RemoteException;

    public String upperCaseString(String string) throws RemoteException;
}
