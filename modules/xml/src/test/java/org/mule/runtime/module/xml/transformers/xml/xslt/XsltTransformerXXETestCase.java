/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml.xslt;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.instanceOf;

import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.IOUtils;

import java.io.ByteArrayInputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This test case validates that by default the XSLT transformer is not vulnerable to External Entity Processing attack unless
 * explicitly allowed
 * <p>
 * <b>EIP Reference:</b> <a href="https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Processing"<a/>
 * </p>
 */
public class XsltTransformerXXETestCase extends FunctionalTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "xslt-xxe-config.xml";
  }

  @Test
  public void xxeAsStream() throws Exception {
    String input = this.makeInput();
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(MessageTransformerException.class));
    this.flowRunner("safeXxe").withPayload(new ByteArrayInputStream(input.getBytes())).run().getMessage().getPayload();
  }

  @Test
  public void xxeAsString() throws Exception {
    String input = this.makeInput();
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(MessageTransformerException.class));
    this.flowRunner("safeXxe").withPayload(input).run().getMessage().getPayload();
  }

  @Test
  public void xxeAsByteArray() throws Exception {
    String input = this.makeInput();
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(MessageTransformerException.class));
    this.flowRunner("safeXxe").withPayload(input.getBytes()).run().getMessage().getPayload();
  }

  @Test
  public void unsafeXxeAsStream() throws Exception {
    String input = this.makeInput();
    this.assertUnsafe(new ByteArrayInputStream(input.getBytes()));
  }

  @Test
  public void unsafeXxeAsString() throws Exception {
    String input = this.makeInput();
    this.assertUnsafe(input);
  }

  @Test
  public void unsafeXxeAsByteArray() throws Exception {
    String input = this.makeInput();
    this.assertUnsafe(input.getBytes());
  }

  private void assertUnsafe(Object payload) throws Exception {
    String output = (String) this.flowRunner("unsafeXxe").withPayload(payload).run().getMessage().getPayload();
    assertTrue(output.contains("secret"));
  }

  private String makeInput() {
    return String.format(
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                             + "<!DOCTYPE spi_doc_type[ <!ENTITY spi_entity_ref SYSTEM 'file:%s'>]>\n" + "<root>\n"
                             + "<elem>&spi_entity_ref;</elem>\n" + "<something/>\n" + "</root>",
                         IOUtils.getResourceAsUrl("xxe-passwd.txt", this.getClass()).getPath());
  }
}
