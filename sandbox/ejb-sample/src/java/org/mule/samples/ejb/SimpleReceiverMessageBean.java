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

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

/**
 * <code>SimpleReceiverBean</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SimpleReceiverMessageBean implements MessageDrivenBean, Callable {
    private MessageDrivenContext messageDrivenContext;

    public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) throws EJBException {
        this.messageDrivenContext = messageDrivenContext;
    }

    public void ejbRemove() throws EJBException {
    }

    public void ejbCreate() throws EJBException {
    }

    public Object onCall(UMOEventContext context) throws Exception {
        Object msg = context.getTransformedMessage();
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println("@@ Received: " + msg);
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        return msg;
    }
}
