/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.transformer;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;

/**
 * Custom transformers cannot have an object source type
 */
@ContainsTransformerMethods
public class BadAnnotatedTransformer
{
    @Transformer(sourceTypes = Object.class)
    public String transform(StringBuffer object)
    {
        return object.toString();
    }
}
