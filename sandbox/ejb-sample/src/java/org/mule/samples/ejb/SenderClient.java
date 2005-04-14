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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOException;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.rmi.RemoteException;

/**
 * <code>SenderClient</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SenderClient {
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(SenderClient.class);

    private Sender sender;
    private String ejbName;

    public SenderClient(String ejbName) throws NamingException, RemoteException, CreateException {
        setEjbName(ejbName);
    }

    public void send(final String message, final String endpoint) throws RemoteException, UMOException, NamingException {
        if (logger.isInfoEnabled()) {
            logger.info("Sending message: " + message);
        }
        sender.send(message, endpoint);
        if (logger.isInfoEnabled()) {
            logger.info("Message sent");
        }
    }

    public String getEjbName() {
        return ejbName;
    }

    private void setEjbName(final String ejbName) throws NamingException, RemoteException, CreateException {
        this.ejbName = ejbName;
        lookupSender(ejbName);
    }

    private void lookupSender(final String ejbName) throws NamingException, RemoteException, CreateException {
        if (logger.isInfoEnabled()) {
            logger.info("Looking up Sender: " + ejbName);
        }
        Context context = new InitialContext();
        Object objectRef = context.lookup(ejbName);
        SenderHome senderHome = (SenderHome) PortableRemoteObject.narrow(objectRef, SenderHome.class);
        sender = senderHome.create();
    }

    public static void main(String[] args) {

        String endpoint;
        if(args.length > 1) {
            endpoint = args[0];
        } else {
            endpoint = "vm://sender.test";
        }
        try {
            SenderClient client = new SenderClient("SenderEJB");
            client.send("Hello Mule", endpoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

