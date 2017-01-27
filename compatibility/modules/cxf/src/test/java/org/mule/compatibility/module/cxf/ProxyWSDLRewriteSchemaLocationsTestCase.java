/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.module.cxf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.helpers.DOMUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class ProxyWSDLRewriteSchemaLocationsTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public final DynamicPort httpPortProxy = new DynamicPort("portProxy");

  @Rule
  public final DynamicPort httpPortMockServer = new DynamicPort("portMockServer");

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).build();

  private MuleContext mockServerContext;

  @Override
  protected String getConfigFile() {
    return "wsdlAndXsdMockServer/proxy-wsdl-rewrite-schema-locations-conf-httpn.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    ApplicationContextBuilder applicationContextBuilder = new ApplicationContextBuilder() {

      /**
       * Adds a {@link ConfigurationBuilder} that sets the {@link #extensionManager} into the {@link #muleContext}. This
       * {@link ConfigurationBuilder} is set as the first element of the {@code builders} {@link List}
       *
       * @param builders the list of {@link ConfigurationBuilder}s that will be used to initialise the {@link #muleContext}
       */
      @Override
      protected final void addBuilders(List<ConfigurationBuilder> builders) {
        ProxyWSDLRewriteSchemaLocationsTestCase.this.addBuilders(builders);
      }
    };
    applicationContextBuilder
        .setApplicationResources(new String[] {"wsdlAndXsdMockServer/proxy-wsdl-rewrite-schema-locations-conf-server-httpn.xml"});
    mockServerContext = applicationContextBuilder.build();
    super.doSetUpBeforeMuleContextCreation();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    super.doTearDownAfterMuleContextDispose();
    if (mockServerContext != null) {
      mockServerContext.dispose();
    }
  }

  @Test
  public void testProxyWSDLRewriteAllSchemaLocations() throws Exception {
    String proxyAddress = "http://localhost:" + httpPortProxy.getNumber() + "/localServicePath";
    InternalMessage response =
        muleContext.getClient()
            .send(proxyAddress + "?wsdl", InternalMessage.builder().nullPayload().build(), HTTP_REQUEST_OPTIONS)
            .getRight();

    Set<String> expectedParametersValues = new HashSet<>();
    expectedParametersValues.addAll(Arrays.asList("xsd=xsd0"));

    List<Element> schemaImports = getSchemaImports(getWsdl(response));
    for (Element schemaImport : schemaImports) {
      String schemaLocation = getLocation(schemaImport);
      int parametersStart = schemaLocation.indexOf("?");
      String locationPath = schemaLocation.substring(0, parametersStart);

      assertThat(locationPath, is(proxyAddress));

      String queryString = schemaLocation.substring(parametersStart + 1);
      expectedParametersValues.remove(queryString);
    }
    assertTrue(expectedParametersValues.isEmpty());
  }

  private Document getWsdl(InternalMessage response) throws Exception {
    return XMLUnit.buildTestDocument(new InputSource(new StringReader(getPayloadAsString(response))));
  }

  private List<Element> getSchemaImports(Document wsdl) {
    return DOMUtils.findAllElementsByTagName(wsdl.getDocumentElement(), "xsd:import");
  }

  private String getLocation(Element schemaImport) {
    return schemaImport.getAttributes().getNamedItem("schemaLocation").getNodeValue();
  }

}
