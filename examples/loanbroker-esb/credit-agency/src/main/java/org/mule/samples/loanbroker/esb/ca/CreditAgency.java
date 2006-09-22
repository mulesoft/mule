/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.loanbroker.esb.ca;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * <code>CreditAgency</code> defines the interface for the credit agency service
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface CreditAgency extends EJBObject
{
    public String getCreditProfile(String name, Integer ssn) throws RemoteException;
}
