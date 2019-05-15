/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.DomDocumentToXml;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.transformer.types.DataTypeFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class AbstractDomXmlTransformersTestCase extends AbstractXmlTransformerTestCase
{

  protected String srcData;
  protected Document resultData;

  public Transformer getTransformer() throws Exception
  {
    XmlToDomDocument trans = createObject(XmlToDomDocument.class);
    trans.setReturnDataType(DataTypeFactory.create(org.w3c.dom.Document.class));
    return trans;
  }

  public Transformer getRoundTripTransformer() throws Exception
  {
    DomDocumentToXml trans = createObject(DomDocumentToXml.class);
    trans.setReturnDataType(DataTypeFactory.STRING);
    return trans;
  }

  static void writeXml(Node n) throws TransformerException {
    TransformerFactory tf = TransformerFactory.newInstance();
    javax.xml.transform.Transformer t = tf.newTransformer();
    t.setOutputProperty(OutputKeys.INDENT, "yes");
    t.transform(new DOMSource(n), new StreamResult(System.out));
  }

  public Object getTestData()
  {
    return srcData;
  }

  public Object getResultData()
  {
    return resultData;
  }

  void setTestData(String srcData) {
    this.srcData = srcData;
  }

  public void setResultData(Document resultData) {
    this.resultData = resultData;
  }
}
