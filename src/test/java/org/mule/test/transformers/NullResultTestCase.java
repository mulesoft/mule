/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.providers.NullPayload;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class NullResultTestCase extends AbstractTransformerTestCase {

    private NullResultTransformer transformer = new NullResultTransformer();

    public Object getTestData() {
        return new Object();
    }

    public Object getResultData() {
        return new NullPayload();
    }

    public UMOTransformer getTransformer() throws Exception {
        return transformer;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception {
        return null;
    }

    public void testNullNotExpected() throws Exception {
        transformer.setReturnClass(String.class);
        try {
            testTransform();
            fail("Transformer should have thrown an exception because the return class doesn't match the result.");
        }
        catch (TransformerException e) {
            // expected
        }
    }

    public final class NullResultTransformer extends AbstractTransformer {

        public NullResultTransformer() {
            registerSourceType(Object.class);
            setReturnClass(NullPayload.class);
        }

        public Object doTransform(Object src, String encoding) throws TransformerException {
            return null;
        }
    }
}
