/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.transport.MessageAdapter;

import java.util.Map;

/**
 * If the message payload is a map this extractor will look up the property value in
 * the map
 */
public class MapPayloadExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "map-payload";
    
    public Object evaluate(String expression, MuleMessage message)
    {
        Object payload = message;
        if (message instanceof MessageAdapter)
        {
            payload = ((MessageAdapter) message).getPayload();
        }
        if (payload instanceof Map)
        {
            return ((Map) payload).get(expression);
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
