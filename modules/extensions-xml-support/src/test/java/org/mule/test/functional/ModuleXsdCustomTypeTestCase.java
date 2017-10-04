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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import com.google.common.collect.ImmutableMap;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;

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

  //the order matters when describing a , and ImmutableMap guarantees it
  private static final ImmutableMap<String, Object> USER_DATA = ImmutableMap.<String, Object>builder()
      .put("name", "somename")
      .put("kind", "somekind")
      .put("weight", 100)
      .put("email", "somename@domain.com")
      .put("userId", "somename-id")
      .build();
  private static final Map<String, Object> EXPECTED_XSDTYPE_1 = ImmutableMap.of("User", USER_DATA);

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
    final CoreEvent muleEvent = flowRunner("testIsXsdType1FromPayloadFlow").withPayload(EXPECTED_XSDTYPE_1).run();
    compareXML((String) muleEvent.getMessage().getPayload().getValue(), XML_TYPE1_SAMPLE);
  }

  @Test
  public void testSendingXsdType1FromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsXsdType1FromExpressionFlow").run();
    compareXML((String) muleEvent.getMessage().getPayload().getValue(), XML_TYPE1_SAMPLE);
  }

  @Test
  public void testIsXsdType1WithNamespaceFromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsXsdType1WithNamespaceFromExpressionFlow").run();
    compareXML((String) muleEvent.getMessage().getPayload().getValue(), XML_TYPE1_SAMPLE_WITH_NAMESPACE);
  }

  @Test
  public void testHardcodedXsdType1Flow() throws Exception {
    final CoreEvent muleEvent = flowRunner("testHardcodedXsdType1Flow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    for (Map.Entry<String, Object> entry : EXPECTED_XSDTYPE_1.entrySet()) {
      assertThat((Map<String, Object>) muleEvent.getMessage().getPayload().getValue(),
                 hasEntry(entry.getKey(), entry.getValue()));
    }
  }

  @Test
  public void testHardcodedXsdType1AndExtractFieldsInVarsFlow() throws Exception {
    final CoreEvent muleEvent = flowRunner("testHardcodedXsdType1AndExtractFieldsInVarsFlow").run();
    for (Map.Entry<String, Object> entry : USER_DATA.entrySet()) {
      assertThat(muleEvent.getVariables().containsKey("extracted-user-" + entry.getKey()), is(true));
      assertThat(muleEvent.getVariables().get("extracted-user-" + entry.getKey()).getValue(), is(entry.getValue()));
    }
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
