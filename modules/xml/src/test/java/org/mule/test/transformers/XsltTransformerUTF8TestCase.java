/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers;

import org.mule.transformers.xml.XsltTransformer;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.IOUtils;

public class XsltTransformerUTF8TestCase extends AbstractXmlTransformerTestCase
{

    private String srcData;
    private String resultData;

    // @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-utf-8.xml", getClass()), "UTF-8");
        resultData = IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-utf-8.html", getClass()),
            "UTF-8");
    }

    public UMOTransformer getTransformer() throws Exception
    {
        XsltTransformer transformer = new XsltTransformer();
        transformer.setXslFile("cdcatalog.xsl");
        transformer.initialise();
        return transformer;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    // @Override
    public void testRoundtripTransform() throws Exception
    {
        // disable this test
    }

    public Object getTestData()
    {
        return srcData;
    }

    public Object getResultData()
    {
        return resultData;
    }

}
