/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.validation.IgnoredExtensionParameterModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.hamcrest.Matcher;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class IgnoredExtensionParameterModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private ExtensionElement extensionElement;

  @Mock
  private ConfigurationElement configurationElement;

  @Mock
  private ExtensionParameter extensionParameter;

  @Mock
  private ExtensionParameter anotherExtensionParameter;

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private IgnoredExtensionParameterModelValidator validator = new IgnoredExtensionParameterModelValidator();

  ProblemsReporter problemsReporter;

  @Before
  public void setup() {
    when(extensionElement.getParameters()).thenReturn(Collections.singletonList(extensionParameter));
    when(extensionElement.getParameterGroups()).thenReturn(Collections.singletonList(anotherExtensionParameter));
    when(extensionElement.getConfigurations()).thenReturn(Collections.singletonList(configurationElement));

    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(Optional.of(new CompileTimeModelProperty()));
    when(extensionModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class))
        .thenReturn(of(new ExtensionTypeDescriptorModelProperty(extensionElement)));

    problemsReporter = new ProblemsReporter(extensionModel);
  }

  @Test
  public void extensionWithParameterAndParameterGroupAndConfigurations() {
    validator.validate(extensionModel, problemsReporter);
    assertThat(problemsReporter.getErrors(), hasSize(0));
    assertThat(problemsReporter.getWarnings(), hasSize(2));
    assertThat(problemsReporter.getWarnings(), containsInAnyOrder(isWarning("parameter"),
                                                                  isWarning("parameter group")));
  }

  @Test
  public void extensionWithParameterAndConfigurations() {
    when(extensionElement.getParameterGroups()).thenReturn(Collections.emptyList());
    validator.validate(extensionModel, problemsReporter);
    assertThat(problemsReporter.getErrors(), hasSize(0));
    assertThat(problemsReporter.getWarnings(), hasSize(1));
    assertThat(problemsReporter.getWarnings(), contains(isWarning("parameter")));
  }

  @Test
  public void extensionWithParameterGroupAndConfigurations() {
    when(extensionElement.getParameters()).thenReturn(Collections.emptyList());
    validator.validate(extensionModel, problemsReporter);
    assertThat(problemsReporter.getErrors(), hasSize(0));
    assertThat(problemsReporter.getWarnings(), hasSize(1));
    assertThat(problemsReporter.getWarnings(), contains(isWarning("parameter group")));
  }

  @Test
  public void extensionWitNoParameterNorParameterGroupAndConfigurations() {
    when(extensionElement.getParameters()).thenReturn(Collections.emptyList());
    when(extensionElement.getParameterGroups()).thenReturn(Collections.emptyList());
    validator.validate(extensionModel, problemsReporter);
    assertThat(problemsReporter.getErrors(), hasSize(0));
    assertThat(problemsReporter.getWarnings(), hasSize(0));
  }

  @Test
  public void extensionWithParameterAndConfigurationsNotAtCompileTime() {
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(Optional.empty());
    validator.validate(extensionModel, problemsReporter);
    assertThat(problemsReporter.getErrors(), hasSize(0));
    assertThat(problemsReporter.getWarnings(), hasSize(0));
  }

  private Matcher isWarning(String parameterType) {
    return allOf(hasProperty("message", containsString(parameterType + "(s) will be ignored")),
                 hasProperty("component", is(extensionModel)));
  }
}
