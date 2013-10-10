/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.transformer;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;

import java.io.IOException;

@ContainsTransformerMethods
public class MimeTypeAwareTransformer
{

    @Transformer(sourceMimeType = "text/xml", resultMimeType = "application/json")
    public String xmlToJson(String source) throws IOException
    {
        return source;
    }
}
