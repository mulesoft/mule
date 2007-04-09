/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.impl.MuleMessage;
import org.mule.umo.transformer.TransformerException;

public class WrappedPayloadTransformationTestCase extends HexStringByteArrayTransformersTestCase
{

    // extra test for MULE-1274: transforming UMOMessages with regular payload
    public void testPayloadWrappedInUMOMessage() throws TransformerException
    {
        Object wrappedPayload = new MuleMessage(this.getResultData());
        assertEquals(this.getTestData(), this.getRoundTripTransformer().transform(wrappedPayload));
    }

}
