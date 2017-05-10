/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap;

import static org.mule.services.soap.SoapTestUtils.assertSimilarXml;
import static org.mule.test.ram.RickAndMortyExtension.RICKS_PHRASE;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.util.IOUtils;
import org.mule.services.soap.TestHttpSoapServer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.soap.services.InterdimentionalCableService;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public class InvokeSoapCustomTransportTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("servicePort");

  private final TestHttpSoapServer service = new TestHttpSoapServer(port.getNumber(), new InterdimentionalCableService());

  @Rule
  public SystemProperty systemProperty = new SystemProperty("serviceAddress", service.getDefaultAddress());

  @Override
  protected void doSetUp() throws Exception {
    service.init();
    XMLUnit.setIgnoreWhitespace(true);
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    service.stop();
  }

  @Override
  protected String getConfigFile() {
    return "soap-rick-and-morty-extension-config.xml";
  }

  @Test
  public void sendThroughCustomTransport() throws Exception {
    Message message = flowRunner("customTransport").keepStreamsOpen().run().getMessage();
    String response = "<con:ram xmlns:con=\"http://ram.test.mule.org\"><text>" + RICKS_PHRASE + "</text></con:ram>";
    assertSimilarXml(response, IOUtils.toString((CursorStreamProvider) message.getPayload().getValue()));
  }

  @Test
  public void sendThroughCustomTransportWithParams() throws Exception {
    Message message = flowRunner("customTransportWithParams").keepStreamsOpen().run().getMessage();
    String response = "<con:ram xmlns:con=\"http://ram.test.mule.org\"><text>CUSTOM RESPONSE</text></con:ram>";
    assertSimilarXml(response, IOUtils.toString((CursorStreamProvider) message.getPayload().getValue()));
  }

  @Test
  public void withDefaultHttp() throws Exception {
    Message message = flowRunner("withDefaultHttp").keepStreamsOpen().run().getMessage();
    String response = "<ns2:getChannelsResponse xmlns:ns2=\"http://services.soap.extension.module.test.mule.org/\">\n"
        + "   <channel>Two Brothers</channel>\n"
        + "   <channel>Fake Doors</channel>\n"
        + "   <channel>The Adventures of Stealy</channel>\n"
        + "</ns2:getChannelsResponse>";
    assertSimilarXml(response, IOUtils.toString((CursorStreamProvider) message.getPayload().getValue()));
  }
}
