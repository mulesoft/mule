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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import javax.ejb.EJBObject;
import java.lang.reflect.Method;
import java.rmi.RMISecurityManager;


/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public class EjbMessageReceiver extends PollingMessageReceiver
{
    protected transient Log logger = LogFactory.getLog(EjbMessageReceiver.class);

    protected EjbConnector connector;

    protected EJBObject remoteObject;

    protected Method invokedMethod;

    public EjbMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint, Long frequency)
            throws InitialisationException
    {
        super(connector, component, endpoint, frequency);

        this.connector = (EjbConnector) connector;
    }

    public void doConnect() throws Exception
    {
        System.setProperty("java.security.policy", connector.getSecurityPolicy());

        // Set security manager
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        remoteObject = EjbConnectorUtil.getRemoteObject(getEndpoint(), connector);

        invokedMethod = EjbConnectorUtil.getMethodObject(getEndpoint(), remoteObject, connector, this.getClass());
    }

    public void doDisconnect()
    {
        // do nothing
    }

    public void poll()
    {
        logger.debug("polling....");

        try {
            Object result = invokedMethod.invoke(remoteObject, connector.getEjbAble().arguments());

            if (null != result)
                routeMessage(new MuleMessage(connector.getMessageAdapter(result).getPayload(), null), endpoint.isSynchronous());
        }
        catch (Exception e) {
            handleException(e);
        }
    }
}
