/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.transformers;

import junit.framework.TestCase;
import org.mule.transformers.SingleTransformerSession;
import org.mule.transformers.codec.Base64Encoder;
import org.mule.transformers.codec.Base64Decoder;
import org.mule.umo.transformer.TransformerException;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SingleTransformerSessionTestCase extends TestCase
{
    private static final String TEST_DATA = "the quick brown fox jumped over the lazy dog";

    private Base64Encoder trans;
    private Base64Decoder roundtripTrans;

    public void testBeginCommitBehaviour() throws Exception
    {
        SingleTransformerSession sts = new SingleTransformerSession(trans);
        assertTrue(!sts.isInSession());

        sts.begin();
        assertTrue(sts.isInSession());
        Object result = sts.transform(TEST_DATA);
        assertTrue(sts.isInSession());
        assertEquals(TEST_DATA, new String(roundtripTrans.transform(result).toString()));

        sts.rollback();
        assertEquals(TEST_DATA, sts.getData());
        assertNotNull(sts.getTransformer());

        result = sts.transform(TEST_DATA);
        assertTrue(sts.isInSession());
        assertEquals(TEST_DATA, new String(roundtripTrans.transform(result).toString()));

        sts.commit();
        assertEquals(result, sts.getData());
        assertTrue(!sts.isInSession());
    }

    public void testBadSessionCalls() throws Exception
    {

        SingleTransformerSession sts = new SingleTransformerSession(trans);

        sts.begin();
        assertTrue(sts.isInSession());
        try
        {
            sts.begin();
            fail("Should throw exception session is already in a transaction");
        }
        catch (TransformerException e)
        {
            // expected
        }

        sts.rollback();
        try
        {
            sts.rollback();
            fail("Transformer should not be in a session");
        }
        catch (TransformerException e1)
        {
            // expected
        }
        try
        {
            sts.commit();
            fail("Transformer should not be in a session");
        }
        catch (TransformerException e2)
        {
            // expected
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
    protected void setUp() throws Exception
    {
        trans = new Base64Encoder();
        roundtripTrans = new Base64Decoder();
    }

}
