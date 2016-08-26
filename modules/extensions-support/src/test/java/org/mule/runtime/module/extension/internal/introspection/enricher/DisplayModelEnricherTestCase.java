/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.PARAMETER_OVERRIDED_DISPLAY_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_PACKS_SUMMARY;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.DOOR_PARAMETER;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.KNOCKEABLE_DOORS_SUMMARY;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.extension.api.introspection.property.DisplayModelProperty;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DisplayModelEnricherTestCase extends AbstractMuleTestCase {

  private static final String PARAMETER_GROUP_DISPLAY_NAME = "Date of decease";
  private static final String PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME = "dateOfDeath";
  private ExtensionDeclarer declarer;

  @Before
  public void setUp() {
    final AnnotationsBasedDescriber basedDescriber =
        new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver(getProductVersion()));
    declarer = basedDescriber.describe(new DefaultDescribingContext(getClass().getClassLoader()));
    new DisplayModelEnricher().enrich(new DefaultDescribingContext(declarer, this.getClass().getClassLoader()));
  }

  @Test
  public void parseDisplayAnnotationsOnParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getParameters();

    assertParameterDisplayName(findParameter(parameters, PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME),
                               PARAMETER_OVERRIDED_DISPLAY_NAME);
  }

  @Test
  public void parseDisplayNameAnnotationOnParameterGroup() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getParameters();

    assertParameterDisplayName(findParameter(parameters, PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME), PARAMETER_GROUP_DISPLAY_NAME);
  }

  @Test
  public void parseDisplayNameAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_DISPLAY_NAME_PARAMETER);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getParameters();

    assertParameterDisplayName(findParameter(parameters, OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME),
                               OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME);
  }

  @Test
  public void parseSummaryAnnotationOnConfigParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getParameters();

    assertParameterSummary(findParameter(parameters, "ricinPacks"), RICIN_PACKS_SUMMARY);
  }

  @Test
  public void parseSummaryAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_SUMMARY);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getParameters();

    assertParameterSummary(findParameter(parameters, DOOR_PARAMETER), KNOCKEABLE_DOORS_SUMMARY);
  }

  private void assertParameterDisplayName(ParameterDeclaration param, String displayName) {
    DisplayModelProperty display = param.getModelProperty(DisplayModelProperty.class).get();
    assertThat(display.getDisplayName().get(), is(displayName));
  }

  private void assertParameterSummary(ParameterDeclaration param, String summary) {
    DisplayModelProperty display = param.getModelProperty(DisplayModelProperty.class).get();
    assertThat(display.getSummary().get(), is(summary));
  }

  private OperationDeclaration getOperation(WithOperationsDeclaration declaration, final String operationName) {
    return (OperationDeclaration) CollectionUtils.find(declaration.getOperations(),
                                                       object -> ((OperationDeclaration) object).getName().equals(operationName));
  }

  private ParameterDeclaration findParameter(List<ParameterDeclaration> parameters, final String name) {
    return (ParameterDeclaration) CollectionUtils.find(parameters,
                                                       object -> name.equals(((ParameterDeclaration) object).getName()));
  }
}
