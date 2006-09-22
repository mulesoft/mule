/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.umo.UMOEventContext;

/**
 * <code>Callable</code> is used to provide UMOs with an interface
 * that supports event calls. UMO components do not have to implement this
 * interface, though the <code>onCall</code> method provides an example lifecycle method that
 * is executed when an event is received for the implementing component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface Callable extends UMOEventListener
{
    /**
     * Passes the event to the listener
     *
     * @param eventContext the context of the current event being process
     * @return Object this object can be anything. When the
     *         <code>UMOLifecycleAdapter</code> for the component receives
     *         this object it will first see if the Object is an
     *         <code>UMOMessage</code> if not and the Object is not null a new
     *         message will be created using the returned object as the payload.
     *         This new event will then get published via the configured
     *         outbound router if-
     *         <ol>
     *         <li>One has been configured for the UMO.</li>
     *         <li>the <code>setStopFurtherProcessing(true)</code> wasn't
     *         called on the event context event.</li>
     *         </ol>
     * @throws Exception if the event fails to process properly. If exceptions
     *             aren't handled by the implementation they will be handled by
     *             the exceptionListener associated with the component
     */
    Object onCall(UMOEventContext eventContext) throws Exception;
}
