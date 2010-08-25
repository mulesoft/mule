/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

import org.apache.commons.lang.SerializationUtils;

/** TODO */
public class MuleMessageToByteArray extends AbstractMessageTransformer
{
    public MuleMessageToByteArray()
    {
        registerSourceType(MuleMessage.class);
        setReturnDataType(DataTypeFactory.create(byte[].class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding)
    {
        return SerializationUtils.serialize(message);
    }
}
