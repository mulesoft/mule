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
import org.mule.transformers.MultiTransformerSession;
import org.mule.umo.transformer.TransformerException;

/**
 * <p/>
 * <code>MultiTransformerTestCase</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MultiTransformerSessionTestCase extends TestCase
{
    private static final String TEST_DATA = "the quick brown fox jumped over the lazy dog";

    private ReverseCompressionTransformer trans;

    public void testBeginCommitBehaviour() throws Exception
    {
        MultiTransformerSession mts = new MultiTransformerSession();
        assertTrue(!mts.isInSession());

        mts.begin();
        assertTrue(mts.isInSession());
        Object result = mts.transform(trans, TEST_DATA);
        assertTrue(mts.isInSession());
        assertEquals(TEST_DATA, new StringBuffer(result.toString()).reverse().toString());
        result = mts.transform(trans, result);
        assertEquals(TEST_DATA, result.toString());

        assertEquals(3, mts.getStackSize());
        assertEquals(TEST_DATA, mts.getFromStack(0));
        assertEquals(new StringBuffer(TEST_DATA).reverse().toString(), mts.getFromStack(1));
        assertEquals(TEST_DATA, mts.getFromStack(2));

        result = mts.transform(trans, TEST_DATA);
        assertEquals(4, mts.getStackSize());
        assertEquals(result, mts.getData());
        assertEquals(new StringBuffer(TEST_DATA).reverse().toString(), mts.getData());

        mts.commit();
        assertEquals(0, mts.getStackSize());
        assertEquals(result, mts.getData());
        assertTrue(!mts.isInSession());
    }

    public void testRollbackBehaviour() throws Exception
    {
        MultiTransformerSession mts = new MultiTransformerSession();
        assertTrue(!mts.isInSession());

        //Test default session start on transform
        Object result = mts.transform(trans, TEST_DATA);
        assertTrue(mts.isInSession());
        assertEquals(TEST_DATA, new StringBuffer(result.toString()).reverse().toString());
        result = mts.transform(trans, result);
        assertEquals(TEST_DATA, result.toString());

        assertEquals(3, mts.getStackSize());
        assertEquals(TEST_DATA, mts.getFromStack(0));
        assertEquals(new StringBuffer(TEST_DATA).reverse().toString(), mts.getFromStack(1));
        assertEquals(TEST_DATA, mts.getFromStack(2));

        result = mts.transform(trans, TEST_DATA);
        assertEquals(4, mts.getStackSize());
        assertEquals(result, mts.getData());
        assertEquals(new StringBuffer(TEST_DATA).reverse().toString(), mts.getData());

        mts.rollback();
        assertEquals(0, mts.getStackSize());
        assertEquals(TEST_DATA, mts.getData());
        assertTrue(!mts.isInSession());
    }

    public void testBadSessionCalls() throws Exception
    {

        MultiTransformerSession mts = new MultiTransformerSession();

        mts.begin();
        assertTrue(mts.isInSession());
        try
        {
            mts.begin();
            fail("Should throw exception session is already in a transaction");
        }
        catch (TransformerException e)
        {
            // expected
        }

        mts.rollback();
        try
        {
            mts.rollback();
            fail("Transformer should not be in a session");
        }
        catch (TransformerException e1)
        {
            // expected
        }
        try
        {
            mts.commit();
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
        trans = new ReverseCompressionTransformer();
    }

}
