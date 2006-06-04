/*
 * $Id$
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

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

import java.rmi.RemoteException;

/**
 * <code>DummyHome</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface DummyEjbHome extends EJBHome
{
    public abstract DummyEjb create() throws RemoteException, CreateException;

}
