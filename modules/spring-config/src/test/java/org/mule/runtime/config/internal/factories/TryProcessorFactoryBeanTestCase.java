/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.transaction.MuleTransactionConfig.ACTION_INDIFFERENT_STRING;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.internal.processor.TryScope;
import org.mule.runtime.core.internal.registry.SimpleRegistry;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TryProcessorFactoryBeanTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private final MuleContextWithRegistry muleContextMock = mockContextWithServices();

  private SimpleRegistry registry;

  @Before
  public void setUp() throws RegistrationException {
    registry = new SimpleRegistry(muleContextMock, new MuleLifecycleInterceptor());
    registry.registerObject("txFactory", new TransactionFactoryLocator());
  }

  @Test
  public void doesNotFailWithNoProcessors() throws Exception {
    TryProcessorFactoryBean tryProcessorFactoryBean = new TryProcessorFactoryBean();
    tryProcessorFactoryBean.setTransactionalAction(ACTION_INDIFFERENT_STRING);
    tryProcessorFactoryBean.setTransactionType(LOCAL);

    final Map<QName, Object> annotations = new HashMap<>();
    annotations.put(LOCATION_KEY, new DefaultComponentLocation(of("root"),
                                                               asList(new DefaultLocationPart("root",
                                                                                              of(TypedComponentIdentifier
                                                                                                  .builder()
                                                                                                  .identifier(mock(ComponentIdentifier.class))
                                                                                                  .type(FLOW).build()),
                                                                                              Optional.empty(),
                                                                                              OptionalInt.empty(),
                                                                                              OptionalInt.empty()))));
    annotations.put(ROOT_CONTAINER_NAME_KEY, "root");
    tryProcessorFactoryBean.setAnnotations(annotations);
    registry.inject(tryProcessorFactoryBean);

    TryScope tryMessageProcessor = tryProcessorFactoryBean.getObject();
    initialiseIfNeeded(tryMessageProcessor, muleContextMock);
    tryMessageProcessor.start();
  }

}
