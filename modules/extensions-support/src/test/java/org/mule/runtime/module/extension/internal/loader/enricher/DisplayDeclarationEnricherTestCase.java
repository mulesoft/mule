/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.ANY;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.DIRECTORY;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
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

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.marvel.MissileProvider;
import org.mule.test.marvel.ironman.IronMan;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.Optional;

public class DisplayDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final DslResolvingContext DSL_CTX = getDefault(emptySet());
  private static final String PARAMETER_GROUP_DISPLAY_NAME = "Date of decease";
  private static final String PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME = "dateOfDeath";
  private ExtensionDeclarer heisenbergDeclarer;
  private ExtensionDeclarer marvelDeclarer;

  @Before
  public void setUp() {
    String version = getProductVersion();
    ClassLoader cl = getClass().getClassLoader();
    DefaultExtensionLoadingContext loadingCtx = new DefaultExtensionLoadingContext(cl, DSL_CTX);
    DefaultJavaModelLoaderDelegate heisenbergLoader = new DefaultJavaModelLoaderDelegate(HeisenbergExtension.class, version);
    DefaultJavaModelLoaderDelegate marvelLoader = new DefaultJavaModelLoaderDelegate(MarvelExtension.class, version);
    heisenbergDeclarer = heisenbergLoader.declare(loadingCtx);
    marvelDeclarer = marvelLoader.declare(loadingCtx);
    DisplayDeclarationEnricher enricher = new DisplayDeclarationEnricher();
    enricher.enrich(new DefaultExtensionLoadingContext(heisenbergDeclarer, cl, DSL_CTX));
    enricher.enrich(new DefaultExtensionLoadingContext(marvelDeclarer, cl, DSL_CTX));
  }

  @Test
  public void parseDisplayAnnotationsOnParameter() {
    ExtensionDeclaration extensionDeclaration = heisenbergDeclarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterDisplayName(findParameter(parameters, PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME),
                               PARAMETER_OVERRIDED_DISPLAY_NAME);
  }

  @Test
  public void parseDisplayNameAnnotationOnParameterGroup() {
    ExtensionDeclaration extensionDeclaration = heisenbergDeclarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterDisplayName(findParameter(parameters, PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME), PARAMETER_GROUP_DISPLAY_NAME);
  }

  @Test
  public void parseDisplayNameAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = heisenbergDeclarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_DISPLAY_NAME_PARAMETER);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getAllParameters();

    assertParameterDisplayName(findParameter(parameters, OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME),
                               OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME);
  }

  @Test
  public void parseSummaryAnnotationOnConfigParameter() {
    ExtensionDeclaration extensionDeclaration = heisenbergDeclarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterSummary(findParameter(parameters, "ricinPacks"), RICIN_PACKS_SUMMARY);
  }

  @Test
  public void parseSummaryAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = heisenbergDeclarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_SUMMARY);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getAllParameters();

    assertParameterSummary(findParameter(parameters, DOOR_PARAMETER), KNOCKEABLE_DOORS_SUMMARY);
  }

  @Test
  public void parseExampleAnnotationOnConfigParameter() {
    ExtensionDeclaration extensionDeclaration = heisenbergDeclarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getAllParameters();

    assertParameterExample(findParameter(parameters, "labAddress"), LAB_ADDRESS_EXAMPLE);
  }

  @Test
  public void parseExampleAnnotationOnOperationParameter() {
    ExtensionDeclaration extensionDeclaration = heisenbergDeclarer.getDeclaration();
    OperationDeclaration operation =
        getOperation(extensionDeclaration, HeisenbergOperations.OPERATION_WITH_EXAMPLE);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getAllParameters();

    assertParameterExample(findParameter(parameters, GREETING_PARAMETER), OPERATION_PARAMETER_EXAMPLE);
  }

  @Test
  public void parsePathParameterWithFileExtensions() {
    ExtensionDeclaration declaration = marvelDeclarer.getDeclaration();
    OperationDeclaration findInstructionsOperation = getOperation(declaration, "findInstructions");
    List<ParameterDeclaration> params = findInstructionsOperation.getAllParameters();
    assertThat(params, hasSize(1));

    ParameterDeclaration pathParam = params.get(0);
    Optional<PathModel> pathModel = pathParam.getDisplayModel().getPathModel();
    assertThat(pathModel.isPresent(), is(true));
    assertThat(pathModel.get().getType(), is(ANY));
    assertThat(pathModel.get().acceptsUrls(), is(false));
    assertThat(pathModel.get().getFileExtensions(), hasItem("xml"));
  }

  @Test
  public void parsePathParameterThatIsDirectory() {
    ExtensionDeclaration declaration = marvelDeclarer.getDeclaration();
    ConfigurationDeclaration config = findConfigByName(declaration, IronMan.CONFIG_NAME);
    ConnectionProviderDeclaration missileProvider = findProviderByName(config, MissileProvider.NAME);

    List<ParameterDeclaration> params = missileProvider.getAllParameters();
    assertThat(params, hasSize(1));

    ParameterDeclaration pathParam = params.get(0);
    Optional<PathModel> pathModel = pathParam.getDisplayModel().getPathModel();
    assertThat(pathModel.isPresent(), is(true));
    assertThat(pathModel.get().getType(), is(DIRECTORY));
    assertThat(pathModel.get().acceptsUrls(), is(false));
    assertThat(pathModel.get().getFileExtensions(), empty());
  }

  @Test
  public void parseSimplePathParameter() {
    ExtensionDeclaration declaration = marvelDeclarer.getDeclaration();
    ConfigurationDeclaration config = findConfigByName(declaration, IronMan.CONFIG_NAME);

    List<ParameterDeclaration> params = config.getAllParameters();
    assertThat(params, hasSize(1));

    ParameterDeclaration pathParam = params.get(0);
    Optional<PathModel> pathModel = pathParam.getDisplayModel().getPathModel();
    assertThat(pathModel.isPresent(), is(true));
    assertThat(pathModel.get().getType(), is(ANY));
    assertThat(pathModel.get().acceptsUrls(), is(true));
    assertThat(pathModel.get().getFileExtensions(), empty());
  }

  private ConfigurationDeclaration findConfigByName(ExtensionDeclaration declaration, String name) {
    return declaration.getConfigurations().stream().filter(c -> c.getName().equals(name)).findAny().get();
  }

  private ConnectionProviderDeclaration findProviderByName(ConnectedDeclaration<?> declaration, String name) {
    return declaration.getConnectionProviders().stream().filter(c -> c.getName().equals(name)).findAny().get();
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
