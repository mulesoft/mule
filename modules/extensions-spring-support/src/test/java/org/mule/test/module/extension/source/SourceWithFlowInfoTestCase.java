/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.runtime.FlowInfo;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.SentientSource;

import org.junit.Test;

public class SourceWithFlowInfoTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "source-with-flow-info-config.xml";
  }

  @Test
  public void injectedFlowInfo() throws Exception {
    FlowInfo flowInfo = (FlowInfo) SentientSource.capturedFlowInfo;
    assertThat(flowInfo, is(notNullValue()));

    assertThat(flowInfo.getName(), equalTo("sentient"));
    assertThat(flowInfo.getMaxConcurrency(), is(2));
  }
}
