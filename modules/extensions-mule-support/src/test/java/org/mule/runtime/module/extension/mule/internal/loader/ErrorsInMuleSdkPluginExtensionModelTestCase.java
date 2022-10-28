/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.EXTENSION_EXTENSION_MODEL;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.mule.internal.loader.ast.AbstractMuleSdkAstTestCase;
import org.mule.runtime.module.extension.mule.internal.operation.FakeExpressionLanguageMetadataService;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Test;

@Feature(REUSE)
@Stories({@Story(EXTENSION_EXTENSION_MODEL), @Story(ERROR_HANDLING)})
public class ErrorsInMuleSdkPluginExtensionModelTestCase extends AbstractMuleSdkAstTestCase {

  private static final ExpressionLanguageMetadataService expressionLanguageMetadataService =
      new FakeExpressionLanguageMetadataService();
  private static final ArtifactCoordinates TEST_ARTIFACT_COORDINATES = new BundleDescriptor.Builder()
      .setArtifactId("TestExtension")
      .setGroupId("TestGroup")
      .setVersion("1.2.3")
      .build();

  private static final Map<String, Set<String>> expectedErrors = new HashMap<>();

  @BeforeClass
  public static void setupExpectedErrors() {
    expectedErrors.put("raiseCustom", singleton("ERRORS:CUSTOM"));
    expectedErrors.put("raiseOther", singleton("ERRORS:OTHER"));
    expectedErrors.put("silencingOneAndRaisingOther", singleton("ERRORS:OTHER"));

    expectedErrors.put("heisenbergCureCancer", asSet("ERRORS:HEISENBERG_HEALTH", "ERRORS:HEISENBERG_OAUTH2"));

    runtimeExtensionModels.addAll(getDependencyExtensions());
  }

  @Override
  protected String getConfigFile() {
    return null;
  }

  @Test
  public void allErrorsOnExtensionModel() {
    ExtensionModel extensionModel = getExtensionModelFrom("extensions/extension-with-errors.xml");
    Set<String> errorsAsString = getRaisedErrors(extensionModel);
    MatcherAssert.assertThat(errorsAsString,
                             containsInAnyOrder("MULE:ANY", "MULE:RETRY_EXHAUSTED", "ERRORS:CUSTOM", "ERRORS:CONNECTIVITY",
                                                "ERRORS:RETRY_EXHAUSTED", "MULE:CONNECTIVITY", "ERRORS:ONE", "ERRORS:OTHER",
                                                "ERRORS:HEISENBERG_HEALTH", "ERRORS:HEISENBERG_OAUTH2"));
  }

  @Test
  public void eachOperationDeclaresTheErrorsThatRaises() {
    ExtensionModel extensionModel = getExtensionModelFrom("extensions/extension-with-errors.xml");
    for (Entry<String, Set<String>> expectedForOperation : expectedErrors.entrySet()) {
      String operationName = expectedForOperation.getKey();
      Set<String> expected = expectedForOperation.getValue();
      assertRaisedErrors(extensionModel, operationName, expected);
    }
  }

  // TODO: Extract!
  private void assertRaisedErrors(ExtensionModel extensionModel, String operationName, Collection<String> expectedSetOfErrors) {
    OperationModel operationModel = getOperationModel(extensionModel, operationName);
    Set<String> actualRaisedErrors = getRaisedErrors(operationModel);

    assertThat(format("Actual set for '%s': %s", operationName, actualRaisedErrors), actualRaisedErrors,
               hasSize(expectedSetOfErrors.size()));
    for (String item : expectedSetOfErrors) {
      assertThat(format("Actual set for '%s': %s", operationName, actualRaisedErrors), actualRaisedErrors, hasItem(item));
    }
  }

  // TODO: Extract!
  private OperationModel getOperationModel(ExtensionModel extensionModel, String operationName) {
    return extensionModel.getOperationModel(operationName)
        .orElseThrow(() -> new IllegalArgumentException(format("Operation '%s' not found in application's extension model",
                                                               operationName)));
  }

  // TODO: Extract!
  private static Set<String> getRaisedErrors(ExtensionModel extensionModel) {
    return extensionModel.getErrorModels().stream().map(ErrorModel::toString).collect(toSet());
  }

  // TODO: Extract!
  private static Set<String> getRaisedErrors(OperationModel operationModel) {
    return operationModel.getErrorModels().stream().map(ErrorModel::toString).collect(toSet());
  }

  // TODO: Extract!!
  private ExtensionModel getExtensionModelFrom(String extensionFile) {
    return getExtensionModelFrom(extensionFile, this.getClass().getClassLoader());
  }

  // TODO: Extract!!
  private ExtensionModel getExtensionModelFrom(String extensionFile, ClassLoader classLoader) {
    ExtensionModelLoadingRequest loadingRequest = builder(classLoader, getDefault(runtimeExtensionModels))
        .addParameter(VERSION_PROPERTY_NAME, TEST_ARTIFACT_COORDINATES.getVersion())
        .addParameter(MULE_SDK_RESOURCE_PROPERTY_NAME, extensionFile)
        .addParameter(MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME, expressionLanguageMetadataService)
        .setArtifactCoordinates(TEST_ARTIFACT_COORDINATES)
        .build();
    return new MuleSdkPluginExtensionModelLoader().loadExtensionModel(loadingRequest);
  }

  // TODO: Extract!!
  private static <T> Set<T> asSet(T... a) {
    return stream(a).collect(toSet());
  }

  private static Set<ExtensionModel> getDependencyExtensions() {
    ExtensionModel petstore = loadExtension(HeisenbergExtension.class, emptySet());
    return asSet(petstore);
  }

  private static ExtensionModel loadExtension(Class<?> extensionClass, Set<ExtensionModel> deps) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put(TYPE_PROPERTY_NAME, extensionClass.getName());
    ctx.put(VERSION, "1.0.0-SNAPSHOT");
    return new DefaultJavaExtensionModelLoader()
        .loadExtensionModel(currentThread().getContextClassLoader(), DslResolvingContext.getDefault(deps), ctx);
  }
}
