/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import java.io.ByteArrayInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import junit.framework.TestCase;
import org.apache.cxf.message.Message;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mule.module.cxf.support.ProxyService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.util.xmlsecurity.XMLSecureFactories.createDefault;

public class MuleInvokerTestCase extends TestCase {

  public static final String SOAP_WITH_ELEMENTS_IN_BODY = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://inbound.tpmtogglevalues.cocacola.com/\"><SOAP-ENV:Header/><SOAP-ENV:Body><test>somecontent</test></SOAP-ENV:Body></SOAP-ENV:Envelope>";
  public static final String SOAP_WITHOUT_ELEMENTS_IN_BODY = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://inbound.tpmtogglevalues.cocacola.com/\"><SOAP-ENV:Header/><SOAP-ENV:Body></SOAP-ENV:Body></SOAP-ENV:Envelope>";
  private MuleInvoker muleInvoker;
  private Message cxfMessage;
  private CxfInboundMessageProcessor cxfProxyInboundMessageProcessor;

  @Before
  public void setUp()
  {
    cxfProxyInboundMessageProcessor = mock(CxfInboundMessageProcessor.class);
    when(cxfProxyInboundMessageProcessor.isProxy()).thenReturn(true);

    muleInvoker = new MuleInvoker(cxfProxyInboundMessageProcessor, ProxyService.class);

    cxfMessage = mock(Message.class);
  }

  @Test
  public void testExtractPayloadFromEmptySoapBodyReturnsEmptyArray_WhenExtractingAPayloadFromAProxyCxfProcessorInvocation() throws XMLStreamException {
    // Given
    when(cxfMessage.getContent(XMLStreamReader.class)).thenReturn(makeXmlStreamReader(SOAP_WITHOUT_ELEMENTS_IN_BODY));

    // When
    Object result = muleInvoker.extractPayload(cxfMessage);

    // Then
    assertThat(result, Matchers.<Object>is(new Object[]{}));
  }

  @Test
  public void testExtractPayloadFromNonEmptySoapBodyReturnsStreamReader_WhenExtractingAPayloadFromAProxyCxfProcessorInvocation() throws XMLStreamException {
    // Given
    when(cxfMessage.getContent(XMLStreamReader.class)).thenReturn(makeXmlStreamReader(SOAP_WITH_ELEMENTS_IN_BODY));

    // When
    Object result = muleInvoker.extractPayload(cxfMessage);

    // Then
    assertThat(result, Matchers.instanceOf(XMLStreamReader.class));
    assertThat(((XMLStreamReader) result).getName().toString(), is("test"));
  }

  private XMLStreamReader makeXmlStreamReader(String stringContent) throws XMLStreamException {
    XMLStreamReader xmlStreamReader = createDefault().getXMLInputFactory().createXMLStreamReader(new ByteArrayInputStream(stringContent.getBytes()));
    xmlStreamReader.nextTag(); // envelope start
    xmlStreamReader.nextTag(); // before header start
    xmlStreamReader.nextTag(); // header start
    xmlStreamReader.nextTag(); // before body start
    xmlStreamReader.nextTag(); // body start
    return xmlStreamReader; // we return the body element
  }

}