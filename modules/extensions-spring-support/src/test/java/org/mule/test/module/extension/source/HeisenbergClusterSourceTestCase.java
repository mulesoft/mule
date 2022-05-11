/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.test.allure.AllureConstants.ClusteringFeature.CLUSTERING;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.sourceTimesStarted;

import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;

@Features({@Feature(CLUSTERING), @Feature(SOURCES)})
public class HeisenbergClusterSourceTestCase extends AbstractExtensionFunctionalTestCase {

  private Flow flow;

  @Override
  protected String getConfigFile() {
    return "source/heisenberg-cluster-source-config.xml";
  }

  @Before
  public void before() {
    sourceTimesStarted = 0;
  }

  @Override
  protected void doTearDown() throws Exception {
    if (flow != null) {
      flow.stop();
    }

    super.doTearDown();
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(OBJECT_CLUSTER_SERVICE,
                                                                         new TestClusterService());
      }
    });
  }

  @Test
  public void primaryNodeOnlyDefaultToTrue() throws Exception {
    startFlow("source-default");
    assertThat(sourceTimesStarted, is(0));
  }

  @Test
  public void primaryNodeOnlySetToTrue() throws Exception {
    startFlow("source-true");
    assertThat(sourceTimesStarted, is(0));
  }

  @Test
  public void primaryNodeOnlySetToFalse() throws Exception {
    startFlow("source-false");
    assertThat(sourceTimesStarted, is(1));
  }

  private void startFlow(String flowName) throws Exception {
    flow = ((Flow) getFlowConstruct(flowName));
    flow.start();
  }

  private static class TestClusterService implements ClusterService {

    @Override
    public boolean isPrimaryPollingInstance() {
      return false;
    }

  }
}
