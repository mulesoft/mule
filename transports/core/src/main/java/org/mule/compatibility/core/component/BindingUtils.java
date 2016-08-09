/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.component;

import org.mule.compatibility.core.api.component.InterfaceBinding;
import org.mule.compatibility.core.api.component.JavaWithBindingsComponent;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.model.resolvers.NoSatisfiableMethodsException;
import org.mule.runtime.core.model.resolvers.TooManySatisfiableMethodsException;
import org.mule.runtime.core.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BindingUtils {

  public static void configureBinding(JavaWithBindingsComponent component, Object componentObject) throws MuleException {
    // Initialise the nested router and bind the endpoints to the methods using a
    // Proxy
    if (component.getInterfaceBindings() != null) {
      Map<Class<?>, Object> bindings = new HashMap<Class<?>, Object>();
      for (InterfaceBinding interfaceBinding : component.getInterfaceBindings()) {
        Object proxy = bindings.get(interfaceBinding.getInterface());

        if (proxy == null) {
          // Create a proxy that implements this interface
          // and just routes away using a mule client
          // ( using the high level Mule client is probably
          // a bit agricultural but this is just POC stuff )
          proxy = interfaceBinding.createProxy(componentObject);
          bindings.put(interfaceBinding.getInterface(), proxy);

          // Now lets set the proxy on the Service object
          Method setterMethod;

          List methods = ClassUtils.getSatisfiableMethods(componentObject.getClass(),
                                                          new Class[] {interfaceBinding.getInterface()}, true, false, null);
          if (methods.size() == 1) {
            setterMethod = (Method) methods.get(0);
          } else if (methods.size() > 1) {
            throw new TooManySatisfiableMethodsException(componentObject.getClass(),
                                                         new Class[] {interfaceBinding.getInterface()});
          } else {
            throw new NoSatisfiableMethodsException(componentObject.getClass(), new Class[] {interfaceBinding.getInterface()});
          }

          try {
            setterMethod.invoke(componentObject, proxy);
          } catch (Exception e) {
            throw new InitialisationException(TransportCoreMessages
                .failedToSetProxyOnService(interfaceBinding, componentObject.getClass()), e, null);
          }
        } else {
          BindingInvocationHandler handler = (BindingInvocationHandler) Proxy.getInvocationHandler(proxy);
          handler.addRouterForInterface(interfaceBinding);
        }
      }
    }
  }

}
