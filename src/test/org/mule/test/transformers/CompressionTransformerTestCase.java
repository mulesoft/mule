/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.transformers;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.compression.CompressionHelper;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class CompressionTransformerTestCase extends AbstractTransformerTestCase
{
    private ReverseCompressionTransformer trans;
    private ReverseCompressionTransformer roundTripTrans;

    public void testNoCompressionOnTransformer() throws Exception
    {
        trans.setDoCompression(false);
        assertTrue(!trans.getDoCompression());
        String result = (String) trans.transform(getTestData());
        assertEquals(getTestData().toString().length(), result.length());
        assertEquals(getResultData(), result);
    }

    public void testCompressionOnTransformer() throws Exception
    {
        trans.setDoCompression(true);
        assertTrue(trans.getDoCompression());
        String result = (String) trans.transform(getTestData(), true);
        assertTrue(getTestData().toString().length() > result.length());
        String result2 = new String(CompressionHelper.uncompressByteArray(result.getBytes()));

        assertEquals(getTestData().toString().length(), result2.length());
        assertEquals(getResultData(), result2); 
        
        //transform back
        String result3 = (String) trans.transform(result);

        assertEquals(getTestData(), result3);
    }


    /* (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#getResultData()
     */
    public Object getResultData()
    {
        return new StringBuffer(getTestData().toString()).reverse().toString();
    }

    /* (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#getTestData()
     */
    public Object getTestData()
    {
        return "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";
    }

    /* (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#getTransformers()
     */
    public UMOTransformer getTransformer()
    {
        return trans;
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        trans = new ReverseCompressionTransformer();
        roundTripTrans = new ReverseCompressionTransformer();
    }

    /* (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#getRoundTripTransformer()
     */
    public UMOTransformer getRoundTripTransformer()
    {
        return roundTripTrans;
    }
}
