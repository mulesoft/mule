/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.resolver;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;

import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParameterResolverOperationExecutionTestCase extends AbstractParameterResolverTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private static final Matcher<? super Weapon> WEAPON_MATCHER =
      allOf(notNullValue(), instanceOf(Ricin.class), hasProperty("microgramsPerKilo", is(100L)));

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"parameter-resolver-operation-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void operationWithExpressionResolver() throws Exception {
    assertExpressionResolverWeapon("processWeapon", PAYLOAD, WEAPON_MATCHER);
  }

  @Test
  public void operationWithExpressionResolverAsStaticChildElement() throws Exception {
    assertExpressionResolverWeapon("processWeaponAsStaticChildElement", null, WEAPON_MATCHER);
  }

  @Test
  public void operationWithExpressionResolverAsDynamicChildElement() throws Exception {
    assertExpressionResolverWeapon("processWeaponAsDynamicChildElement", null, WEAPON_MATCHER);
  }

  @Test
  public void parameterResolverWithDefaultValue() throws Exception {
    assertExpressionResolverWeapon("processWeaponWithDefaultValue", PAYLOAD, WEAPON_MATCHER);
  }


  @Test
  public void operationWithExpressionResolverAndNullWeapon() throws Exception {
    assertExpressionResolverWeapon("processNullWeapon", null, is(nullValue()));
  }

  @Test
  public void operationWithExpressionResolverNegative() throws Exception {
    expectedException.expect(ExpressionRuntimeException.class);
    final ParameterResolver<Weapon> weapon =
        (ParameterResolver<Weapon>) flowRunner("processWrongWeapon").run().getMessage().getPayload().getValue();
    weapon.resolve();
  }

  @Test
  public void parameterResolverOfListOfComplexType() throws Exception {
    assertExpressionResolverWeapon("processWeaponList", "#[mel:payload]",
                                   allOf((Matcher) hasSize(1), hasItem(is(instanceOf(Weapon.class)))));
  }

  @Test
  public void parameterResolverOfListOfComplexTypeAsChild() throws Exception {
    assertExpressionResolverWeapon("processWeaponListAsChild", null,
                                   allOf((Matcher) hasSize(2), hasItem(is(instanceOf(Weapon.class)))));
  }

  @Test
  public void parameterResolverOfListOfSimpleType() throws Exception {
    assertExpressionResolverWeapon("processAddressBookAsExpression", "#[mel:['123-333-33','333-333-333']]",
                                   allOf((Matcher) hasSize(2), hasItem(is(instanceOf(String.class)))));
  }

  @Test
  public void parameterResolverOfListOfSimpleTypeAsChild() throws Exception {
    assertExpressionResolverWeapon("processAddressBookAsChild", null,
                                   allOf((Matcher) hasSize(2), hasItem(is(instanceOf(String.class)))));
  }

  private void assertExpressionResolverWeapon(String flowName, String expression, Matcher weaponMatcher)
      throws Exception {
    ParameterResolver weaponInfo =
        (ParameterResolver) flowRunner(flowName).run().getMessage().getPayload().getValue();
    assertThat(weaponInfo.getExpression(), is(ofNullable(expression)));
    assertThat(weaponInfo.resolve(), weaponMatcher);
  }
}
