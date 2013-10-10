/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
