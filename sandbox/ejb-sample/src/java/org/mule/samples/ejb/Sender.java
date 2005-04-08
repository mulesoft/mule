/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.ejb;

import org.mule.umo.UMOException;

import javax.ejb.EJBObject;
import javax.naming.NamingException;
import java.rmi.RemoteException;

/**
 * <code>Sender</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface Sender extends EJBObject
{
    public void send(String message, String endpoint) throws NamingException, UMOException, RemoteException;
}
