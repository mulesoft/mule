/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_PAYLOAD_STATISTICS;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.MuleSystemProperties;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STATISTICS)
public class PayloadStatisticsDisabledTestCase extends AbstractPayloadStatisticsTestCase {

  @Rule
  public SystemProperty muleEnableStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  @Rule
  public SystemProperty muleDisablePayloadStatistics = new SystemProperty(MULE_DISABLE_PAYLOAD_STATISTICS, "true");

  @Test
  public void decorateInputStreamDisabled() throws IOException {
    final InputStream decorated = new ByteArrayInputStream("Hello World".getBytes(UTF_8));
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1).decorateInput(decorated, CORR_ID);

    assertThat(decorated, sameInstance(decorator));

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    final DefaultFlowConstructStatistics defaultFlowConstrutStatistics =
        new DefaultFlowConstructStatistics("flowType", "flowName");
    final AllStatistics allStatistics = muleContext.getStatistics();
    allStatistics.add(defaultFlowConstrutStatistics);


    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read();

    verifyNoStatistics(statistics);

    assertThat(allStatistics.getApplicationStatistics().isEnabled(), is(TRUE));
    assertThat(defaultFlowConstrutStatistics.isEnabled(), is(TRUE));
    assertThat(statistics.isEnabled(), is(Boolean.FALSE));
  }

}
