/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

import javax.mail.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractMailFilter</code> is a base class for all javax.mail.Message
 * filters.
 */
public abstract class AbstractMailFilter implements UMOFilter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public final boolean accept(UMOMessage message)
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
                                               + object.getClass().getName());
        }
    }

    public abstract boolean accept(Message message);
}
