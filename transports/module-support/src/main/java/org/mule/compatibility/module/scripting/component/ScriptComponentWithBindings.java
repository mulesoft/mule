/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.scripting.component;

import org.mule.compatibility.core.api.component.InterfaceBinding;
import org.mule.compatibility.core.component.BindingInvocationHandler;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.module.scripting.component.ScriptComponent;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;

/**
 * A Script service that supports java interface bindings.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class ScriptComponentWithBindings extends ScriptComponent {

  protected List<InterfaceBinding> bindings = new ArrayList<InterfaceBinding>();

  private Map<String, Object> proxies;

  @Override
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();
    try {
      configureComponentBindings();
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }

  public List<InterfaceBinding> getInterfaceBindings() {
    return bindings;
  }

  public void setInterfaceBindings(List<InterfaceBinding> bindingCollection) {
    this.bindings = bindingCollection;
  }

  @Override
  protected void putBindings(Bindings bindings) {
    if (proxies.size() > 0) {
      bindings.putAll(proxies);
    }
  }

  protected void configureComponentBindings() throws MuleException {
    proxies = new HashMap<String, Object>();
    // Initialise the nested router and bind the endpoints to the methods using a
    // Proxy
    if (bindings != null && bindings.size() > 0) {
      for (Iterator<?> it = bindings.iterator(); it.hasNext();) {
        InterfaceBinding interfaceBinding = (InterfaceBinding) it.next();
        String bindingName = ClassUtils.getSimpleName(interfaceBinding.getInterface());
        if (proxies.containsKey(bindingName)) {
          Object proxy = proxies.get(bindingName);
          BindingInvocationHandler handler = (BindingInvocationHandler) Proxy.getInvocationHandler(proxy);
          handler.addRouterForInterface(interfaceBinding);
        } else {
          Object proxy =
              Proxy.newProxyInstance(muleContext.getExecutionClassLoader(), new Class[] {interfaceBinding.getInterface()},
                                     new BindingInvocationHandler(interfaceBinding));
          // new BindingInvocationHandler(interfaceBinding, muleContext));
          proxies.put(bindingName, proxy);
        }
      }
    }
  }
}
