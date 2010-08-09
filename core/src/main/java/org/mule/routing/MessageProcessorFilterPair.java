/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;

/**
 * A holder for a pair of MessageProcessor and Filter.
 */
public class MessageProcessorFilterPair
{
    private final MessageProcessor messageProcessor;
    private final Filter filter;

    public MessageProcessorFilterPair(MessageProcessor messageProcessor, Filter filter)
    {
        Validate.notNull(messageProcessor, "messageProcessor can't be null");
        Validate.notNull(filter, "filter can't be null");
        this.messageProcessor = messageProcessor;
        this.filter = filter;
    }

    public MessageProcessor getMessageProcessor()
    {
        return messageProcessor;
    }

    public Filter getFilter()
    {
        return filter;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
