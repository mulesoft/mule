/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

public class TestOutboundTransformer extends AbstractTransformer
{

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        return src;
    }

}


