/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExclusiveParametersDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.test.vegan.extension.VeganExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class InheritedMetadataKeyResolverTestCase extends AbstractJavaExtensionDeclarationTestCase {

  public static final String APPLE_CONFIG_NAME = "apple-config";
  public static final String EAT_APPLE_OPERATION_NAME = "eatApple";

  public static final String TYPE_KEYS_RESOLVE_NAME = "AppleTypesResolver";
  public static final boolean TYPE_KEYS_RESOLVE_REQUIRES_CONFIG = true;
  public static final boolean TYPE_KEYS_RESOLVE_REQUIRES_CONNECTION = true;

  private ExtensionDeclaration extensionDeclaration;

  @Before
  public void setUp() {
    setDeclarer(declarerFor(VeganExtension.class));
    extensionDeclaration = declareExtension().getDeclaration();
  }

  @Test
  public void inheritedMetadataKeyResolverFromExtension() {
    ConfigurationDeclaration configuration = getConfiguration(extensionDeclaration, APPLE_CONFIG_NAME);
    OperationDeclaration operation = getOperation(configuration, EAT_APPLE_OPERATION_NAME);

    TypeResolversInformationModelProperty typeResolversInformationModelProperty =
        operation.getModelProperty(TypeResolversInformationModelProperty.class).get();

    assertThat(typeResolversInformationModelProperty.getKeysResolver().isPresent(), is(true));

    ResolverInformation keyResolverInformation = typeResolversInformationModelProperty.getKeysResolver().get();

    assertThat(keyResolverInformation.getResolverName(), is(TYPE_KEYS_RESOLVE_NAME));
    assertThat(keyResolverInformation.isRequiresConfiguration(), is(TYPE_KEYS_RESOLVE_REQUIRES_CONFIG));
    assertThat(keyResolverInformation.isRequiresConnection(), is(TYPE_KEYS_RESOLVE_REQUIRES_CONNECTION));
  }

}
