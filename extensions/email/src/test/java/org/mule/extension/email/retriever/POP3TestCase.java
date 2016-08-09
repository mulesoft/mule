/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameters;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.api.message.MuleMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunnerDelegateTo(Parameterized.class)
public class POP3TestCase extends AbstractEmailRetrieverTestCase {

  @Parameter
  public String protocol;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"pop3"}, {"pop3s"}});
  }

  @Override
  protected String getConfigFile() {
    return format("retriever/%s.xml", protocol);
  }

  @Override
  public String getProtocol() {
    return protocol;
  }

  @Test
  public void retrieveAndRead() throws Exception {
    List<MuleMessage> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    assertThat(messages, hasSize(10));
    messages.forEach(m -> assertBodyContent((String) m.getPayload()));
  }
}
