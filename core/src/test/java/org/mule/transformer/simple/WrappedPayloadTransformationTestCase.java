/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.TransformerException;

public class WrappedPayloadTransformationTestCase extends HexStringByteArrayTransformersTestCase
{

    // extra test for MULE-1274: transforming Mule Messages with regular payload
    public void testPayloadWrappedInMuleMessage() throws TransformerException
    {
        Object wrappedPayload = new DefaultMuleMessage(this.getResultData());
        assertEquals(this.getTestData(), this.getRoundTripTransformer().transform(wrappedPayload));
    }

}
