/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.transformer.AbstractMessageTransformer;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.DataTypeFactory;

/**
 * A transformer that uses the transform discovery mechanism to convert the message payload. This transformer
 * works much better when transforming custom object types rather that java types since there is less chance for
 * ambiguity.
 * If an exact match cannot be made an execption will be thrown.
 */
public class AutoTransformer extends AbstractMessageTransformer
{
    /**
     * Template method where deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *
     */
    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if(getReturnClass().equals(Object.class))
        {
            throw new InitialisationException(CoreMessages.transformerInvalidReturnType(Object.class, getName()), this);
        }
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        return message.getPayload(DataTypeFactory.create(getReturnClass()));
    }
}
