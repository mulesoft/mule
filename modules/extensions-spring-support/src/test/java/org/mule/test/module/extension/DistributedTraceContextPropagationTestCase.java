/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.extension;

import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class DistributedTraceContextPropagationTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "distributed-trace-context-propagation-test.xml";
  }

  @Test
  public void defaultTraceContextPropagator() throws Exception {
    final CoreEvent event = flowRunner("distributedTraceContextManager").run();
    DistributedTraceContextManager distributedTraceContextManager =
        (DistributedTraceContextManager) event.getMessage().getPayload().getValue();
    assertThat(distributedTraceContextManager.getClass().getName(),
               equalTo("org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager"));
  }
}
