/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.datatype;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.nio.charset.Charset;

/**
 * Converts strings to {@link Charset} instances. See {@link DataTypeBuilder#charset(String)}
 */
public class StringToCharsetTransformer extends AbstractTransformer
{

    public StringToCharsetTransformer()
    {
        this.registerSourceType(DataType.STRING);
        this.setReturnDataType(DataType.builder().type(Charset.class).build());
    }

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException
    {
        try
        {
            return DataType.builder().charset((String) src).build().getMediaType().getCharset().get();
        }
        catch (Exception e)
        {
            throw new TransformerException(createStaticMessage("Exception transforming to Charset."), e);
        }
    }

}
