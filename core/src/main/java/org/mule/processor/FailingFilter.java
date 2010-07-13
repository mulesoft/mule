/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.FilterException;
import org.mule.config.i18n.CoreMessages;

public class FailingFilter extends SilentFilter
{

    public FailingFilter(Filter filter)
    {
        super(filter);
    }

    protected MuleMessage handleUnacceptedFilter(MuleEvent event) throws FilterException
    {
        throw new FilterException(CoreMessages.messageRejectedByFilter(), filter);
    }

}
