/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.checkIsPresent;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.getDeclaration;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.extension.api.introspection.property.ConfigTypeModelProperty;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Before;
import org.junit.Test;

public class ConfigurationModelEnricherTestCase {

  private static final String GET_ENEMY = "getEnemy";
  private static final String LISTEN_PAYMENTS = "ListenPayments";
  private ExtensionDeclaration declaration = null;
  private ClassTypeLoader typeLoader;

  @Before
  public void setUp() {
    final AnnotationsBasedDescriber basedDescriber =
        new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver(getProductVersion()));
    ExtensionDeclarer declarer = basedDescriber.describe(new DefaultDescribingContext(getClass().getClassLoader()));
    new ConfigurationModelEnricher().enrich(new DefaultDescribingContext(declarer, this.getClass().getClassLoader()));
    declaration = declarer.getDeclaration();
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(HeisenbergExtension.class.getClassLoader());
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
    assertThat(getType(configTypeModelProperty.getConfigType()), equalTo(getType(typeLoader.load(HeisenbergExtension.class))));
  }
}
