/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class TransformerChainingTestCase extends AbstractMuleTestCase
{

    public UMOTransformer getTransformer() throws Exception
    {
        AbstractTransformer transformer = new AbstractTransformer()
        {
            protected Object doTransform(final Object src, final String encoding) throws TransformerException
            {
                return src;
            }
        };
        transformer.setName("root");
        transformer.setReturnClass(this.getClass());
        transformer.registerSourceType(String.class);
        transformer.initialise();

        return transformer;
    }

    public void testIgnoreBadInputDoesNotBreakChain() throws Exception
    {
        // Grrrr....
        AbstractTransformer transformer = (AbstractTransformer) this.getTransformer();
        assertNotNull(transformer);

        transformer.setIgnoreBadInput(true);
        final AtomicBoolean nextCalled = new AtomicBoolean(false);
        final Object marker = new Object();
        transformer.setNextTransformer(new AbstractTransformer()
        {

            protected Object doTransform(Object src, String encoding) throws TransformerException
            {
                nextCalled.set(true);
                return marker;
            }
        });

        Object result = transformer.transform(this);
        assertNotNull(result);
        assertSame(marker, result);
        assertTrue("Next transformer not called.", nextCalled.get());
    }

}