/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
