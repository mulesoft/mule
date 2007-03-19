/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring.events;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>StringToOrder</code> converts a String representation of an Order to an
 * Order object
 */
public class StringToOrder extends AbstractTransformer
{

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        return null;
    }

}
