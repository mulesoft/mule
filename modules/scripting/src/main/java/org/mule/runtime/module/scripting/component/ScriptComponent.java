/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting.component;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.InterfaceBinding;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.component.AbstractComponent;
import org.mule.runtime.core.component.BindingInvocationHandler;
import org.mule.runtime.core.util.ClassUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Script service backed by a JSR-223 compliant script engine such as
 * Groovy, JavaScript, or Rhino.
 */
public class ScriptComponent extends AbstractComponent
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptComponent.class);

    protected List<InterfaceBinding> bindings = new ArrayList<InterfaceBinding>();

    private Scriptable script;

    private Map<String, Object> proxies;

    @Override
    protected void doInitialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(script, muleContext);
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
    protected void doDispose()
    {
        LifecycleUtils.disposeIfNeeded(script, LOGGER);
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
        try
        {
            return script.runScript(bindings);
        }
        catch (Exception e)
        {
            // leave this catch block in place to help debug classloading issues
            throw e;
        } finally {
            bindings.clear();
        }
    }

    public Scriptable getScript()
    {
        return script;
    }

    public void setScript(Scriptable script)
    {
        this.script = script;
    }

    public List<InterfaceBinding> getInterfaceBindings()
    {
        return bindings;
    }

    public void setInterfaceBindings(List<InterfaceBinding> bindingCollection)
    {
        this.bindings = bindingCollection;
    }

    protected void configureComponentBindings() throws MuleException
    {
        proxies = new HashMap<String, Object>();
        // Initialise the nested router and bind the endpoints to the methods using a
        // Proxy
        if (bindings != null && bindings.size() > 0)
        {
            for (Iterator<?> it = bindings.iterator(); it.hasNext();)
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
                    Object proxy = Proxy.newProxyInstance(muleContext.getExecutionClassLoader(),
                        new Class[]{interfaceBinding.getInterface()},
                            new BindingInvocationHandler(interfaceBinding));
                    // new BindingInvocationHandler(interfaceBinding, muleContext));
                    proxies.put(bindingName, proxy);
                }
            }
        }
    }
}
