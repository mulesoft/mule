/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.transformer;

import org.mule.api.MuleContext;
import org.mule.api.transformer.DataType;

/**
 * Used by the transformer proxy to find or create context objects such as JAXB to be passed into a transform method
 */
public interface TransformerArgumentResolver
{
    <T> T resolve(Class<T> type, DataType source, DataType result, MuleContext context) throws Exception;
}
