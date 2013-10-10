/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
