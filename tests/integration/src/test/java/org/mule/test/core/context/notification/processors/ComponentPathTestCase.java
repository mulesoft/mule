/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.INTERCEPTING;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.PROCESSOR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.FLOW_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SUBFLOW_IDENTIFIER;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.junit.Test;

@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-validation"})
public class ComponentPathTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  private static final Optional<TypedComponentIdentifier> FLOW_TYPED_COMPONENT_IDENTIFIER =
      of(builder().withIdentifier(FLOW_IDENTIFIER).withType(FLOW).build());

  private static final Optional<TypedComponentIdentifier> SUB_FLOW_TYPED_COMPONENT_IDENTIFIER =
      of(builder().withIdentifier(SUBFLOW_IDENTIFIER).withType(PROCESSOR).build());

  private static final Optional<String> CONFIG_FILE_NAME = of("component-path-test-flow.xml");

  private static final DefaultComponentLocation FLOW_WITH_SINGLE_MP_LOCATION =
      new DefaultComponentLocation(of("flowWithSingleMp"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithSingleMp",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(27))));
  private static final DefaultComponentLocation FLOW_WITH_MULTIPLE_MP_LOCATION =
      new DefaultComponentLocation(of("flowWithMultipleMps"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithMultipleMps",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(31))));
  private static final DefaultComponentLocation FLOW_WITH_ERROR_HANDLER =
      new DefaultComponentLocation(of("flowWithErrorHandler"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithErrorHandler",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(37))));
  private static final DefaultComponentLocation FLOW_WITH_BLOCK_WITH_ERROR_HANDLER =
      new DefaultComponentLocation(of("flowWithBlockWithErrorHandler"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithBlockWithErrorHandler",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(58))));

  private static final DefaultComponentLocation FLOW_WITH_SOURCE =
      new DefaultComponentLocation(of("flowWithSource"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithSource",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(70))));

  private static final DefaultComponentLocation FLOW_WITH_SPLITTER =
      new DefaultComponentLocation(of("flowWithSplitter"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithSplitter",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(75))));

  private static final DefaultComponentLocation FLOW_WITH_AGGREGATOR =
      new DefaultComponentLocation(of("flowWithAggregator"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithAggregator",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(81))));

  private static final DefaultComponentLocation FLOW_WITH_SCATTER_GATHER =
      new DefaultComponentLocation(of("flowWithScatterGather"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithScatterGather",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(98))));

  private static final DefaultComponentLocation FLOW_WITH_WIRE_TAP =
      new DefaultComponentLocation(of("flowWithWireTap"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithWireTap",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(106))));

  private static final DefaultComponentLocation FLOW_WITH_ASYNC =
      new DefaultComponentLocation(of("flowWithAsync"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithAsync",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(116))));

  private static final DefaultComponentLocation FLOW_WITH_SUBFLOW =
      new DefaultComponentLocation(of("flowWithSubflow"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithSubflow",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(87))));


  private static final DefaultComponentLocation SUBFLOW =
      new DefaultComponentLocation(of("subflow"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("subflow",
                                                                                           SUB_FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(93))));
  private static final Optional<TypedComponentIdentifier> LOGGER =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:logger"))
          .withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> SET_PAYLOAD =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:set-payload"))
          .withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> OBJECT_TO_STRING_TRANSFORMER =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:object-to-byte-array-transformer")).withType(PROCESSOR)
          .build());
  private static final Optional<TypedComponentIdentifier> CHOICE =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:choice")).withType(ROUTER).build());
  private static final Optional<TypedComponentIdentifier> ERROR_HANDLER =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:error-handler"))
          .withType(TypedComponentIdentifier.ComponentType.ERROR_HANDLER).build());
  private static final Optional<TypedComponentIdentifier> ON_ERROR_CONTINUE =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:on-error-continue")).withType(ON_ERROR).build());
  private static final Optional<TypedComponentIdentifier> VALIDATION_IS_FALSE =
      of(builder().withIdentifier(buildFromStringRepresentation("validation:is-false")).withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> WHEN =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:when")).withType(UNKNOWN).build());
  private static final Optional<TypedComponentIdentifier> TEST_COMPONENT =
      of(builder().withIdentifier(buildFromStringRepresentation("test:component")).withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> ON_ERROR_PROPAGATE =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:on-error-propagate")).withType(ON_ERROR).build());
  private static final Optional<TypedComponentIdentifier> BLOCK =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:block")).withType(INTERCEPTING).build());
  private static final Optional<TypedComponentIdentifier> VALIDATION_IS_TRUE =
      of(builder().withIdentifier(buildFromStringRepresentation("validation:is-true")).withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> SKELETON_SOURCE =
      of(builder().withIdentifier(buildFromStringRepresentation("test:skeleton-source")).withType(SOURCE).build());
  private static final Optional<TypedComponentIdentifier> COLLECTION_SPLITTER =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:collection-splitter")).withType(INTERCEPTING).build());
  private static final Optional<TypedComponentIdentifier> COLLECTION_AGGREGATOR =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:collection-aggregator")).withType(INTERCEPTING).build());
  private static final Optional<TypedComponentIdentifier> SCATTER_GATHER =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:scatter-gather")).withType(ROUTER).build());
  private static final Optional<TypedComponentIdentifier> WIRE_TAP =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:wire-tap")).withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> FLOW_REF =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:flow-ref")).withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> PROCESS_CHAIN =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:processor-chain")).withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> ASYNC =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:async")).withType(PROCESSOR).build());

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/component-path-test-flow.xml";
  }

  @Test
  public void flowWithSingleMp() throws Exception {
    flowRunner("flowWithSingleMp").run();
    assertNextProcessorLocationIs(FLOW_WITH_SINGLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(28)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithMultipleMps() throws Exception {
    flowRunner("flowWithMultipleMps").run();
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(32)));
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, CONFIG_FILE_NAME, of(33)));
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("2", OBJECT_TO_STRING_TRANSFORMER,
                            CONFIG_FILE_NAME, of(34)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithErrorHandlerExecutingOnContinue() throws Exception {
    flowRunner("flowWithErrorHandler").withVariable("executeFailingComponent", false).run();
    DefaultComponentLocation choiceLocation = FLOW_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", CHOICE, CONFIG_FILE_NAME, of(38));
    assertNextProcessorLocationIs(choiceLocation);
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER,
                            CONFIG_FILE_NAME, of(46))
        .appendLocationPart("0", ON_ERROR_CONTINUE, CONFIG_FILE_NAME,
                            of(47))
        .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                            of(48)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithErrorHandlerExecutingOnPropagate() throws Exception {
    flowRunner("flowWithErrorHandler").withVariable("executeFailingComponent", true).runExpectingException();
    DefaultComponentLocation choiceLocation = FLOW_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", CHOICE, CONFIG_FILE_NAME, of(38));
    assertNextProcessorLocationIs(choiceLocation);
    assertNextProcessorLocationIs(choiceLocation
        .appendLocationPart("0", WHEN, CONFIG_FILE_NAME, of(39))
        .appendLocationPart("0", TEST_COMPONENT, CONFIG_FILE_NAME, of(40)));
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(46))
        .appendLocationPart("1", ON_ERROR_PROPAGATE, CONFIG_FILE_NAME, of(50))
        .appendLocationPart("0", BLOCK, CONFIG_FILE_NAME, of(51)));
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(46))
        .appendLocationPart("1", ON_ERROR_PROPAGATE, CONFIG_FILE_NAME,
                            of(50))
        .appendLocationPart("0", BLOCK, CONFIG_FILE_NAME, of(51))
        .appendLocationPart("0", VALIDATION_IS_TRUE, CONFIG_FILE_NAME, of(52)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithBlockWithErrorHandler() throws Exception {
    flowRunner("flowWithBlockWithErrorHandler").run();
    DefaultComponentLocation blockLocation =
        FLOW_WITH_BLOCK_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), empty())
            .appendLocationPart("0", BLOCK, CONFIG_FILE_NAME, of(59));
    assertNextProcessorLocationIs(blockLocation);
    assertNextProcessorLocationIs(blockLocation
        .appendLocationPart("0", TEST_COMPONENT, CONFIG_FILE_NAME, of(60)));
    DefaultComponentLocation blockOnErrorContinueLocation = blockLocation
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(61))
        .appendLocationPart("0", ON_ERROR_CONTINUE, CONFIG_FILE_NAME,
                            of(62));
    assertNextProcessorLocationIs(blockOnErrorContinueLocation
        .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME, of(63)));
    assertNextProcessorLocationIs(blockOnErrorContinueLocation
        .appendLocationPart("1", VALIDATION_IS_TRUE, CONFIG_FILE_NAME, of(64)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSource() throws Exception {
    DefaultComponentLocation expectedSourceLocation =
        FLOW_WITH_SOURCE.appendLocationPart("source", SKELETON_SOURCE,
                                            CONFIG_FILE_NAME, of(71));
    Flow flowWithSource = (Flow) getFlowConstruct("flowWithSource");
    DefaultComponentLocation sourceLocation =
        (DefaultComponentLocation) ((AnnotatedObject) flowWithSource.getMessageSource()).getAnnotation(LOCATION_KEY);
    assertThat(sourceLocation, is(expectedSourceLocation));
    assertThat(((AnnotatedObject) flowWithSource.getMessageProcessors().get(0)).getAnnotation(LOCATION_KEY), is(FLOW_WITH_SOURCE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(72))));
  }

  @Test
  public void flowWithSplitter() throws Exception {
    flowRunner("flowWithSplitter").withPayload(Arrays.asList("item")).run();
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SPLITTER.appendLocationPart("processors", empty(), empty(), empty());
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("0", COLLECTION_SPLITTER, CONFIG_FILE_NAME,
                            of(76)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("1", LOGGER, CONFIG_FILE_NAME, of(77)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("2", LOGGER, CONFIG_FILE_NAME, of(78)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithAggregator() throws Exception {
    flowRunner("flowWithAggregator").withPayload(Arrays.asList("item")).run();
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_AGGREGATOR.appendLocationPart("processors", empty(), empty(), empty());
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("0", COLLECTION_SPLITTER, CONFIG_FILE_NAME, of(82)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("1", LOGGER, CONFIG_FILE_NAME, of(83)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("2", COLLECTION_AGGREGATOR, CONFIG_FILE_NAME, of(84)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSubflow() throws Exception {
    flowRunner("flowWithSubflow").run();
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SUBFLOW.appendLocationPart("processors", empty(), empty(), empty());
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(88)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("1", FLOW_REF, CONFIG_FILE_NAME, of(89)));

    assertNextProcessorLocationIs(SUBFLOW.appendLocationPart("0", LOGGER,
                                                             CONFIG_FILE_NAME, of(94)));
    assertNextProcessorLocationIs(SUBFLOW.appendLocationPart("1", VALIDATION_IS_TRUE,
                                                             CONFIG_FILE_NAME, of(95)));

    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("2", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                            of(90)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithScatterGather() throws Exception {
    flowRunner("flowWithScatterGather").run();
    waitUntilNotificationsArrived(4);
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SCATTER_GATHER.appendLocationPart("processors", empty(), empty(), empty());
    DefaultComponentLocation scatterGatherLocation =
        flowWithSplitterProcessorsLocation.appendLocationPart("0", SCATTER_GATHER,
                                                              CONFIG_FILE_NAME, of(99));
    assertNextProcessorLocationIs(scatterGatherLocation);
    assertNextProcessorLocationIs(scatterGatherLocation.appendLocationPart("0", LOGGER,
                                                                           CONFIG_FILE_NAME, of(100)));
    assertNextProcessorLocationIs(scatterGatherLocation.appendLocationPart("1",
                                                                           VALIDATION_IS_TRUE,
                                                                           CONFIG_FILE_NAME, of(101)));
    assertNextProcessorLocationIs(scatterGatherLocation.appendLocationPart("2",
                                                                           VALIDATION_IS_FALSE,
                                                                           CONFIG_FILE_NAME, of(102)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithWireTap() throws Exception {
    flowRunner("flowWithWireTap").run();
    DefaultComponentLocation flowWithWireTapLocation =
        FLOW_WITH_WIRE_TAP.appendLocationPart("processors", empty(), empty(), empty());
    DefaultComponentLocation wireTapLocation =
        flowWithWireTapLocation.appendLocationPart("0", WIRE_TAP,
                                                   CONFIG_FILE_NAME, of(107));
    assertNextProcessorLocationIs(wireTapLocation);
    DefaultComponentLocation wireTapProcessorChainLocation =
        wireTapLocation.appendLocationPart("0", PROCESS_CHAIN,
                                           CONFIG_FILE_NAME, of(108));
    assertNextProcessorLocationIs(wireTapProcessorChainLocation.appendLocationPart("0",
                                                                                   LOGGER,
                                                                                   CONFIG_FILE_NAME, of(109)));
    assertNextProcessorLocationIs(wireTapProcessorChainLocation
        .appendLocationPart("1", VALIDATION_IS_TRUE, CONFIG_FILE_NAME,
                            of(110)));
    assertNextProcessorLocationIs(wireTapProcessorChainLocation
        .appendLocationPart("2", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                            of(111)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithAsync() throws Exception {
    flowRunner("flowWithAsync").run();
    waitUntilNotificationsArrived(3);
    DefaultComponentLocation flowWithAsyncLocation = FLOW_WITH_ASYNC.appendLocationPart("processors", empty(), empty(), empty());
    DefaultComponentLocation asyncLocation = flowWithAsyncLocation
        .appendLocationPart("0", ASYNC, CONFIG_FILE_NAME, of(117));
    assertNextProcessorLocationIs(asyncLocation);
    assertNextProcessorLocationIs(asyncLocation.appendLocationPart("0", LOGGER,
                                                                   CONFIG_FILE_NAME, of(118)));
    assertNextProcessorLocationIs(asyncLocation.appendLocationPart("1", VALIDATION_IS_TRUE,
                                                                   CONFIG_FILE_NAME, of(119)));
    assertNoNextProcessorNotification();
  }

  private void waitUntilNotificationsArrived(int minimumRequiredNotifications) {
    new PollingProber(RECEIVE_TIMEOUT, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return getNotificationsStore().getNotifications().size() >= minimumRequiredNotifications;
      }

      @Override
      public String describeFailure() {
        return "not all expected notifications arrived";
      }
    });
  }

  private void assertNoNextProcessorNotification() {
    ProcessorNotificationStore processorNotificationStore = getNotificationsStore();
    Iterator iterator = processorNotificationStore.getNotifications().iterator();
    assertThat(iterator.hasNext(), is(false));
  }

  private void assertNextProcessorLocationIs(DefaultComponentLocation componentLocation) {
    ProcessorNotificationStore processorNotificationStore = getNotificationsStore();
    assertThat(processorNotificationStore.getNotifications().isEmpty(), is(false));
    MessageProcessorNotification processorNotification =
        (MessageProcessorNotification) processorNotificationStore.getNotifications().get(0);
    processorNotificationStore.getNotifications().remove(0);
    assertThat(processorNotification.getComponentLocation().getLocation(), is(componentLocation.getLocation()));
    assertThat(processorNotification.getComponentLocation(), is(componentLocation));
  }

  private ProcessorNotificationStore getNotificationsStore() {
    return muleContext.getRegistry().get("notificationsStore");
  }

}
