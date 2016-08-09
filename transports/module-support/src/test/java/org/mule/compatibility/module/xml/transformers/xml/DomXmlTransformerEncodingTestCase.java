/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.xml.transformers.xml;

import static java.nio.charset.StandardCharsets.US_ASCII;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.transformer.EndpointAwareTransformer;
import org.mule.compatibility.core.config.builders.TransportsConfigurationBuilder;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.transport.service.DefaultEndpointAwareTransformer;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.module.xml.transformer.DomDocumentToXml;
import org.mule.runtime.module.xml.transformer.XmlToDomDocument;
import org.mule.runtime.module.xml.transformers.xml.AbstractXmlTransformerTestCase;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.Document;

public class DomXmlTransformerEncodingTestCase extends AbstractXmlTransformerTestCase {

  private Document srcData; // Parsed XML doc
  private String resultData; // String as US-ASCII

  @Override
  protected void doSetUp() throws Exception {
    org.dom4j.Document dom4jDoc =
        DocumentHelper.parseText(IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-utf-8.xml", getClass()), "UTF-8"));
    srcData = new DOMWriter().write(dom4jDoc);
    resultData = IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-us-ascii.xml", getClass()), "US-ASCII");
  }

  @Override
  public Transformer getTransformer() throws Exception {
    EndpointAwareTransformer trans =
        new DefaultEndpointAwareTransformer(createObject(DomDocumentToXml.class), SystemUtils.getDefaultEncoding(muleContext));
    trans.setReturnDataType(DataType.STRING);

    EndpointBuilder builder = new EndpointURIEndpointBuilder("test://test", muleContext);
    builder.setEncoding(US_ASCII);
    ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint(builder);

    trans.setEndpoint(endpoint);
    return trans;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    XmlToDomDocument trans = createObject(XmlToDomDocument.class); // encoding is not interesting
    trans.setReturnDataType(DataType.fromType(org.w3c.dom.Document.class));
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
    // This is only used during roundtrip test, so it will always be Document
    // instances
    if (expected instanceof Document) {
      expected = new DOMReader().read((Document) expected).asXML();
      result = new DOMReader().read((Document) result).asXML();
    }

    return super.compareResults(expected, result);
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new TransportsConfigurationBuilder();
  }

}
