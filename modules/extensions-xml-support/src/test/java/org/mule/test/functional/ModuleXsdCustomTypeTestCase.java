/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.custommonkey.xmlunit.XMLUnit.setIgnoreAttributeOrder;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreComments;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreWhitespace;
import static org.custommonkey.xmlunit.XMLUnit.setNormalizeWhitespace;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.mule.runtime.core.api.Event;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModuleXsdCustomTypeTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String XML_TYPE1_SAMPLE_WITH_NAMESPACE = "<?xml version='1.0' encoding='US-ASCII'?>"
      + "<val:User xmlns:val=\"http://validationnamespace.raml.org\">"
      + "  <val:name>somename</val:name>"
      + "  <val:kind>somekind</val:kind>"
      + "  <val:weight>100</val:weight>"
      + "  <val:email>somename@domain.com</val:email>"
      + "  <val:userId>somename-id</val:userId>"
      + "</val:User>";

  private static final String XML_TYPE1_SAMPLE = "<?xml version='1.0' encoding='US-ASCII'?>"
      + "<User>"
      + "  <name>somename</name>"
      + "  <kind>somekind</kind>"
      + "  <weight>100</weight>"
      + "  <email>somename@domain.com</email>"
      + "  <userId>somename-id</userId>"
      + "</User>";

  @Override
  protected String getModulePath() {
    return "modules/module-xsd-custom-types.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-xsd-custom-types.xml";
  }

  @Test
  public void testSendingXsdType1FromMap() throws Exception {
    //the order matters, that's why we need a sorted Map to describe the User
    final LinkedHashMap<String, Object> user = new LinkedHashMap<>();
    user.put("name", "somename");
    user.put("kind", "somekind");
    user.put("weight", 100);
    user.put("email", "somename@domain.com");
    user.put("userId", "somename-id");
    final Map<String, Object> payload = new HashMap<>();
    payload.put("User", user);
    final Event muleEvent = flowRunner("testIsXsdType1FromPayloadFlow").withPayload(payload).run();
    compareXML((String) muleEvent.getMessage().getPayload().getValue(), XML_TYPE1_SAMPLE);
  }

  @Test
  public void testSendingXsdType1FromExpression() throws Exception {
    final Event muleEvent = flowRunner("testIsXsdType1FromExpressionFlow").run();
    compareXML((String) muleEvent.getMessage().getPayload().getValue(), XML_TYPE1_SAMPLE);
  }

  @Test
  public void testIsXsdType1WithNamespaceFromExpression() throws Exception {
    final Event muleEvent = flowRunner("testIsXsdType1WithNamespaceFromExpressionFlow").run();
    compareXML((String) muleEvent.getMessage().getPayload().getValue(), XML_TYPE1_SAMPLE_WITH_NAMESPACE);
  }

  private void compareXML(String expected, String actual) throws Exception {
    setNormalizeWhitespace(true);
    setIgnoreWhitespace(true);
    setIgnoreComments(true);
    setIgnoreAttributeOrder(false);

    Diff diff = XMLUnit.compareXML(expected, actual);
    if (!(diff.similar() && diff.identical())) {
      DetailedDiff detDiff = new DetailedDiff(diff);
      @SuppressWarnings("rawtypes")
      List differences = detDiff.getAllDifferences();
      StringBuilder diffLines = new StringBuilder();
      for (Object object : differences) {
        Difference difference = (Difference) object;
        diffLines.append(difference.toString() + '\n');
      }
      throw new IllegalArgumentException("Actual XML differs from expected: \n" + diffLines.toString());
    }
  }

}
