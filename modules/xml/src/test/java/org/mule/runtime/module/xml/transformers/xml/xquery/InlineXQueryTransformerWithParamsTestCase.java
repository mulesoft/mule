/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml.xquery;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.transformer.AbstractTransformerTestCase;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.transformer.XQueryTransformer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;

public class InlineXQueryTransformerWithParamsTestCase extends AbstractTransformerTestCase {

  private String srcData;
  private String resultData;

  @Override
  protected void doSetUp() throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    srcData = IOUtils.getResourceAsString("cd-catalog.xml", getClass());
    resultData = IOUtils.getResourceAsString("cd-catalog-result-with-params.xml", getClass());
  }

  @Override
  public Transformer getTransformer() throws Exception {
    XQueryTransformer transformer = new XQueryTransformer();
    transformer.setXquery("declare variable $document external;\n" + "declare variable $title external;\n"
        + "declare variable $rating external;\n" + " <cd-listings title='{$title}' rating='{$rating}'>\n" + "{\n"
        + "    for $cd in $document/catalog/cd\n" + "    return <cd-title>{data($cd/title)}</cd-title>\n" + "} \n</cd-listings>");
    transformer.setReturnDataType(DataType.STRING);

    Map<String, Object> params = new HashMap<>();
    params.put("title", "#[message.outboundProperties.ListTitle]");
    params.put("rating", "#[message.outboundProperties.ListRating]");
    transformer.setContextProperties(params);

    transformer.setMuleContext(muleContext);
    transformer.initialise();
    return transformer;
  }


  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return null;
  }

  @Override
  public void testRoundtripTransform() throws Exception {
    // disable this test
  }

  @Override
  public Object getTestData() {
    Map<String, Serializable> props = new HashMap<>(2);
    props.put("ListTitle", "MyList");
    props.put("ListRating", new Integer(6));
    return MuleMessage.builder().payload(srcData).outboundProperties(props).build();
  }

  @Override
  public Object getResultData() {
    return resultData;
  }

  @Override
  public boolean compareResults(Object expected, Object result) {
    if (expected instanceof Document && result instanceof Document) {
      return XMLUnit.compareXML((Document) expected, (Document) result).similar();
    } else if (expected instanceof String && result instanceof String) {
      try {
        String expectedString = this.normalizeString((String) expected);
        String resultString = this.normalizeString((String) result);
        return XMLUnit.compareXML(expectedString, resultString).similar();
      } catch (Exception ex) {
        return false;
      }
    }

    // all other comparisons are passed up
    return super.compareResults(expected, result);
  }
}
