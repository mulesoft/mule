/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.deployment.management.ComponentInitialStateManager.SERVICE_ID;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.functional.Either;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Scheduler source")
@Stories("Scheduler source management")
public class SchedulerInitialStateTestCase extends AbstractIntegrationTestCase {

  private List<AnnotatedObject> recordedOnStartMessageSources = new ArrayList<>();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-initial-state-management-config.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new ConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(SERVICE_ID, createCustomStateManager());
      }

      private ComponentInitialStateManager createCustomStateManager() {
        return new ComponentInitialStateManager() {

          @Override
          public boolean mustStartMessageSource(AnnotatedObject component) {
            recordedOnStartMessageSources.add(component);
            return component.getLocation().getParts().get(0).getPartPath().equals("runningSchedulerOnStartup");
          }
        };
      }

      @Override
      public boolean isConfigured() {
        return false;
      }
    });
  }

  @Description("ComponentInitialStateManager is called during startup for all message sources")
  @Test
  public void startMessageSourceRequestedOnStartup() {
    assertThat(recordedOnStartMessageSources, hasSize(2));
    assertThat(recordedOnStartMessageSources.stream()
        .map(component -> component.getLocation().getLocation()).collect(toList()),
               hasItems("runningSchedulerOnStartup/source", "notRunningSchedulerOnStartup/source"));
  }

  @Description("ComponentInitialStateManager does not allow to start scheduler message sources")
  @Test
  public void verifyMessageSourcesAreNotStarted() throws MuleException {
    SchedulerMessageSource schedulerMessageSource = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(Location.builder().globalName("runningSchedulerOnStartup").addSourcePart().build()).get();
    assertThat(schedulerMessageSource.isStarted(), is(true));
    schedulerMessageSource = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(Location.builder().globalName("notRunningSchedulerOnStartup").addSourcePart().build()).get();
    assertThat(schedulerMessageSource.isStarted(), is(false));

    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          Either<Error, Optional<Message>> response =
              muleContext.getClient().request("test://runningSchedulerOnStartupQueue", 100);
          return response.isRight() && response.getRight().isPresent();
        } catch (MuleException e) {
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "Message expected by in flow runningSchedulerOnStartup";
      }
    });

    Either<Error, Optional<Message>> response =
        muleContext.getClient().request("test://notRunningSchedulerOnStartupQueue", 100);
    assertThat(response.isRight(), is(true));
    assertThat(response.getRight().isPresent(), is(false));
  }
}
