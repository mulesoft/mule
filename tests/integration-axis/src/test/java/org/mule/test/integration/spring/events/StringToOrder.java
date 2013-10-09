/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
