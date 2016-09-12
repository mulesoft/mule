/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-10083:  The XMLUnit assertion at the end of this tests requires to instantiate a javax.xml.transform.TransformerFactory "
    + "class, that class is found using SPI and results in org.apache.xalan.processor.TransformerFactoryImpl from Xalan."
    + "Test runner classifies Xalan as an app dependency, so Xalan jar will be in the app's classloader. "
    + "The problem is that CXF module, from the container, exports org.apache.xalan package, making it parent only."
    + "This tests can't run if Xalan is not properly placed in the container classloader")
public class XQueryFunctionalTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/xml/xquery-functional-test.xml";
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
    final Event muleEvent = flowRunner("Echo").withPayload(srcData).withInboundProperties(props).run();

    InternalMessage message = muleEvent.getMessage();
    assertNotNull(message);
    assertThat(muleEvent.getError().isPresent(), is(false));
    // Compare results
    assertTrue(XMLUnit.compareXML(getPayloadAsString(message), resultData).similar());
  }
}
