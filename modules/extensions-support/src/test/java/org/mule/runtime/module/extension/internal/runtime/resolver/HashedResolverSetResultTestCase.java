/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.model.HealthStatus;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class HashedResolverSetResultTestCase extends AbstractMuleTestCase {

  private static final String NAME = "MG";
  private static final int AGE = 31;
  private static final HealthStatus HEALTH = null;

  private ParameterModel nameParameterModel;
  private ParameterModel ageParameterModel;
  private ParameterModel healthParameterModel;

  private ResolverSetResult result;

  @Before
  public void before() throws Exception {
    nameParameterModel = getParameter("myName", String.class);
    ageParameterModel = getParameter("age", Integer.class);
    healthParameterModel = getParameter("initialHealth", HealthStatus.class);

    result = buildResult();
  }

  @Test
  public void testValues() {
    assertResult(result);
  }

  @Test
  public void equals() {
    assertThat(result, equalTo(buildResult()));
  }

  @Test
  public void notEquals() {
    assertThat(result, not(equalTo(agelessResult())));
  }

  @Test
  public void equivalentHashCode() {
    assertThat(result.hashCode(), equalTo(buildResult().hashCode()));
  }

  @Test
  public void nonEquivalentHashCode() {
    assertThat(result.hashCode(), not(equalTo(agelessResult().hashCode())));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullParameter() {
    getBuilder().add(null, "blah");
  }

  @Test
  public void invalidParameter() {
    assertThat(result.get("invalid"), is(nullValue()));
  }

  @Test
  public void invalidParameterName() {
    assertThat(result.get("invalid"), is(nullValue()));
  }

  private void assertResult(ResolverSetResult result) {
    assertThat(result.get(nameParameterModel.getName()), is(NAME));
    assertThat(result.get(ageParameterModel.getName()), is(AGE));
    assertThat(result.get(healthParameterModel.getName()), is(HEALTH));
  }

  private ResolverSetResult buildResult() {
    return getBuilder().build();
  }

  private ResolverSetResult agelessResult() {
    return getBuilder().add(ageParameterModel.getName(), null).build();
  }

  private ResolverSetResult.Builder getBuilder() {
    return HashedResolverSetResult.newBuilder().add(nameParameterModel.getName(), NAME).add(ageParameterModel.getName(), AGE)
        .add(healthParameterModel.getName(), HEALTH);
  }
}
