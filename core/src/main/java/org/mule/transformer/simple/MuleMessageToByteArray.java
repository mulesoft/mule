/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.SerializationUtils;

/** TODO */
public class MuleMessageToByteArray extends AbstractMessageTransformer
{
    public MuleMessageToByteArray()
    {
        registerSourceType(DataTypeFactory.MULE_MESSAGE);
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding)
    {
        return SerializationUtils.serialize(message);
    }
}
