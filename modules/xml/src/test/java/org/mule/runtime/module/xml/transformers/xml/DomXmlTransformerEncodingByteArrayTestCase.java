/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.transformer.XmlToDomDocument;

import java.io.InputStream;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;

public class DomXmlTransformerEncodingByteArrayTestCase extends DomXmlTransformerEncodingTestCase {

  private byte[] srcData; // Parsed XML doc
  private String resultData; // String as US-ASCII

  @Override
  protected void doSetUp() throws Exception {
    InputStream resourceStream = IOUtils.getResourceAsStream("cdcatalog-utf-8.xml", getClass());
    srcData = IOUtils.toString(resourceStream, "UTF-8").getBytes("UTF-8");

    resourceStream = IOUtils.getResourceAsStream("cdcatalog-us-ascii.xml", getClass());
    resultData = IOUtils.toString(resourceStream, "US-ASCII");
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    XmlToDomDocument trans = createObject(XmlToDomDocument.class); // encoding is not interesting
    trans.setReturnDataType(DataType.BYTE_ARRAY);
    return trans;
  }

  @Override
  public Object getTestData() {
    return srcData;
  }

  @Override
  public Object getResultData() {
    return resultData;
  }

  @Override
  public boolean compareResults(Object expected, Object result) {
    try {
      // This is only used during roundtrip test, so it will always be byte[] instances
      if (expected instanceof byte[]) {
        org.dom4j.Document dom4jDoc = null;
        dom4jDoc = DocumentHelper.parseText(new String((byte[]) expected, "UTF-8"));
        expected = new DOMWriter().write(dom4jDoc);
        dom4jDoc = DocumentHelper.parseText(new String((byte[]) result, "UTF-8"));
        result = new DOMWriter().write(dom4jDoc);
      }
    } catch (Exception ex) {
      // ignored.
    }

    return super.compareResults(expected, result);
  }
}
