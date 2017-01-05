/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.util.NotificationUtils.buildPathResolver;

import org.mule.functional.extensions.CompatibilityFunctionalTestCaseRunnerConfig;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.DefaultMessageProcessorPathElement;
import org.mule.runtime.core.util.NotificationUtils.FlowMap;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

public class CompatibilityMessageProcessorNotificationPathTestCase extends MuleArtifactFunctionalTestCase
    implements CompatibilityFunctionalTestCaseRunnerConfig {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/compatibility-message-processor-notification-test-flow.xml";
  }

  @Test
  public void interceptors() throws Exception {
    testFlowPaths("cxfMP", "/0", "/1", "/2");
  }

  private void testFlowPaths(String flowName, String... nodes) throws Exception {
    String[] expectedPaths = generatePaths(flowName, nodes);
    FlowConstruct flow = getFlowConstruct(unescape(flowName));
    DefaultMessageProcessorPathElement flowElement = new DefaultMessageProcessorPathElement(null, flowName);
    ((Pipeline) flow).addMessageProcessorPathElements(flowElement);
    FlowMap messageProcessorPaths = buildPathResolver(flowElement);

    assertThat(messageProcessorPaths.getAllPaths().toString(), messageProcessorPaths.getAllPaths(), hasSize(nodes.length));
    assertThat(messageProcessorPaths.getAllPaths(), hasItems(expectedPaths));
  }

  private String[] generatePaths(String flowName, String[] nodes) {
    Set<String> pathSet = new LinkedHashSet<>();
    String base = "/" + flowName + "/processors";
    for (String node : nodes) {
      if (!node.startsWith("/")) {
        base = "/" + flowName + "/";
      }
      pathSet.add(base + node);
    }
    return pathSet.toArray(new String[0]);
  }

  private String unescape(String name) {
    StringBuilder builder = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (i < (name.length() - 1) && name.charAt(i + 1) == '/') {
        builder.append("/");
        i++;
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }
}
