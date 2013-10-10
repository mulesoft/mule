/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ejb;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.rmi.RmiConnector;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * Provides Connection configurstion for EJB endpoints
 */

public class EjbConnector extends RmiConnector
{
    public static final String EJB = "ejb";

    // Errorcodes
    public static final int EJB_SERVICECLASS_INVOCATION_FAILED = 2;

    public EjbConnector(MuleContext context)
    {
        super(context);
    }
    
    public String getProtocol()
    {
        return EJB;
    }

    public Remote getRemoteObject(ImmutableEndpoint endpoint) throws RemoteException, UnknownHostException
    {
        EJBObject remoteObj;

        try
        {
            Object ref = getRemoteRef(endpoint);

            Method method = ClassUtils.getMethod(ref.getClass(), "create", null);

            remoteObj = (EJBObject)method.invoke(ref);
        }
        catch (Exception e)
        {
            throw new RemoteException("Remote EJBObject lookup failed for '" + endpoint.getEndpointURI(), e);
        }

        return remoteObj;
    }
}
