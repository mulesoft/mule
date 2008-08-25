/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.component;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.NestedRouter;
import org.mule.api.routing.NestedRouterCollection;
import org.mule.component.AbstractComponent;
import org.mule.routing.nested.DefaultNestedRouterCollection;
import org.mule.routing.nested.NestedInvocationHandler;
import org.mule.util.ClassUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A Script service backed by a JSR-223 compliant script engine such as
 * Groovy, JavaScript, or Rhino.
 */
public class ScriptComponent extends AbstractComponent
{

    protected NestedRouterCollection nestedRouter = new DefaultNestedRouterCollection();

    private Scriptable script;

    private Map proxies;

    //@Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        try
        {
            configureComponentBindings();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }

    }

    protected Object doInvoke(MuleEvent event) throws Exception
    {
        // Set up initial script variables.
        Bindings bindings = script.getScriptEngine().createBindings();
        if (proxies.size() > 0)
        {
            bindings.putAll(proxies);
        }
        script.populateBindings(bindings, event);
        return script.runScript(bindings);
    }


    public Scriptable getScript()
    {
        return script;
    }

    public void setScript(Scriptable script)
    {
        this.script = script;
    }

    public NestedRouterCollection getNestedRouter()
    {
        return nestedRouter;
    }

    public void setNestedRouter(NestedRouterCollection nestedRouter)
    {
        this.nestedRouter = nestedRouter;
    }

    protected void configureComponentBindings() throws MuleException
    {
        proxies = new HashMap();
        // Initialise the nested router and bind the endpoints to the methods using a
        // Proxy
        if (nestedRouter != null && nestedRouter.getRouters().size() > 0)
        {
            for (Iterator it = nestedRouter.getRouters().iterator(); it.hasNext();)
            {
                NestedRouter nestedRouter = (NestedRouter) it.next();
                String bindingName = ClassUtils.getSimpleName(nestedRouter.getInterface());
                if (proxies.containsKey(bindingName))
                {
                    Object proxy = proxies.get(bindingName);
                    NestedInvocationHandler handler = (NestedInvocationHandler) Proxy.getInvocationHandler(proxy);
                    handler.addRouterForInterface(nestedRouter);
                }
                else
                {
                    Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{nestedRouter.getInterface()},
                            new NestedInvocationHandler(nestedRouter));
                    proxies.put(bindingName, proxy);
                }

            }
        }
    }
}
