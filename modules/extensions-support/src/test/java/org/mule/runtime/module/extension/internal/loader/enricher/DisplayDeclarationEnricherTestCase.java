/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.LAB_ADDRESS_EXAMPLE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.PARAMETER_OVERRIDED_DISPLAY_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_PACKS_SUMMARY;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.DOOR_PARAMETER;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.GREETING_PARAMETER;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.KNOCKEABLE_DOORS_SUMMARY;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.OPERATION_PARAMETER_EXAMPLE;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DisplayDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final String PARAMETER_GROUP_DISPLAY_NAME = "Date of decease";
  private static final String PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME = "dateOfDeath";
  private ExtensionDeclarer declarer;

  @Before
  public void setUp() {
    final DefaultJavaModelLoaderDelegate loader =
        new DefaultJavaModelLoaderDelegate(HeisenbergExtension.class, getProductVersion());
    declarer = loader.declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new DisplayDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
  }

  @Test
  public void parseDisplayAnnotationsOnParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterDisplayName(findParameter(parameters, PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME),
                               PARAMETER_OVERRIDED_DISPLAY_NAME);
  }

  @Test
  public void parseDisplayNameAnnotationOnParameterGroup() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterDisplayName(findParameter(parameters, PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME), PARAMETER_GROUP_DISPLAY_NAME);
  }

  @Test
  public void parseDisplayNameAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_DISPLAY_NAME_PARAMETER);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getAllParameters();

    assertParameterDisplayName(findParameter(parameters, OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME),
                               OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME);
  }

  @Test
  public void parseSummaryAnnotationOnConfigParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterSummary(findParameter(parameters, "ricinPacks"), RICIN_PACKS_SUMMARY);
  }

  @Test
  public void parseSummaryAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_SUMMARY);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getAllParameters();

    assertParameterSummary(findParameter(parameters, DOOR_PARAMETER), KNOCKEABLE_DOORS_SUMMARY);
  }

  @Test
  public void parseExampleAnnotationOnConfigParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterExample(findParameter(parameters, "labAddress"), LAB_ADDRESS_EXAMPLE);
  }

  @Test
  public void parseExampleAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_EXAMPLE);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getAllParameters();

    assertParameterExample(findParameter(parameters, GREETING_PARAMETER), OPERATION_PARAMETER_EXAMPLE);
  }

  private void assertParameterDisplayName(ParameterDeclaration param, String displayName) {
    DisplayModel display = param.getDisplayModel();
    assertThat(display.getDisplayName(), is(displayName));
  }

  private void assertParameterSummary(ParameterDeclaration param, String summary) {
    DisplayModel display = param.getDisplayModel();
    assertThat(display.getSummary(), is(summary));
  }

  private void assertParameterExample(ParameterDeclaration param, String example) {
    DisplayModel display = param.getDisplayModel();
    assertThat(display.getExample(), is(example));
  }

  private OperationDeclaration getOperation(WithOperationsDeclaration declaration, final String operationName) {
    return (OperationDeclaration) find(declaration.getOperations(),
                                                       object -> ((OperationDeclaration) object).getName().equals(operationName));
  }

  private ParameterDeclaration findParameter(List<ParameterDeclaration> parameters, final String name) {
    return (ParameterDeclaration) find(parameters, object -> name.equals(((ParameterDeclaration) object).getName()));
  }
}
