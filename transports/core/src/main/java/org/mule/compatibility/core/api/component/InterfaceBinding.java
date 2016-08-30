/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.component;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;

/* 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface InterfaceBinding extends MessageProcessor {

  /**
   * This method is responsible for routing the Message via the MuleSession. The logic for this method will change for each type
   * of router depending on expected behaviour. For example, a MulticastingRouter might just iterate through the list of
   * assoaciated endpoints sending the message. Another type of router such as the ExceptionBasedRouter will hit the first
   * endpoint, if it fails try the second, and so on. Most router implementations will extends the FilteringOutboundRouter which
   * implements all the common logic need for a router.
   *
   * @return a result message if any from the invocation. If the endpoint bound has a one-way exchange pattern configured a null
   *         result will always be returned.
   * @throws MessagingException if any errors occur during the sending of messages
   * @throws MuleException
   * @see org.mule.compatibility.core.routing.outbound.FilteringOutboundRouter
   * @see org.mule.compatibility.core.routing.outbound.ExceptionBasedRouter
   * @see org.mule.compatibility.core.routing.outbound.MulticastingRouter
   * @since 2.1 the synchronous argument has been removed. Instead use the synchronous attribute of the endpoint you are
   *        dispatching to.
   */
  @Override
  MuleEvent process(MuleEvent event) throws MuleException;

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  void setEndpoint(ImmutableEndpoint endpoint) throws MuleException;

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  ImmutableEndpoint getEndpoint();

  Class<?> getInterface();

  void setInterface(Class<?> interfaceClass);

  String getMethod();

  void setMethod(String method);

  /**
   * This wires the dynamic proxy to the service object.
   */
  Object createProxy(Object target);
}
