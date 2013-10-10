/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
