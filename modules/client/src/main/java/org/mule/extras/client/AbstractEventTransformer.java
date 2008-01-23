/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.client;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

import java.lang.reflect.Method;

/**
 * <code>AbstractEventTransformer</code> adds support for adding method details to
 * the result message.
 */

public abstract class AbstractEventTransformer extends AbstractTransformer
{
    protected AbstractEventTransformer()
    {
        setReturnClass(MuleMessage.class);
    }

    public MuleMessage transform(Object src, Method method) throws TransformerException
    {
        MuleMessage message = (MuleMessage)transform(src);
        message.setProperty(MuleProperties.MULE_METHOD_PROPERTY, method.getName());
        return message;
    }
}
