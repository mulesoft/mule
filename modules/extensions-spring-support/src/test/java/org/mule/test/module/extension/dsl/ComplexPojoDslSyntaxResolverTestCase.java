/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.dsl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.NameUtils.hyphenize;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resolver.SingleExtensionImportTypesStrategy;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.CarWash;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

public class ComplexPojoDslSyntaxResolverTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String INVESTMENT_PLAN_B_NAME = "investmentPlanB";

  @Inject
  private ExtensionManager extensionManager;

  private ExtensionModel heisenbergExtensionModel;
  private DslSyntaxResolver dslSyntaxResolver;

  @Override
  protected String getConfigFile() {
    // Any config file that uses the Heisenberg extension can be used.
    return "source/heisenberg-source-config.xml";
  }

  @Before
  public void setup() {
    heisenbergExtensionModel = extensionManager.getExtension(HeisenbergExtension.HEISENBERG).get();
    dslSyntaxResolver = DslSyntaxResolver.getDefault(heisenbergExtensionModel, new SingleExtensionImportTypesStrategy());
  }

  @Test
  public void innerWrapperPojoIsShownAsChild() {
    MetadataType carWashType = heisenbergExtensionModel.getTypes().stream()
        .filter(metadataType -> metadataType.toString().contains(CarWash.class.getName())).findFirst().get();
    DslElementSyntax carWashDslElementSyntax = dslSyntaxResolver.resolve(carWashType).get();
    assertThat(carWashDslElementSyntax.getChild(INVESTMENT_PLAN_B_NAME).isPresent(), is(true));

    DslElementSyntax investmentPlanBElementSyntax = carWashDslElementSyntax.getChild(INVESTMENT_PLAN_B_NAME).get();

    assertThat(investmentPlanBElementSyntax.supportsChildDeclaration(), is(true));
    assertThat(investmentPlanBElementSyntax.getElementName(), is(hyphenize(INVESTMENT_PLAN_B_NAME)));
  }

}
