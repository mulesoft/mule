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
*
*/
package org.mule.test.transformers.codec;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.codec.SgmlEntityDecoder;
import org.mule.transformers.codec.SgmlEntityEncoder;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SgmlTransformersTestCase  extends AbstractTransformerTestCase
{
    /*
     * (non-Javadoc)
     *
     * @see org.mule.tck.AbstractTransformerTestCase#getResultData()
     */
    public Object getResultData()
    {
        return "&lt;test&gt;blah&lt;/test&gt;";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.tck.AbstractTransformerTestCase#getTestData()
     */
    public Object getTestData()
    {
        return "<test>blah</test>";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.tck.AbstractTransformerTestCase#getTransformers()
     */
    public UMOTransformer getTransformer()
    {
        return new SgmlEntityEncoder();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.tck.AbstractTransformerTestCase#getRoundTripTransformer()
     */
    public UMOTransformer getRoundTripTransformer()
    {
        return new SgmlEntityDecoder();
    }

}
