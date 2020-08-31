/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.sampledata.SampleDataResult;

import java.util.Map;

import org.junit.Test;

public class SampleDataTestCase extends DeclarationSessionTestCase {

  @Test
  public void mockedSampleData() {
    SampleDataResult sampleData = session.getSampleData(configLessOPDeclaration(CONFIG_NAME, "item"));
    assertThat(sampleData.isSuccess(), is(true));
    assertThat(sampleData.getSampleData().isPresent(), is(true));
    Message message = sampleData.getSampleData().get();

    Map<String, String> payload = message.<Map<String, String>>getPayload().getValue();
    assertThat(payload.get("id"), is("x"));

    Map<String, String> attributes = message.<Map<String, String>>getAttributes().getValue();
    assertThat(attributes.get("time"), is("now"));
  }
}
