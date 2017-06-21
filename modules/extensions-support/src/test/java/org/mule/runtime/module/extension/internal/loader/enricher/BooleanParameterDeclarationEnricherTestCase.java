/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getNamedObject;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Before;
import org.junit.Test;

public class BooleanParameterDeclarationEnricherTestCase {

  private ExtensionDeclaration declaration = null;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = new DefaultJavaModelLoaderDelegate(HeisenbergExtension.class, getProductVersion())
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new BooleanParameterDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void verifyConfigurationModelPropertyOnOperation() {
    ParameterDeclaration booleanDeclaration =
        getNamedObject(declaration.getConfigurations().get(0).getAllParameters(), "lovesMinerals");
    assertThat(booleanDeclaration.isRequired(), is(FALSE));
    assertThat(booleanDeclaration.getDefaultValue(), is(valueOf(FALSE)));

    booleanDeclaration = getNamedObject(declaration.getConfigurations().get(0).getAllParameters(), "worksAtDEA");
    assertThat(booleanDeclaration.isRequired(), is(FALSE));
    assertThat(booleanDeclaration.getDefaultValue(), is(valueOf(TRUE)));
  }
}
