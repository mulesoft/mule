/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.source.SchedulerMessageSource.SCHEDULER_MESSAGE_SOURCE_IDENTIFIER;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.simple.SetPayloadMessageProcessor;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Configuration component locator")
@Stories("Search configuration component locations")
public class ConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/component-locator-config.xml";
  }

  @Description("Search for a global component that do not exists return an empty optional")
  @Test
  public void globalObjectNotFound() {
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("nonExistent").build()).isPresent(),
               is(false));
  }

  @Description("Search for a global component found returns an Optional with the proper value")
  @Test
  public void globalObjectFound() {
    Optional<AnnotatedObject> myFlow =
        muleContext.getConfigurationComponentLocator().find(builder().globalName("myFlow").build());
    assertThat(myFlow.isPresent(), is(true));
    assertThat(myFlow.get(), instanceOf(Flow.class));
  }

  @Description("Search for a component with an non existent global component part returns an empty optional")
  @Test
  public void badContainerType() {
    Location location = builder().globalName("pepe").addProcessorsPart().addIndexPart(0).build();
    assertThat(muleContext.getConfigurationComponentLocator().find(location).isPresent(), is(false));
  }

  @Description("Search for a message source component by the flow component name")
  @Test
  public void sourceByPath() {
    Location sourceLocation = builder().globalName("myFlow").addSourcePart().build();
    Optional<AnnotatedObject> source = muleContext.getConfigurationComponentLocator().find(sourceLocation);
    assertThat(source.isPresent(), is(true));
    assertThat(source.get(), instanceOf(MessageSource.class));
  }

  @Description("Search message processors components within a flow")
  @Test
  public void messageProcessorByPath() {
    Location.Builder myFlowProcessorsLocationBuilder = builder().globalName("myFlow").addProcessorsPart();
    Optional<AnnotatedObject> processor =
        muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(0).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(LoggerMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(1).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(SetPayloadMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(2).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(AsyncDelegateMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator()
        .find(myFlowProcessorsLocationBuilder.addIndexPart(2).addProcessorsPart().addIndexPart(0).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(SetPayloadMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator()
        .find(myFlowProcessorsLocationBuilder.addIndexPart(2).addProcessorsPart().addIndexPart(1).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(LoggerMessageProcessor.class));
  }

  @Description("Search all scheduler message sources within the configuration")
  @Test
  public void findAllSchedulers() {
    List<AnnotatedObject> components = muleContext.getConfigurationComponentLocator().find(SCHEDULER_MESSAGE_SOURCE_IDENTIFIER);
    assertThat(components, hasSize(2));
    assertThat(components.stream().map(component -> component.getLocation().getParts().get(0).getPartPath()).collect(toList()),
               hasItems("myFlow", "anotherFlow"));
  }

  @Description("Search by ComponentIdentifier returns empty collection when no elements could be found")
  @Test
  public void notFoundComponentByComponentIdentifierReturnsEmptyCollection() {
    List<AnnotatedObject> components = muleContext.getConfigurationComponentLocator()
        .find(ComponentIdentifier.buildFromStringRepresentation("mule:nonExistent"));
    assertThat(components, notNullValue());
    assertThat(components, hasSize(0));
  }
}
