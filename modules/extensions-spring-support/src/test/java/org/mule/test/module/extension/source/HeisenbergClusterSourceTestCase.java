/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.test.allure.AllureConstants.ClusteringFeature.CLUSTERING;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.sourceTimesStarted;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;

@RunnerDelegateTo(Parameterized.class)
@Features({@Feature(CLUSTERING), @Feature(SOURCES)})
public class HeisenbergClusterSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Parameters(name = "primaryPollingInstance: {0}")
  public static Collection<Boolean> params() {
    return asList(false, true);
  }

  @Parameter(0)
  public boolean primaryPollingInstance;

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
                                                                         new TestClusterService(primaryPollingInstance));
      }
    });
  }

  @Test
  public void primaryNodeOnlyDefaultToTrue() throws Exception {
    startFlow("source-default");
    if (primaryPollingInstance) {
      assertThat(sourceTimesStarted, is(1));
    } else {
      assertThat(sourceTimesStarted, is(0));
    }
  }

  @Test
  public void primaryNodeOnlySetToTrue() throws Exception {
    startFlow("source-true");
    if (primaryPollingInstance) {
      assertThat(sourceTimesStarted, is(1));
    } else {
      assertThat(sourceTimesStarted, is(0));
    }
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

    private final boolean primaryPollingInstance;

    public TestClusterService(boolean primaryPollingInstance) {
      this.primaryPollingInstance = primaryPollingInstance;
    }

    @Override
    public boolean isPrimaryPollingInstance() {
      return primaryPollingInstance;
    }

  }
}
