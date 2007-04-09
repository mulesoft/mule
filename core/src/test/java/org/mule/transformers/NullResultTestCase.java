/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers;

import org.mule.providers.NullPayload;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

public class NullResultTestCase extends AbstractTransformerTestCase
{

    private final NullResultTransformer transformer = new NullResultTransformer();

    public Object getTestData()
    {
        return new Object();
    }

    public Object getResultData()
    {
        return NullPayload.getInstance();
    }

    public UMOTransformer getTransformer() throws Exception
    {
        return transformer;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    public void testNullNotExpected() throws Exception
    {
        transformer.setReturnClass(String.class);
        try
        {
            testTransform();
            fail("Transformer should have thrown an exception because the return class doesn't match the result.");
        }
        catch (TransformerException e)
        {
            // expected
        }
    }

    public static final class NullResultTransformer extends AbstractTransformer
    {
        public NullResultTransformer()
        {
            super();
            this.registerSourceType(Object.class);
            this.setReturnClass(NullPayload.class);
        }

        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            return null;
        }
    }

}
