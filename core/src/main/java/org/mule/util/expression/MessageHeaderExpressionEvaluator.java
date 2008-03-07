/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.expression;

import org.mule.api.transport.MessageAdapter;
import org.mule.transport.NullPayload;

/**
 * Looks up the property on the message using the name given.
 */
public class MessageHeaderExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "header";

    public Object evaluate(String name, Object message)
    {
        if (message instanceof MessageAdapter)
        {
            if (name.equalsIgnoreCase("payload"))
            {
                Object payload = ((MessageAdapter) message).getPayload();
                return (payload instanceof NullPayload ? null : payload);
            }
            else
            {
                return ((MessageAdapter) message).getProperty(name);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }

    /** {@inheritDoc} */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }
}
