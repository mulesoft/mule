/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.component;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;

public interface InterfaceBinding extends MessageProcessor
{
    /**
     * This method is responsible for routing the Message via the MuleSession. The
     * logic for this method will change for each type of router depending on
     * expected behaviour. For example, a MulticastingRouter might just iterate
     * through the list of assoaciated endpoints sending the message. Another type of
     * router such as the ExceptionBasedRouter will hit the first endpoint, if it
     * fails try the second, and so on. Most router implementations will extends the
     * FilteringOutboundRouter which implements all the common logic need for a
     * router.
     *
     * @return a result message if any from the invocation. If the endpoint bound has
     *         a one-way exchange pattern configured a null result will always be
     *         returned.
     * @throws MessagingException if any errors occur during the sending of messages
     * @throws MuleException
     * @see org.mule.routing.outbound.FilteringOutboundRouter
     * @see org.mule.routing.outbound.ExceptionBasedRouter
     * @see org.mule.routing.outbound.MulticastingRouter
     * @since 2.1 the synchronous argument has been removed. Instead use the
     *        synchronous attribute of the endpoint you are dispatching to.
     */
    @Override
    MuleEvent process(MuleEvent event) throws MuleException;

    void setEndpoint(ImmutableEndpoint endpoint) throws MuleException;

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
