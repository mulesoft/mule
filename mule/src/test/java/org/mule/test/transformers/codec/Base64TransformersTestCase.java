/*
 * $Header:
 * /cvsroot/mule/mule/src/test/org/mule/test/transformers/DefaultTransformersTestCase.java,v
 * 1.3 2003/11/26 06:45:02 rossmason Exp $ $Revision$ $Date: 2003/11/26
 * 06:45:02 $
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.transformers.codec;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.codec.Base64Decoder;
import org.mule.transformers.codec.Base64Encoder;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.Base64;

/**
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Base64TransformersTestCase extends AbstractTransformerTestCase
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getResultData()
     */
    public Object getResultData()
    {
        try {
            return Base64.encodeBytes(getTestData().toString().getBytes());
        }
        catch (Exception ex) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTestData()
     */
    public Object getTestData()
    {
        return "the quick brown fox jumped over the lazy dog";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTransformers()
     */
    public UMOTransformer getTransformer()
    {
        return new Base64Encoder();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getRoundTripTransformer()
     */
    public UMOTransformer getRoundTripTransformer()
    {
        UMOTransformer t = new Base64Decoder();
        // our input is a String so we expect a String as output
        t.setReturnClass(this.getTestData().getClass());
        return t;
    }

}
