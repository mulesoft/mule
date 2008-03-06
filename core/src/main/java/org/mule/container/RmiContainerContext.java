/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.container;

import org.mule.api.context.ObjectNotFoundException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.util.ClassUtils;

import javax.naming.NamingException;

/**
 * <code>RmiContainerContext</code> is a container implementaiton that allows RMi
 * objects to be referenced either as components or properties on components
 */
public class RmiContainerContext extends JndiContainerContext
{
    protected String securityPolicy = null;
    protected String securityManager = null;

    protected RmiContainerContext(String name)
    {
        super(name);
    }

    public RmiContainerContext()
    {
        super("rmi");
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return LifecycleTransitionResult.initialiseAll(super.initialise(), new LifecycleTransitionResult.Closure()
        {
            public LifecycleTransitionResult doContinue() throws InitialisationException
            {
                if (securityPolicy != null)
                {
                    if (ClassUtils.getResource(securityPolicy, getClass()) != null)
                    {
                        System.setProperty("java.security.policy", securityPolicy);
                    }
                }

                // Set security manager
                if (System.getSecurityManager() == null)
                {
                    try
                    {
                        if (securityManager != null)
                        {
                            Class clazz = ClassUtils.loadClass(securityManager, getClass());
                            System.setSecurityManager((SecurityManager) clazz.newInstance());
                        }
                    }
                    catch (Exception e)
                    {
                        throw new InitialisationException(e, RmiContainerContext.this);
                    }
                }
                return LifecycleTransitionResult.OK;
            }});
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        Object object;
        if (key == null)
        {
            throw new ObjectNotFoundException("null");
        }
        try
        {
            object = context.lookup(key.toString());
        }
        catch (NamingException e)
        {
            throw new ObjectNotFoundException(key.toString(), e);
        }

        if (object == null)
        {
            throw new ObjectNotFoundException(key.toString());
        }
        else
        {
            return object;
        }
    }

    public String getSecurityPolicy()
    {
        return securityPolicy;
    }

    public void setSecurityPolicy(String securityPolicy)
    {
        this.securityPolicy = securityPolicy;
    }

    public String getSecurityManager()
    {
        return securityManager;
    }

    public void setSecurityManager(String securityManager)
    {
        this.securityManager = securityManager;
    }
}
