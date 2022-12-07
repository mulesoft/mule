/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ComponentParameterizationUtils.createComponentParameterization;
import static org.powermock.api.mockito.PowerMockito.when;

import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ComponentParameterizationUtilsTestCase {

  private static final String PARAMETER_NAME = "parameterName";
  private static final String ANOTHER_PARAMETER_NAME = "anotherParameterName";

  private static final String PARAMETER_GROUP_NAME = "parameterName";
  private static final String ANOTHER_PARAMETER_GROUP_NAME = "anotherParameterName";

  private static final String PARAMETER_VALUE = "parameterValue";
  private static final String ANOTHER_PARAMETER_VALUE = "anotherParameterValue";

  @Mock
  private ParameterizedModel parameterizedModelMock;

  @Mock
  private ParameterGroupModel parameterGroupModelMock;

  @Mock
  private ParameterGroupModel anotherParameterGroupModelMock;

  @Mock
  private ParameterModel parameterModelMock;

  @Mock
  private ParameterModel anotherParameterModelMock;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() {
    when(parameterModelMock.getName()).thenReturn(PARAMETER_NAME);
    when(anotherParameterModelMock.getName()).thenReturn(ANOTHER_PARAMETER_NAME);
    when(parameterGroupModelMock.getName()).thenReturn(PARAMETER_GROUP_NAME);
    when(anotherParameterGroupModelMock.getName()).thenReturn(ANOTHER_PARAMETER_GROUP_NAME);
    when(parameterizedModelMock.getParameterGroupModels())
        .thenReturn(asList(parameterGroupModelMock, anotherParameterGroupModelMock));
    when(parameterGroupModelMock.getParameter(PARAMETER_NAME)).thenReturn(of(parameterModelMock));
    when(anotherParameterGroupModelMock.getParameter(ANOTHER_PARAMETER_NAME)).thenReturn(of(anotherParameterModelMock));
  }

  @Test
  public void parameterValueIsMappedCorrectlyToEachParameterGroup() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PARAMETER_NAME, PARAMETER_VALUE);
    parameters.put(ANOTHER_PARAMETER_NAME, ANOTHER_PARAMETER_VALUE);
    ComponentParameterization componentParameterization = createComponentParameterization(parameterizedModelMock, parameters);
    assertThat(componentParameterization.getParameter(PARAMETER_GROUP_NAME, PARAMETER_NAME), is(PARAMETER_VALUE));
    assertThat(componentParameterization.getParameter(ANOTHER_PARAMETER_GROUP_NAME, ANOTHER_PARAMETER_NAME),
               is(ANOTHER_PARAMETER_VALUE));
  }

  @Test
  public void parameterizedModelWithDuplicatedParameterName() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Parameter exists in more than one group");
    when(anotherParameterGroupModelMock.getParameter(PARAMETER_NAME)).thenReturn(of(anotherParameterModelMock));
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PARAMETER_NAME, PARAMETER_VALUE);
    createComponentParameterization(parameterizedModelMock, parameters);
  }

}
