/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml.xslt;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * This test case validates that the XSLT transformer is not vulnerable to Billion Laughs attack when internal entity expansion is
 * disabled
 * <p>
 * <b>EIP Reference:</b> <a href="https://en.wikipedia.org/wiki/Billion_laughs"<a/>
 * </p>
 */
public class XsltTransformerBLTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "xslt-bl-config.xml";
  }

  protected String makeInput() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<!DOCTYPE foo [<!ENTITY xxe1 \"01\">" +
        "<!ENTITY xxe2 \"&xxe1;&xxe1;\">" +
        "<!ENTITY xxe3 \"&xxe2;&xxe2;\">" +
        "<!ENTITY xxe4 \"&xxe3;&xxe3;\">" +
        "]> \n" +
        "<entityName>Hello123456890 &xxe4;&xxe4;&xxe4;</entityName>";
  }

  @Test
  public void enabled() throws Exception {
    String input = makeInput();
    Object payload = input.getBytes();
    String output = (String) flowRunner("flowBLEnabled").withPayload(payload).run().getMessage().getPayload().getValue();
    assertThat(output, containsString("010101010101010101010101010101010101010101010101"));
  }

  @Test
  public void disabled() throws Exception {
    String input = makeInput();
    Object payload = input.getBytes();
    String output = (String) flowRunner("flowBLDisabled").withPayload(payload).run().getMessage().getPayload().getValue();
    assertThat(output, not(containsString("010101010101010101010101010101010101010101010101")));
  }
}
