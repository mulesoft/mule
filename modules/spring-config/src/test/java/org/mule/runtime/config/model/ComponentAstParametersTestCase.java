/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.model;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.DefaultComponentParameterAst;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class ComponentAstParametersTestCase extends AbstractMuleTestCase {

  private static final String PARAMETER_A = "a";
  private static final String PARAMETER_B = "b";
  private static final String PARAMETER_C = "c";
  private final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Rule
  public ExpectedException expectedException = none();

  private org.mule.runtime.api.meta.model.ComponentModel createParameterizedModel() {
    org.mule.runtime.api.meta.model.ComponentModel parameterizedModel =
        mock(org.mule.runtime.api.meta.model.ComponentModel.class);
    List<ParameterModel> parameterModels = new ArrayList<>();
    parameterModels.add(createParameterModel(PARAMETER_A));
    parameterModels.add(createParameterModel(PARAMETER_B));
    parameterModels.add(createParameterModel(PARAMETER_C));
    when(parameterizedModel.getAllParameterModels()).thenReturn(parameterModels);
    return parameterizedModel;
  }

  private ParameterModel createParameterModel(String name) {
    ParameterModel parameterModel = mock(ParameterModel.class);
    when(parameterModel.getName()).thenReturn(name);
    when(parameterModel.getType()).thenReturn(TYPE_LOADER.load(String.class));
    return parameterModel;
  }

  @Test
  public void retrieveParametersAsDefinedAtExtensionModel() {
    ComponentModel componentModel = baseComponentModelBuilder().build();
    org.mule.runtime.api.meta.model.ComponentModel parameterizedModel = createParameterizedModel();
    parameterizedModel.getAllParameterModels().forEach(p -> componentModel.setParameter(p,
                                                                                        new DefaultComponentParameterAst("value-"
                                                                                            + p.getName(), () -> p)));
    componentModel.setComponentModel(parameterizedModel);

    Collection<ComponentParameterAst> parameters = componentModel.getParameters();
    assertThat(parameters, hasSize(3));

    List<String> expectedParametersName =
        parameterizedModel.getAllParameterModels().stream().map(NamedObject::getName).collect(toList());
    List<String> parametersName = parameters.stream().map(p -> p.getModel().getName()).collect(toList());
    assertThat(parametersName, is(expectedParametersName));
  }

  @Test
  public void getParametersShouldFailIfParameterizedModelIsNotPresent() {
    expectedException.expectMessage(containsString("is not parameterizable"));
    expectedException.expect(IllegalStateException.class);
    ComponentModel componentModel = baseComponentModelBuilder().build();
    componentModel.getParameters();
  }

  @Test
  @Issue("MULE-18513")
  public void paramsInShowDslGroupProperlyProcessed() {
    org.mule.runtime.api.meta.model.ComponentModel parameterizedModel =
        mock(org.mule.runtime.api.meta.model.ComponentModel.class);

    final ParameterModel paramA = createParameterModel(PARAMETER_A);
    final ParameterModel paramB = createParameterModel(PARAMETER_B);
    final ParameterModel paramC = createParameterModel(PARAMETER_C);

    when(parameterizedModel.getAllParameterModels()).thenReturn(asList(paramA, paramB, paramC));

    final ParameterGroupModel paramGroupA = mock(ParameterGroupModel.class);
    when(paramGroupA.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(paramGroupA.getParameterModels()).thenReturn(singletonList(paramA));
    when(paramGroupA.isShowInDsl()).thenReturn(false);
    final ParameterGroupModel paramGroupB = mock(ParameterGroupModel.class);
    when(paramGroupB.getName()).thenReturn("groupB");
    when(paramGroupB.getParameterModels()).thenReturn(singletonList(paramB));
    when(paramGroupB.isShowInDsl()).thenReturn(false);
    final ParameterGroupModel paramGroupC = mock(ParameterGroupModel.class);
    when(paramGroupC.getName()).thenReturn("groupC");
    when(paramGroupC.getParameterModels()).thenReturn(singletonList(paramC));
    when(paramGroupC.isShowInDsl()).thenReturn(true);

    when(parameterizedModel.getParameterGroupModels()).thenReturn(asList(paramGroupA, paramGroupB, paramGroupC));

    ComponentModel componentModel = baseComponentModelBuilder()
        .addParameter(PARAMETER_A, "a", false)
        .addParameter(PARAMETER_B, "b", false)
        .addChildComponentModel(baseComponentModelBuilder()
            .setIdentifier(buildFromStringRepresentation("test:group-c"))
            .addParameter(PARAMETER_C, "c", false)
            .build())
        .build();
    componentModel.setComponentModel(parameterizedModel);

    Collection<ComponentParameterAst> parameters = componentModel.getParameters();

    assertThat(parameters, hasSize(3));
    assertThat(parameters.stream().map(p -> p.getValue().getRight()).collect(toList()),
               is(asList("a", "b", "c")));
  }

  private ComponentModel.Builder baseComponentModelBuilder() {
    return new ComponentModel.Builder().setIdentifier(mock(ComponentIdentifier.class));
  }
}
