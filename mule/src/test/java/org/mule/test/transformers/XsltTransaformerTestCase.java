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
 */
package org.mule.test.transformers;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.xml.XsltTransformer;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.Utility;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class XsltTransaformerTestCase extends AbstractTransformerTestCase {

    private String srcData;
    private String resultData;

    protected void setUp() throws Exception
    {
        super.setUp();
        srcData = Utility.fileToString("src/test/conf/cdcatalog.xml");
        resultData = Utility.fileToString("src/test/conf/cdcatalog.html");
    }

    public UMOTransformer getTransformer() throws Exception
    {
        XsltTransformer transformer = new XsltTransformer();
		transformer.setXslFile("src/test/conf/cdcatalog.xsl");
		transformer.initialise();
        return transformer;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        throw new UnsupportedOperationException("Round trip not implemented");
    }

    public Object getTestData()
    {
        return srcData;
    }

    public Object getResultData()
    {
        return resultData;
    }

    public void testRoundTripSessionTransform() throws Exception
    {
        //noop
    }

    public void testRoundTripBadSessionCalls() throws Exception
    {
        //noop
    }

    public void testRoundtripBadReturnType() throws Exception
    {
        //noop
    }

    public void testRoundTrip() throws Exception
    {
        //noop
    }

    public void testRoundtripAutoCommitTransform() throws Exception
    {
        //noop
    }

    public boolean compareResults(Object src, Object result) {
        if(src!=null) {
            src = ((String)src).replaceAll("\r", "");
        }
        return super.compareResults(src, result);
    }
}