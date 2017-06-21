/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.checkIsPresent;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigTypeModelProperty;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Before;
import org.junit.Test;

public class JavaConfigurationDeclarationEnricherTestCase {

  private static final String GET_ENEMY = "getEnemy";
  private static final String LISTEN_PAYMENTS = "ListenPayments";
  private ExtensionDeclaration declaration = null;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = new DefaultJavaModelLoaderDelegate(HeisenbergExtension.class, getProductVersion())
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new JavaConfigurationDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void verifyConfigurationModelPropertyOnOperation() {
    OperationDeclaration operationDeclaration = getDeclaration(declaration.getConfigurations().get(0).getOperations(), GET_ENEMY);
    final ConfigTypeModelProperty configTypeModelProperty = checkIsPresent(operationDeclaration, ConfigTypeModelProperty.class);

    assertType(configTypeModelProperty);
  }

  @Test
  public void verifyConfigurationModelPropertyOnSource() {
    SourceDeclaration sourceDeclaration =
        getDeclaration(declaration.getConfigurations().get(0).getMessageSources(), LISTEN_PAYMENTS);
    final ConfigTypeModelProperty configTypeModelProperty = checkIsPresent(sourceDeclaration, ConfigTypeModelProperty.class);

    assertType(configTypeModelProperty);
  }

  private void assertType(ConfigTypeModelProperty configTypeModelProperty) {
    assertThat(configTypeModelProperty.getConfigType(), equalTo(HeisenbergExtension.class));
  }
}
