/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.util.ClassUtils;

import javax.mail.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractMailFilter</code> is a base class for all javax.mail.Message
 * filters.
 */
public abstract class AbstractMailFilter implements Filter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public final boolean accept(MuleMessage message)
    {
        if (message == null)
        {
            return false;
        }

        Object object = message.getPayload();
        if (object instanceof Message)
        {
            return accept((Message)object);
        }
        else
        {
            throw new IllegalArgumentException("The Mail filter does not understand: "
                                               + ClassUtils.getSimpleName(object.getClass()));
        }
    }

    public abstract boolean accept(Message message);
}
