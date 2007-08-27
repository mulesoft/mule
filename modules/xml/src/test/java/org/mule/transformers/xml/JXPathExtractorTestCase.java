/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.tck.AbstractMuleTestCase;

import java.util.List;

public class JXPathExtractorTestCase extends AbstractMuleTestCase
{

    protected static final String TEST_XML_MULTI_RESULTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                                           + "<root>" + "<node>value1</node>"
                                                           + "<node>value2</node>" + "<node>value3</node>"
                                                           + "</root>";

    protected static final String TEST_XML_MULTI_NESTED_RESULTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                                                  + "<root>"
                                                                  + "<node>"
                                                                  + "<subnode1>val1</subnode1>"
                                                                  + "<subnode2>val2</subnode2>"
                                                                  + "</node>"
                                                                  + "<node>"
                                                                  + "<subnode1>val3</subnode1>"
                                                                  + "<subnode2>val4</subnode2>"
                                                                  + "</node>"
                                                                  + "</root>";

    protected static final String TEST_XML_SINGLE_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                                           + "<root>" + "<node>value1</node>" + "</root>";

    public void testSingeResult() throws Exception
    {
        final JXPathExtractor extractor = new JXPathExtractor();
        final String expression = "/root/node";
        extractor.setExpression(expression);
        // just make code coverage tools happy
        assertEquals("Wrong expression returned.", expression, extractor.getExpression());
        final Object objResult = extractor.transform(TEST_XML_SINGLE_RESULT);
        assertNotNull(objResult);
        String result = (String)objResult;
        assertEquals("Wrong value extracted.", "value1", result);
    }

    public void testMultipleResults() throws Exception
    {
        JXPathExtractor extractor = new JXPathExtractor();
        extractor.setExpression("/root/node");
        extractor.setSingleResult(false);
        final Object objResult = extractor.transform(TEST_XML_MULTI_RESULTS);
        assertNotNull(objResult);
        List results = (List)objResult;
        assertEquals("Wrong number of results returned.", 3, results.size());
        assertEquals("Wrong value returned.", "value1", results.get(0));
        assertEquals("Wrong value returned.", "value2", results.get(1));
        assertEquals("Wrong value returned.", "value3", results.get(2));
    }

    /**
     * This xpath expression will internally have DefaultText returned, test there
     * are no ClassCastExceptions.
     */
    public void testMultipleResultsNested() throws Exception
    {
        JXPathExtractor extractor = new JXPathExtractor();
        extractor.setExpression("/root/node[*]/*/text()");
        extractor.setSingleResult(false);
        final Object objResult = extractor.transform(TEST_XML_MULTI_NESTED_RESULTS);
        assertNotNull(objResult);
        List results = (List)objResult;
        assertEquals("Wrong number of results returned.", 4, results.size());
        assertEquals("Wrong value returned.", "val1", results.get(0));
        assertEquals("Wrong value returned.", "val2", results.get(1));
        assertEquals("Wrong value returned.", "val3", results.get(2));
        assertEquals("Wrong value returned.", "val4", results.get(3));
    }

}
