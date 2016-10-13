/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public abstract class ResourcesGeneratorContractTestCase extends AbstractMuleTestCase {

  protected static final String RESOURCE_PATH = "path";
  protected static final byte[] RESOURCE_CONTENT = "hello world!".getBytes();

  protected ResourcesGenerator generator;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected GeneratedResourceFactory resourceFactory;

  @Mock
  protected ExtensionModel extensionModel;

  protected List<GeneratedResourceFactory> resourceFactories;

  @Before
  public void before() {
    when(resourceFactory.generateResource(extensionModel))
        .thenReturn(Optional.of(new GeneratedResource(RESOURCE_PATH, RESOURCE_CONTENT)));
    resourceFactories = Arrays.asList(resourceFactory);
    generator = buildGenerator();

  }

  protected abstract ResourcesGenerator buildGenerator();

  @Test
  public void generate() {
    generator.generateFor(extensionModel);
    verify(resourceFactory).generateResource(extensionModel);
  }
}
