/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring.events;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

/**
 * <code>StringToOrder</code> converts a String representation of an Order to an
 * Order object
 */
public class StringToOrder extends AbstractTransformer
{

    @Override
    public Object doTransform(Object src, String enc) throws TransformerException
    {
        return null;
    }

}
