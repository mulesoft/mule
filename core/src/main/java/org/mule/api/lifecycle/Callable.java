/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleEventContext;

/**
 * <code>Callable</code> is used to provide a Service with an interface that supports
 * event calls. Components do not have to implement this interface, though the
 * <code>onCall</code> method provides an example lifecycle method that is executed
 * when an event is received for the implementing service.
 */
public interface Callable extends EventListener
{

    /**
     * Passes the event to the listener
     * 
     * @param eventContext the context of the current event being process
     * @return Object this object can be anything. When the
     *         <code>LifecycleAdapter</code> for the service receives this
     *         object it will first see if the Object is an <code>MuleMessage</code>
     *         if not and the Object is not null a new message will be created using
     *         the returned object as the payload. This new event will then get
     *         published via the configured outbound router if-
     *         <ol>
     *         <li>One has been configured for the component.</li>
     *         <li>the <code>setStopFurtherProcessing(true)</code> wasn't called
     *         on the event context event.</li>
     *         </ol>
     * @throws Exception if the event fails to process properly. If exceptions aren't
     *             handled by the implementation they will be handled by the
     *             exceptionListener associated with the service
     */
    Object onCall(MuleEventContext eventContext) throws Exception;

}
