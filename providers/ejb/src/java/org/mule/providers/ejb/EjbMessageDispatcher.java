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
package org.mule.providers.ejb;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;

import javax.ejb.EJBObject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;


/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public class EjbMessageDispatcher extends AbstractMessageDispatcher
{
    private EjbConnector connector;

    private AtomicBoolean initialised = new AtomicBoolean(false);

    protected EJBObject remoteObject;

    protected Method invokedMethod;

    public EjbMessageDispatcher(EjbConnector connector)
    {
        super(connector);

        this.connector = connector;
    }

    protected void initialise(UMOEvent event) throws IOException, InitialisationException, NotBoundException,
            NoSuchMethodException, ClassNotFoundException
    {
        if (!initialised.get()) {
            String rmiPolicyPath = connector.getSecurityPolicy();
            String serverCodebasePath = connector.getServerCodebase();

            System.setProperty("java.security.policy", rmiPolicyPath);
            // System.setProperty("java.rmi.server.codebase",
            // serverCodebasePath);

            // Set security manager
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            remoteObject = EjbConnectorUtil.getRemoteObject(event.getEndpoint(), connector);

            invokedMethod = EjbConnectorUtil.getMethodObject(event.getEndpoint(), remoteObject, connector, this.getClass());

            initialised.set(true);
        }
    }


    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        logger.debug("receive");

        return null;
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        logger.debug("doDispatch");

        initialise(event);

        Object[] arguments = getArgs(event);

        invokedMethod.invoke(remoteObject, arguments);
    }

    public UMOMessage doSend(UMOEvent event) throws IllegalAccessException, InvocationTargetException, Exception
    {
        logger.debug("doSend");

        initialise(event);

        Object[] arguments = getArgs(event);

        Object result = invokedMethod.invoke(remoteObject, arguments);

        return (null == result ? null
                : new MuleMessage(connector.getMessageAdapter(result).getPayload(), null));
    }

    public Object getDelegateSession() throws UMOException
    {
        return (null);
    }

    public void doDispose()
    {
    }

    private Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();

        return (payload instanceof Object[] ? (Object[]) payload : new Object[]{payload});
    }
}
