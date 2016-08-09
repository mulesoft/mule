/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.component;

import org.mule.compatibility.core.api.component.InterfaceBinding;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class DefaultInterfaceBinding implements InterfaceBinding, MessagingExceptionHandlerAware, Initialisable {

  protected static final Logger logger = LoggerFactory.getLogger(DefaultInterfaceBinding.class);

  private Class<?> interfaceClass;

  private String methodName;

  private MessagingExceptionHandler messagingExceptionHandler;

  // The router used to actually dispatch the message
  protected OutboundEndpoint endpoint;

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    OptimizedRequestContext.unsafeRewriteEvent(event.getMessage());
    return endpoint.process(RequestContext.getEvent());
  }

  @Override
  public void setInterface(Class<?> interfaceClass) {
    this.interfaceClass = interfaceClass;
  }

  @Override
  public Class<?> getInterface() {
    return interfaceClass;
  }

  @Override
  public String getMethod() {
    return methodName;
  }

  @Override
  public void setMethod(String methodName) {
    this.methodName = methodName;
  }

  @Override
  public Object createProxy(Object target) {
    try {
      Object proxy = Proxy.newProxyInstance(getInterface().getClassLoader(), new Class[] {getInterface()},
                                            new BindingInvocationHandler(this));
      if (logger.isDebugEnabled()) {
        logger.debug("Have proxy?: " + (null != proxy));
      }
      return proxy;

    } catch (Exception e) {
      throw new MuleRuntimeException(TransportCoreMessages.failedToCreateProxyFor(target), e);
    }
  }

  @Override
  public void setEndpoint(ImmutableEndpoint e) throws MuleException {
    if (e instanceof OutboundEndpoint) {
      endpoint = (OutboundEndpoint) e;
    } else {
      throw new IllegalArgumentException("An outbound endpoint is required for Interface binding");
    }
  }

  public Class<?> getInterfaceClass() {
    return interfaceClass;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("DefaultInterfaceBinding");
    sb.append("{method='").append(methodName).append('\'');
    sb.append(", interface=").append(interfaceClass);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public ImmutableEndpoint getEndpoint() {
    if (endpoint != null) {
      return endpoint;
    } else {
      return null;
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    endpoint.setMessagingExceptionHandler(messagingExceptionHandler);
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
  }
}
