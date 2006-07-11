/*
* $Id$
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
package org.mule.test.transformers;

import java.util.List;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformers.xml.JXPathExtractor;

public class JXPathExtractorTestCase extends AbstractMuleTestCase {

    protected static final String TEST_XML_MULTI_RESULTS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<root>" +
                "<node>value1</node>" +
                "<node>value2</node>" +
                "<node>value3</node>" +
            "</root>";

    protected static final String TEST_XML_SINGLE_RESULT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<root>" +
                "<node>value1</node>" +
            "</root>";

    public void testSingeResult() throws Exception {
        final JXPathExtractor extractor = new JXPathExtractor();
        final String expression = "/root/node";
        extractor.setExpression(expression);
        // just make code coverage tools happy
        assertEquals("Wrong expression returned.", expression, extractor.getExpression());
        final Object objResult = extractor.transform(TEST_XML_SINGLE_RESULT);
        assertNotNull(objResult);
        String result = (String) objResult;
        assertEquals("Wrong value extracted.", "value1", result);
    }

    public void testMultipleResults() throws Exception {
        JXPathExtractor extractor = new JXPathExtractor();
        extractor.setExpression("/root/node");
        extractor.setSingleResult(false);
        final Object objResult = extractor.transform(TEST_XML_MULTI_RESULTS);
        assertNotNull(objResult);
        List results = (List) objResult;
        assertEquals("Wrong number of results returned.", 3, results.size());
        assertEquals("Wrong value returned.", "value1", results.get(0));
        assertEquals("Wrong value returned.", "value2", results.get(1));
        assertEquals("Wrong value returned.", "value3", results.get(2));
    }

}
