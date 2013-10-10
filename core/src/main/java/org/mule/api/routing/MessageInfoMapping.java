/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.routing;

import org.mule.api.MuleMessage;

/**
 * This mapping us used by routers to control how Important message information is pulled from the current
 * message.  It is unlikely that many users will need to configure a custom mapping except where this information
 * is already set within their message type and Mule should use that rather than generate it itself.
 */
public interface MessageInfoMapping
{
    String getMessageId(MuleMessage message);

    String getCorrelationId(MuleMessage message);
}
