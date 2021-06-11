/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.internal.xml.DefaultAstXmlParser;
import org.mule.runtime.ast.internal.xml.resolver.ModuleDelegatingEntityResolver;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;

public class AstXmlParserFactoryTestCase {

  private AstXmlParserFactory astXmlParserFactory;
  private AstXmlParser.Builder builder;
  private FeatureFlaggingService featureFlaggingService;

  @Before
  public void setUp() throws Exception {
    builder = mock(AstXmlParser.Builder.class);
    when(builder.withPropertyResolver(any())).thenReturn(builder);
    when(builder.withExtensionModels(any())).thenReturn(builder);
    when(builder.withParentArtifact(any())).thenReturn(builder);
    astXmlParserFactory = new AstXmlParserFactory(builder);
    featureFlaggingService = mock(FeatureFlaggingService.class);
  }

  @Test
  @Issue("EE-7827")
  public void createParserWithLegacyBehaviourWhenFeatureFlagDisabled() {
    Mockito.when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(false);

    astXmlParserFactory.createMuleXmlParser(null, null, false, null, null, false, featureFlaggingService);

    verify(builder).withLegacyFailStrategy();
  }

  @Test
  @Issue("EE-7827")
  public void createParserWithNewBehaviourWhenFeatureFlagEnabled() {
    Mockito.when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(true);

    astXmlParserFactory.createMuleXmlParser(null, null, false, null, null, false, featureFlaggingService);

    verify(builder, never()).withLegacyFailStrategy();
  }
}
