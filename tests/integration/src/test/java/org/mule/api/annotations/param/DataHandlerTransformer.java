/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;

import java.io.IOException;

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
        return (String)source.getContent();
    }
}
