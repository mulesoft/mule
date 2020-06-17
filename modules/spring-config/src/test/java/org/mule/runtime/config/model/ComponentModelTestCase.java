/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.model;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.DefaultComponentParameterAst;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class ComponentModelTestCase extends AbstractMuleTestCase {

  private static final String PARAMETER_A = "a";
  private static final String PARAMETER_B = "b";
  private static final String PARAMETER_C = "c";
  private ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

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
    ComponentModel componentModel = new ComponentModel();
    org.mule.runtime.api.meta.model.ComponentModel parameterizedModel = createParameterizedModel();
    parameterizedModel.getAllParameterModels().forEach(p -> componentModel.setParameter(p,
                                                                                        new DefaultComponentParameterAst("value-"
                                                                                            + p.getName(), () -> p)));
    componentModel.setComponentModel(parameterizedModel);

    Collection<ComponentParameterAst> parameters = componentModel.getParameters();
    assertThat(parameters, not(empty()));
    assertThat(parameters.size(), is(3));

    List<String> expectedParametersName =
        parameterizedModel.getAllParameterModels().stream().map(NamedObject::getName).collect(toList());
    List<String> parametersName = parameters.stream().map(p -> p.getModel().getName()).collect(toList());
    assertThat(parametersName, is(expectedParametersName));

  }
}
