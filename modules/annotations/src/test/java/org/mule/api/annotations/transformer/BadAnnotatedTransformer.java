/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.transformer;

import org.mule.api.annotations.Transformer;

/**
 * Custom transformers cannot have an object source type
 */
public class BadAnnotatedTransformer
{
    @Transformer(sourceTypes = Object.class)
    public String transform(StringBuffer object)
    {
        return object.toString();
    }
}
