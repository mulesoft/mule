/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
