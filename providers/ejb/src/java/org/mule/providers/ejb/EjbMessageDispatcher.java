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
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.rmi.RmiConnector;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.ClassUtils;

import javax.ejb.EJBObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


/**
 * Todo
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public class EjbMessageDispatcher extends AbstractMessageDispatcher
{
    private EjbConnector connector;

    protected EJBObject remoteObject;

    public EjbMessageDispatcher(UMOImmutableEndpoint endpoint) {
        super(endpoint);
        this.connector = (EjbConnector)endpoint.getConnector();
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws IOException, InitialisationException, NotBoundException,
            NoSuchMethodException, ClassNotFoundException
    {
        if (remoteObject==null) {
            String rmiPolicyPath = connector.getSecurityPolicy();

            System.setProperty("java.security.policy", rmiPolicyPath);

            // Set security manager
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new RMISecurityManager());
            }

            remoteObject = getRemoteObject();
        }
    }

    protected void doDisconnect() throws Exception {
        remoteObject=null;
    }

    protected EJBObject getRemoteObject() throws RemoteException, UnknownHostException {
        EJBObject remoteObj;

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
        } catch (Exception e) {
            throw new RemoteException("Remote EJBObject lookup failed for '" + inetAddress.getHostAddress() + ":" + port + serviceName + "'", e);
        }

        return (remoteObj);
    }

    protected Method getMethodObject(UMOEvent event, EJBObject remoteObject) throws UMOException, NoSuchMethodException, ClassNotFoundException {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();

        String methodName = MapUtils.getString(endpointUri.getParams(),
                RmiConnector.PARAM_SERVICE_METHOD, null);

        if (null == methodName) {
            methodName = (String) event.getMessage().getProperty(RmiConnector.PARAM_SERVICE_METHOD);

            if (null == methodName) {
                throw new DispatchException(new org.mule.config.i18n.Message("ejb",
                        RmiConnector.MSG_PARAM_SERVICE_METHOD_NOT_SET), event.getMessage(), event.getEndpoint());
            }
        }

        // Parse method args
        String arguments = (String) event.getMessage().getProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES);

        List methodArgumentTypes = new ArrayList();
        if (null != arguments) {
            String[] split = arguments.split(",");

            for (int i = 0; i < split.length; i++) {
                methodArgumentTypes.add(ClassUtils.loadClass(split[i].trim(), getClass()));

            }
        } else if (event.getTransformedMessage().getClass().isArray()) {
            Object args[] = (Object[]) event.getTransformedMessage();
            for (int i = 0; i < args.length; i++) {
                methodArgumentTypes.add(args[i].getClass());
            }
        } else {
            methodArgumentTypes.add(event.getTransformedMessage().getClass());
        }

        Class types[] = new Class[methodArgumentTypes.size()];
        types = (Class[]) methodArgumentTypes.toArray(types);

        // Returns possible method
        return (remoteObject.getClass().getMethod(methodName, types));
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout  the maximum time the operation should block before returning. The call should
     *                 return immediately if there is data available. If no data becomes available before the timeout
     *                 elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     *         avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception {
        throw new UnsupportedOperationException("doReceive");
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        Object[] arguments = getArgs(event);
        Method invokedMethod = getMethodObject(event, remoteObject);
        invokedMethod.invoke(remoteObject, arguments);
    }

    public UMOMessage doSend(UMOEvent event) throws IllegalAccessException, InvocationTargetException, Exception
    {
        Object[] arguments = getArgs(event);
        Method invokedMethod = getMethodObject(event, remoteObject);
        Object result = invokedMethod.invoke(remoteObject, arguments);
        return (null == result
                    ? null
                    : new MuleMessage(connector.getMessageAdapter(result).getPayload()));
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    protected void doDispose()
    {
        // template method
    }

    private Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();
        return (payload instanceof Object[] ? (Object[]) payload : new Object[]{payload});
    }
}
