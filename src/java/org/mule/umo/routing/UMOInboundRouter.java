/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.routing;

import org.mule.umo.UMOEvent;
import org.mule.umo.MessagingException;

/**
 * <code>UMOInboundRouter</code> defines an interface for an inbound Message
 * router. An imbound router is used to control how events are received by a component.
 * One or more of these routers can be associated with a UMOInboundMessageRouter implementation.
 * @see UMOInboundMessageRouter 
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOInboundRouter extends UMORouter
{
    public UMOEvent[] process(UMOEvent event) throws MessagingException;

    public boolean isMatch(UMOEvent event) throws MessagingException;
}
