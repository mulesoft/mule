/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.test;

import static java.util.Arrays.asList;
import static java.util.Optional.of;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
public abstract class ResourcesGeneratorContractTestCase extends AbstractMuleTestCase {

  protected static final String RESOURCE_PATH = "path";
  protected static final byte[] RESOURCE_CONTENT = "hello world!".getBytes();

  protected ResourcesGenerator generator;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected GeneratedResourceFactory resourceFactory;

  @Mock
  protected ExtensionModel extensionModel;

  protected List<GeneratedResourceFactory> resourceFactories;

  @Before
  public void before() {
    when(resourceFactory.generateResource(extensionModel))
        .thenReturn(of(new GeneratedResource(false, RESOURCE_PATH, RESOURCE_CONTENT)));
    resourceFactories = asList(resourceFactory);
    generator = buildGenerator();

  }

  protected abstract ResourcesGenerator buildGenerator();

  @Test
  public void generate() {
    generator.generateFor(extensionModel);
    verify(resourceFactory).generateResource(extensionModel);
  }
}
