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
import org.mule.api.routing.BindingCollection;
import org.mule.api.routing.InterfaceBinding;
import org.mule.component.AbstractComponent;
import org.mule.routing.binding.BindingInvocationHandler;
import org.mule.routing.binding.DefaultBindingCollection;
import org.mule.util.ClassUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.script.Bindings;

/**
 * A Script service backed by a JSR-223 compliant script engine such as
 * Groovy, JavaScript, or Rhino.
 */
public class ScriptComponent extends AbstractComponent
{

    protected BindingCollection bindingCollection = new DefaultBindingCollection();

    private Scriptable script;

    private Map<String, Object> proxies;

    @Override
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

    @Override
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

    public BindingCollection getBindingCollection()
    {
        return bindingCollection;
    }

    public void setBindingCollection(BindingCollection bindingCollection)
    {
        this.bindingCollection = bindingCollection;
    }

    protected void configureComponentBindings() throws MuleException
    {
        proxies = new HashMap<String, Object>();
        // Initialise the nested router and bind the endpoints to the methods using a
        // Proxy
        if (bindingCollection != null && bindingCollection.getRouters().size() > 0)
        {
            for (Iterator<?> it = bindingCollection.getRouters().iterator(); it.hasNext();)
            {
                InterfaceBinding interfaceBinding = (InterfaceBinding) it.next();
                String bindingName = ClassUtils.getSimpleName(interfaceBinding.getInterface());
                if (proxies.containsKey(bindingName))
                {
                    Object proxy = proxies.get(bindingName);
                    BindingInvocationHandler handler = (BindingInvocationHandler) Proxy.getInvocationHandler(proxy);
                    handler.addRouterForInterface(interfaceBinding);
                }
                else
                {
                    Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), 
                        new Class[]{interfaceBinding.getInterface()}, 
                        new BindingInvocationHandler(interfaceBinding));
                    proxies.put(bindingName, proxy);
                }
            }
        }
    }
}
