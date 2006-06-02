/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.ejb;

import org.mule.ra.MuleConnection;
import org.mule.umo.UMOException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * <code>SenderBean</code> is a simple session bean used to send an event
 * via a Mule connection
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SenderBean implements SessionBean
{
    private SessionContext ctx;

    public void ejbActivate() throws EJBException {
    }

    public void ejbPassivate() throws EJBException {
    }

    public void ejbRemove() throws EJBException {
    }

    public void ejbCreate() throws EJBException {
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException {
        ctx = sessionContext;
    }

    public void send(String message, String endpoint) throws NamingException, UMOException {
        InitialContext initCtx = new InitialContext();
        MuleConnection connection = (MuleConnection)initCtx.lookup("java:comp/env/mule/connectionFactory");
        connection.dispatch(endpoint, message, null);
    }
}
