/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.jaxb.model.Person;
import org.mule.runtime.module.xml.filters.SchemaValidationFilter;
import org.mule.runtime.module.xml.transformer.jaxb.JAXBMarshallerTransformer;
import org.mule.runtime.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;
import org.mule.runtime.module.xml.util.NamespaceManager;

import org.junit.Test;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlNamespaceTestCase extends FunctionalTestCase {

  public XmlNamespaceTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "xml-namespace-config.xml";
  }

  @Test
  public void testGlobalNamespaces() throws Exception {
    NamespaceManager manager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
    assertNotNull(manager);
    assertTrue(manager.isIncludeConfigNamespaces());
    assertEquals(5, manager.getNamespaces().size());
  }

  @Test
  public void testJaxbConfig() throws Exception {
    JAXBMarshallerTransformer t = (JAXBMarshallerTransformer) lookupTransformer("ObjectToXml");
    assertNotNull(t.getJaxbContext());

    JAXBUnmarshallerTransformer t2 = (JAXBUnmarshallerTransformer) lookupTransformer("XmlToObject");
    assertEquals(Person.class, t2.getReturnDataType().getType());
    assertNotNull(t2.getJaxbContext());
  }

  @Test
  public void testSchemaValidationFilterWithCustomResourceResolver() {
    SchemaValidationFilter filter = (SchemaValidationFilter) lookupFilter("SchemaValidationWithResourceResolver");
    assertEquals("schema1.xsd", filter.getSchemaLocations());
    assertTrue(filter.getResourceResolver() instanceof MockResourceResolver);
    assertTrue(filter.getErrorHandler() instanceof MockErrorHandler);
    assertFalse(filter.isReturnResult());
  }

  private Transformer lookupTransformer(String name) {
    Transformer transformer = muleContext.getRegistry().lookupTransformer(name);
    assertNotNull(transformer);
    return transformer;
  }

  private Filter lookupFilter(String name) {
    Filter filter = muleContext.getRegistry().lookupObject(name);
    assertNotNull(filter);
    return filter;
  }

  private static class MockResourceResolver implements LSResourceResolver {

    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
      return null;
    }
  }

  private static class MockErrorHandler implements ErrorHandler {

    public void error(SAXParseException exception) throws SAXException {
      // does nothing
    }

    public void fatalError(SAXParseException exception) throws SAXException {
      // does nothing
    }

    public void warning(SAXParseException exception) throws SAXException {
      // does nothing
    }
  }
}
