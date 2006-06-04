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
package org.mule.providers.ejb;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.rmi.RmiConnector;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;

import javax.ejb.EJBObject;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public class EjbConnectorUtil
{
    protected static transient Log logger = LogFactory.getLog(EjbConnectorUtil.class);

    private EjbConnectorUtil()
    {
        // prevent instantiation.
    }

    /**
     * @param endpoint
     * @param connector
     * @return Remote EJB object from Appserver
     * @throws RemoteException
     * @throws UnknownHostException
     */

    public static EJBObject getRemoteObject(UMOEndpoint endpoint, EjbConnector connector) throws RemoteException, UnknownHostException
    {
        EJBObject remoteObj = null;

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();

        int port = endpointUri.getPort();

        if (port < 1) {
            port = RmiConnector.DEFAULT_RMI_REGISTRY_PORT;
        }

        InetAddress inetAddress = InetAddress.getByName(endpointUri.getHost());

        String serviceName = endpointUri.getPath();

        try {
            Object ref = connector.getJndiContext(inetAddress.getHostAddress() + ":" + port).lookup(serviceName);

            Method method = ClassUtils.getMethod("create", null, ref.getClass());

            remoteObj = (EJBObject) method.invoke(ref, ClassUtils.NO_ARGS);
        }
        catch (Exception e) {
            throw new RemoteException("Remote EJBObject lookup failed for '" + inetAddress.getHostAddress() + ":" + port + serviceName + "'", e);
        }

        return (remoteObj);
    }

    /**
     * @param endpoint
     * @param remoteObject
     * @param connector
     * @param clazz
     * @return Actual remote method to be invoked
     * @throws InitialisationException
     * @throws NoSuchMethodException
     */
    public static Method getMethodObject(UMOEndpoint endpoint, EJBObject remoteObject, EjbConnector connector, Class clazz) throws InitialisationException, NoSuchMethodException
    {
        UMOEndpointURI endpointUri = endpoint.getEndpointURI();

        String methodName = MapUtils.getString(endpointUri.getParams(),
                RmiConnector.PARAM_SERVICE_METHOD, null);

        if (null == methodName) {
            methodName = (String) endpoint.getProperties().get(RmiConnector.PARAM_SERVICE_METHOD);

            if (null == methodName) {
                throw new InitialisationException(new org.mule.config.i18n.Message("ejb",
                        RmiConnector.MSG_PARAM_SERVICE_METHOD_NOT_SET), endpoint);
            }
        }

        Class[] argumentClasses = null;

        // Attempt to init params
        if (clazz.equals(EjbMessageReceiver.class)) {
            try {
                EjbAble object = (EjbAble) ClassUtils.instanciateClass(connector.getReceiverArgumentClass(), ClassUtils.NO_ARGS);

                argumentClasses = object.argumentClasses();

                connector.setEjbAble(object);
            }
            catch (Exception e) {
                throw new InitialisationException(new org.mule.config.i18n.Message("ejb", EjbConnector.EJB_SERVICECLASS_INVOCATION_FAILED), e);
            }
        } else {
            argumentClasses = connector.getArgumentClasses();
        }

        return (remoteObject.getClass().getMethod(methodName, argumentClasses));
    }
}
