/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.IOUtils;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.Document;

public class DomXmlTransformersTestCase extends AbstractXmlTransformerTestCase
{

    private String srcData;
    private Document resultData;

    // @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.getResourceAsString("cdcatalog.xml", getClass());
        org.dom4j.Document dom4jDoc = DocumentHelper.parseText(srcData);
        resultData = new DOMWriter().write(dom4jDoc);
    }

    public UMOTransformer getTransformer() throws Exception
    {
        return new XmlToDomDocument();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new DomDocumentToXml();
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
