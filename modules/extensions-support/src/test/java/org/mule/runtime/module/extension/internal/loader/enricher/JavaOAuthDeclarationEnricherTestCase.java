/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getDeclaration;
import static org.mule.test.module.extension.internal.util.ExtensionDeclarationTestUtils.declarerFor;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaOAuthDeclarationEnricher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.oauth.TestOAuthExtension;

import org.junit.Before;
import org.junit.Test;

public class JavaOAuthDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private ExtensionDeclaration declaration = null;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = declarerFor(TestOAuthExtension.class, getMuleManifest().getProductVersion());
    new JavaOAuthDeclarationEnricher().enrich(
                                              new DefaultExtensionLoadingContext(declarer,
                                                                                 builder(getClass().getClassLoader(),
                                                                                         getDefault(emptySet()))
                                                                                     .build()));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void verifyUnauthorizeOperationIsAddedToAllConfigs() {
    declaration.getConfigurations().stream().forEach(configurationDeclaration -> {
      OperationDeclaration operationDeclaration = getDeclaration(configurationDeclaration.getOperations(), "unauthorize");
      assertThat(operationDeclaration, is(not(nullValue())));
    });
  }

}
