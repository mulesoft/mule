/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.core.internal.streaming.object.ConsumerIterator;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class POP3TestCase extends AbstractEmailRetrieverTestCase {

  @Parameter
  public String protocol;

  @Parameters(name = "{0}")
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
    ConsumerIterator<Result> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    int count = 0;
    while (messages.hasNext()) {
      assertBodyContent((String) messages.next().getOutput());
      count++;
    }
    assertThat(count, is(pageSize));
  }
}
