/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.Aquarium;
import org.mule.test.petstore.extension.ExclusiveCashier;
import org.mule.test.petstore.extension.ExclusivePetBreeder;
import org.mule.test.petstore.extension.PetStoreDeal;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunnerDelegateTo(MockitoJUnitRunner.class)
public class PetStoreExclusiveParameterRequiredWithNullExpressionTestCase extends AbstractExtensionFunctionalTestCase {

  private final String TEST_VALUE = "TEST";

  @Mock
  ConfigurationProperties configProperties;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Mockito.doReturn(Optional.empty()).when(configProperties).resolveBooleanProperty(Matchers.anyString());
    return singletonMap(OBJECT_CONFIGURATION_PROPERTIES, configProperties);
  }

  @Override
  protected String getConfigFile() {
    return "petstore-exclusive-parameters-required-with-null-expression.xml";
  }

  @Test
  public void getBreederOperationFail() throws Exception {
    flowRunner("getBreederOperation").withVariable("mammals", null)
        .runExpectingException(hasMessage(containsString("Required parameters need to be assigned with non null values")));
  }

  @Test
  public void getBreederOperationSuccess() throws Exception {
    ExclusivePetBreeder exclusivePetBreeder = (ExclusivePetBreeder) flowRunner("getBreederOperation")
        .withVariable("mammals", TEST_VALUE).run().getMessage().getPayload().getValue();
    assertThat(exclusivePetBreeder.getunaliasedNammals(), is(TEST_VALUE));
  }

  @Test
  public void getAquariumOperationFail() throws Exception {
    flowRunner("getAquariumOperation").withVariable("frogName", null)
        .runExpectingException(hasMessage(containsString("Required parameters need to be assigned with non null values")));
  }

  @Test
  public void getAquariumOperationSuccess() throws Exception {
    Aquarium aquarium = (Aquarium) flowRunner("getAquariumOperation")
        .withVariable("frogName", TEST_VALUE).run().getMessage().getPayload().getValue();
    assertThat(aquarium.getPond().getFrogName(), is(TEST_VALUE));
  }

  @Test
  public void getCashierOperationSucess() throws Exception {
    ExclusiveCashier cashier = (ExclusiveCashier) flowRunner("getCashierOperation")
        .withVariable("pensionPlan", null).run().getMessage().getPayload().getValue();
    assertThat(cashier.getDebt(), nullValue());
    assertThat(cashier.getMoney(), nullValue());
    assertThat(cashier.getPensionPlan(), nullValue());
    assertThat(cashier.getRothIRA(), nullValue());
  }

  @Test
  public void getPetStoreDealOperation() throws Exception {
    PetStoreDeal petStoreDeal = (PetStoreDeal) flowRunner("getPetStoreDealOperation").withVariable("pensionPlan", null).run()
        .getMessage().getPayload().getValue();
    assertThat(petStoreDeal.getCashier().getPensionPlan(), nullValue());
  }

}
