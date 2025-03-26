/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.dsl;

import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver.getDefault;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resolver.SingleExtensionImportTypesStrategy;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.CarWash;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.subtypes.extension.TopLevelStatelessType;

import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class ComplexPojoDslSyntaxResolverTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String INVESTMENT_PLAN_B_NAME = "investmentPlanB";

  @Inject
  private ExtensionManager extensionManager;

  private ExtensionModel heisenbergExtensionModel;
  private ExtensionModel subTypesExtensionModel;

  @Override
  protected String getConfigFile() {
    // Any config file that uses the Heisenberg extension can be used.
    return "source/heisenberg-source-config.xml";
  }

  @Before
  public void setup() {
    heisenbergExtensionModel = extensionManager.getExtension(HeisenbergExtension.HEISENBERG).get();
    subTypesExtensionModel = extensionManager.getExtension(SubTypesMappingConnector.NAME).get();
  }

  @Test
  public void innerWrapperPojoIsShownAsChild() {
    DslSyntaxResolver dslSyntaxResolver = getDefault(heisenbergExtensionModel, new SingleExtensionImportTypesStrategy());
    MetadataType carWashType = getSubType(heisenbergExtensionModel, CarWash.class).get();
    DslElementSyntax carWashDslElementSyntax = dslSyntaxResolver.resolve(carWashType).get();
    assertThat(carWashDslElementSyntax.getChild(INVESTMENT_PLAN_B_NAME).isPresent(), is(true));

    DslElementSyntax investmentPlanBElementSyntax = carWashDslElementSyntax.getChild(INVESTMENT_PLAN_B_NAME).get();

    assertThat(investmentPlanBElementSyntax.supportsChildDeclaration(), is(true));
    assertThat(investmentPlanBElementSyntax.getElementName(), is(hyphenize(INVESTMENT_PLAN_B_NAME)));
  }

  @Test
  @Issue("W-14645134")
  @Description("Tests that a stateless subtype can be defined as a child element")
  public void statelessSubType() {
    DslSyntaxResolver dslSyntaxResolver = getDefault(subTypesExtensionModel, new SingleExtensionImportTypesStrategy());
    MetadataType type = getSubType(subTypesExtensionModel, TopLevelStatelessType.class).get();
    DslElementSyntax syntax = dslSyntaxResolver.resolve(type).get();
    assertThat(syntax.supportsChildDeclaration(), is(true));
  }

  private Optional<ObjectType> getSubType(ExtensionModel model, Class<?> clazz) {
    return model.getSubTypes().stream()
        .flatMap(subTypesModel -> subTypesModel.getSubTypes().stream())
        .filter(t -> t.toString().contains(clazz.getName()))
        .findFirst();
  }

}
