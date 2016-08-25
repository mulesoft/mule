/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.IOUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

public class XSLTWikiDocsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/xml/xslt-functional-test-flow.xml";
  }

  @Test
  public void testMessageTransform() throws Exception {
    // We're using Xml Unit to compare results
    // Ignore whitespace and comments
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);

    // Read in src and result data
    String srcData = IOUtils.getResourceAsString("org/mule/test/integration/xml/cd-catalog.xml", getClass());
    String resultData =
        IOUtils.getResourceAsString("org/mule/test/integration/xml/cd-catalog-result-with-params.xml", getClass());

    // These are the message properties that will get passed into the XQuery context
    Map<String, Serializable> props = new HashMap<>();
    props.put("ListTitle", "MyList");
    props.put("ListRating", new Integer(6));

    // Invoke the service
    final MuleEvent muleEvent = flowRunner("Echo").withPayload(srcData).withInboundProperties(props).run();
    MuleMessage message = muleEvent.getMessage();
    assertNotNull(message);
    assertNull(muleEvent.getError());
    // Compare results

    String result = getPayloadAsString(message);
    // remove namespace information so that it doesn't interferes with XMLUnit test
    result = result.replaceAll("xmlns=\"http://www.mulesoft.org/schema/mule/core\"", "");

    assertTrue(XMLUnit.compareXML(result, resultData).similar());
  }
}
