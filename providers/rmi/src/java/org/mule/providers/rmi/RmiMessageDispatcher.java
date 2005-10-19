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
package org.mule.providers.rmi;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.PropertiesHelper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

/**
 * <code>RmiMessageDispatcher</code> will send transformed mule events over
 * RMI-JRMP.
 *
 * @author <a href="mailto:fsweng@bass.com.my">fs Weng</a>
 * @version $Revision$
 */
public class RmiMessageDispatcher extends AbstractMessageDispatcher {

    protected static transient Log logger = LogFactory.getLog(RmiMessageDispatcher.class);

    private RmiConnector connector;

    private AtomicBoolean initialised = new AtomicBoolean(false);

    private InetAddress inetAddress;

    private int port;

    private String serviceName;

    protected Remote remoteObject;

    protected Method invokedMethod;

    public RmiMessageDispatcher(RmiConnector connector) {
        super(connector);
        this.connector = connector;
    }

    protected void initialise(UMOEvent event) throws IOException, DispatchException, NotBoundException,
            NoSuchMethodException, ClassNotFoundException {
        if (!initialised.get()) {

            remoteObject = getRemoteObject(event);
            invokedMethod = getMethodObject(event, remoteObject);

            initialised.set(true);
        }
    }

    private Remote getRemoteObject(UMOEvent event) throws RemoteException, MalformedURLException,
            NotBoundException, UnknownHostException {
        Remote remoteObj;

        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();

        // TODO - add more error handling on uri
        port = endpointUri.getPort();
        if (port < 1) {
            port = RmiConnector.DEFAULT_RMI_REGISTRY_PORT;
        }

        inetAddress = InetAddress.getByName(endpointUri.getHost());
        serviceName = endpointUri.getPath();

        String name = "//" + inetAddress.getHostAddress() + ":" + port + serviceName;

        remoteObj = Naming.lookup(name);

        return remoteObj;
    }

    private Method getMethodObject(UMOEvent event, Remote remoteObject) throws DispatchException,
            NoSuchMethodException, ClassNotFoundException {
        Method method;
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();

        String methodName = PropertiesHelper.getStringProperty(endpointUri.getParams(),
                RmiConnector.PARAM_SERVICE_METHOD, null);

        if (methodName == null) {
            methodName = (String) event.getEndpoint().getProperties().get(RmiConnector.PARAM_SERVICE_METHOD);
            if (methodName == null) {
                throw new DispatchException(new org.mule.config.i18n.Message("rmi",
                        RmiConnector.MSG_PARAM_SERVICE_METHOD_NOT_SET),
                        event.getMessage(),
                        event.getEndpoint());
            }
        }


        List methodArgumentTypes = (List) event.getEndpoint()
                .getProperties()
                .get(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES);
        if (methodArgumentTypes != null) {
            connector.setMethodArgumentTypes(methodArgumentTypes);
        }

        Class [] argTypes = connector.getArgumentClasses();
        method = remoteObject.getClass().getMethod(methodName, argTypes);

        return method;
    }

    private Object[] getArgs(UMOEvent event) throws TransformerException {
        Object payload = event.getTransformedMessage();
        Object[] args;
        if (payload instanceof Object[]) {
            args = (Object[]) payload;
        } else {
            args = new Object[]{payload};
        }
        return args;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#dispatch(org.mule.umo.UMOEvent)
     */
    public void doDispatch(UMOEvent event) throws Exception {
        initialise(event);

        Object[] arguments = getArgs(event);
        invokedMethod.invoke(remoteObject, arguments);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#send(org.mule.umo.UMOEvent)
     */
    public UMOMessage doSend(UMOEvent event) throws IllegalAccessException, InvocationTargetException, Exception {

        UMOMessage resultMessage;

        initialise(event);

        Object[] arguments = getArgs(event);
        Object result = invokedMethod.invoke(remoteObject, arguments);

        if (result == null) {
            return null;
        } else {
            resultMessage = new MuleMessage(connector.getMessageAdapter(result).getPayload(), Collections.EMPTY_MAP);
        }

        return resultMessage;
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        throw new UnsupportedOperationException("receive on RMIMessageDispatcher");
    }

    /**
     * There is no associated session for a RMI connector
     *
     * @return
     * @throws UMOException
     */
    public Object getDelegateSession() throws UMOException {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#getConnector()
     */
    public UMOConnector getConnector() {
        return connector;
    }

    public void doDispose() {
    }

}
