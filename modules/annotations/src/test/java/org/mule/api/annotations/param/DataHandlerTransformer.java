/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;

/**
 * TODO
 */
@ContainsTransformerMethods
public class DataHandlerTransformer
{
    @Transformer
    public String dataHandlerToObject(DataHandler source) throws IOException
    {
        if(source.getContent() instanceof InputStream)
        {
            return IOUtils.toString((InputStream)source.getContent());
        }
        else
        {
            return (String)source.getContent();
        }
    }
}
