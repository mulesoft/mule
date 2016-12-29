/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.introspection;

import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.mule.runtime.core.util.IOUtils;

public class WsdlSchemasCollectorTestCase {

  @Test
  public void wsdlWithEmbeddedTypeaSchema() throws Exception {
    URL wsdl = currentThread().getContextClassLoader().getResource("wsdl/simple-service.wsdl");
    WsdlIntrospecter introspecter = new WsdlIntrospecter(wsdl.getPath(), "TestService", "TestPort");
    Map<String, InputStream> schemas = introspecter.getSchemas().collect();
    assertThat(schemas.size(), is(1));

    URL expectedXsd = currentThread().getContextClassLoader().getResource("schemas/simple-service-types.xsd");
    String expected = IOUtils.toString(expectedXsd.openStream());
    String result = IOUtils.toString(schemas.entrySet().iterator().next().getValue());
    XMLUnit.setIgnoreWhitespace(true);
    assertSimilarXml(expected, result);
  }
}
