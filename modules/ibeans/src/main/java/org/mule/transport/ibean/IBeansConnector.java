/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ibean;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.ibeans.config.IBeanHolder;
import org.mule.module.ibeans.spi.MuleIBeansPlugin;
import org.mule.transport.AbstractConnector;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.ibeans.annotation.State;

/**
 * <code>IBeansConnector</code> TODO document
 */
public class IBeansConnector extends AbstractConnector
{
    public static final String STATE_PARAMS_PROPERTY = "ibean.state.params";
    public static final String CALL_PARAMS_PROPERTY = "ibean.call.params";

    private MuleIBeansPlugin iBeansPlugin;

    /* This constant defines the main transport protocol identifier */
    public static final String PROTOCOL = "ibean";

    public IBeansConnector(MuleContext context)
    {
        super(context);
        this.iBeansPlugin = new MuleIBeansPlugin(context);

    }
       
    @Override
    public void doInitialise() throws InitialisationException
    {
        //nothing to do
    }

    @Override
    public void doConnect() throws Exception
    {
        //nothing to do
    }

    @Override
    public void doDisconnect() throws Exception
    {
        //nothing to do
    }

    @Override
    public void doStart() throws MuleException
    {
        //nothing to do
    }

    @Override
    public void doStop() throws MuleException
    {
        //nothing to do
    }

    @Override
    public void doDispose()
    {
        //nothing to do
    }

    public String getProtocol()
    {
        return PROTOCOL;
    }

    public MuleIBeansPlugin getiBeansPlugin()
    {
        return iBeansPlugin;
    }

    public void setiBeansPlugin(MuleIBeansPlugin iBeansPlugin)
    {
        this.iBeansPlugin = iBeansPlugin;
    }

    Object createIbean(EndpointURI uri, List<?> state) throws MuleException
    {
        try {
            Object ibean;
            String address = uri.getAddress();
            int i = address.indexOf(".");
            String ibeanName = address.substring(0, i);
            IBeanHolder holder = getMuleContext().getRegistry().lookupObject(ibeanName);
            if(holder==null)
            {
                throw new IllegalArgumentException();
            }
            ibean = holder.create(getMuleContext(), getiBeansPlugin());

            if(state.size() > 0)
            {
                Class[] types = new Class[state.size()];
                Object[] params = new Object[state.size()];
                int x = 0;
                for (Object o : state)
                {
                    types[x] = o.getClass();
                    params[x++] = o;
                }

                List<Method> methods = ClassUtils.getSatisfiableMethods(holder.getIbeanClass(), types,
                    true, false, Collections.<String>emptyList(), null);
                if(methods.size()==0)
                {
                    throw new IllegalArgumentException("no matching methods");
                }
                else if(methods.size()==1)
                {
                    if(methods.get(0).isAnnotationPresent(State.class))
                    {
                        methods.get(0).invoke(ibean, params);
                    }
                }
                else
                {
                    boolean match = false;
                    for (Method method1 : methods)
                    {
                        if(method1.isAnnotationPresent(State.class))
                        {
                            method1.invoke(ibean, params);
                            match = true;
                            break;
                        }
                    }
                    if(!match)
                    {
                        throw new IllegalArgumentException("no matching @State method");
                    }
                }
            }
            return ibean;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }
}
