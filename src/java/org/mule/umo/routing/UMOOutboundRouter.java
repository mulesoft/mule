/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.List;

/**
 * <code>UMOOutboundRouter</code> is used to control outbound routing
 * behaviour for an event. One or more Outbound routers can be associated with
 * an <code>UMOOutboundMessageRouter</code> and will be selected based on the
 * filters set on the individual Outbound Router.
 * 
 * @see UMOOutboundMessageRouter
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOOutboundRouter extends UMORouter
{
    void setEndpoints(List endpoints);

    List getEndpoints();

    void addEndpoint(UMOEndpoint endpoint);

    boolean removeEndpoint(UMOEndpoint endpoint);

    UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws MessagingException;

    boolean isMatch(UMOMessage message) throws MessagingException;

    UMOTransactionConfig getTransactionConfig();

    void setTransactionConfig(UMOTransactionConfig transactionConfig);

    public String getReplyTo();

    public void setReplyTo(String replyTo);

    public boolean isDynamicEndpoints();

}
