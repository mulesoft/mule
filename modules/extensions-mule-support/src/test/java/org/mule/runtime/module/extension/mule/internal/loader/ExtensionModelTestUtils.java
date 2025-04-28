/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

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
import org.mule.runtime.module.extension.mule.internal.operation.FakeExpressionLanguageMetadataService;

import java.util.Collection;
import java.util.Set;

/**
 * Utility class providing convenient methods to make test cases more readable.
 */
public final class ExtensionModelTestUtils {

  private ExtensionModelTestUtils() {
    // Empty private constructor in order to avoid incorrect instantiations.
  }

  private static final ExpressionLanguageMetadataService expressionLanguageMetadataService =
      new FakeExpressionLanguageMetadataService();

  public static final ArtifactCoordinates TEST_ARTIFACT_COORDINATES = new BundleDescriptor.Builder()
      .setArtifactId("TestExtension")
      .setGroupId("TestGroup")
      .setVersion("1.2.3")
      .build();

  /**
   * Gets the {@link OperationModel} of an operation from within the {@link ExtensionModel}, by matching the name.
   *
   * @param extensionModel the {@link ExtensionModel} containing the operation.
   * @param operationName  the name of the {@link OperationModel} being searched.
   * @return an {@link OperationModel} with the given name.
   * @throws IllegalArgumentException if the operation wasn't found.
   */
  public static OperationModel getOperationModel(ExtensionModel extensionModel, String operationName) {
    return extensionModel.getOperationModel(operationName)
        .orElseThrow(() -> new IllegalArgumentException(format("Operation '%s' not found in extension model '%s'",
                                                               operationName, extensionModel.getName())));
  }

  /**
   * Gets the errors as string for a given {@link ExtensionModel}. The output strings have the format "NAMESPACE:TYPE".
   *
   * @param extensionModel the {@link ExtensionModel}.
   * @return a set of strings "NAMESPACE:TYPE" for all the errors in the given {@link ExtensionModel}.
   */
  public static Set<String> getRaisedErrors(ExtensionModel extensionModel) {
    return extensionModel.getErrorModels().stream().map(ErrorModel::toString).collect(toSet());
  }

  /**
   * Gets the errors as string for a given {@link OperationModel}. The output strings have the format "NAMESPACE:TYPE".
   *
   * @param operationModel the {@link OperationModel}.
   * @return a set of strings "NAMESPACE:TYPE" for all the errors in the given {@link OperationModel}.
   */
  public static Set<String> getRaisedErrors(OperationModel operationModel) {
    return operationModel.getErrorModels().stream().map(ErrorModel::toString).collect(toSet());
  }

  /**
   * Asserts that the {@link OperationModel} with name {@code operationName} within the {@link ExtensionModel} has the expected
   * set of errors, passed in the parameter {@code expectedSetOfErrors}. The expected errors are passed as Strings, and they must
   * have the format "NAMESPACE:TYPE".
   *
   * @param extensionModel      the {@link ExtensionModel} containing the operation. If doesn't contain the operation, this method
   *                            will raise a {@link IllegalArgumentException}.
   * @param operationName       the name of the {@link OperationModel}.
   * @param expectedSetOfErrors the expected set of errors, indicated with strings which format must be "NAMESPACE:TYPE".
   */
  public static void assertRaisedErrors(ExtensionModel extensionModel, String operationName,
                                        Collection<String> expectedSetOfErrors) {
    OperationModel operationModel = getOperationModel(extensionModel, operationName);
    Set<String> actualRaisedErrors = getRaisedErrors(operationModel);

    assertThat(format("Actual set for '%s': %s", operationName, actualRaisedErrors), actualRaisedErrors,
               hasSize(expectedSetOfErrors.size()));
    for (String item : expectedSetOfErrors) {
      assertThat(format("Actual set for '%s': %s", operationName, actualRaisedErrors), actualRaisedErrors, hasItem(item));
    }
  }

  /**
   * Provides a convenient way to create a set initialized to contain several elements.
   *
   * @param a elements to add to the resulting {@link Set}.
   * @return a {@link Set} containing all the passed arguments.
   * @param <T> the class of the objects in the set.
   */
  public static <T> Set<T> asSet(T... a) {
    return stream(a).collect(toSet());
  }

  /**
   * Creates a MuleSDK plugin's {@link ExtensionModel} for the file passed in the parameter {@code extensionFile}.
   *
   * @param extensionFile name of the file declaring an extension with the MuleSDK.
   * @param classLoader   classLoader to be used to load the plugin.
   * @param dependencies  set of extension models available for the {@link DslResolvingContext}.
   * @return the corresponding {@link ExtensionModel}.
   */
  public static ExtensionModel loadMuleSdkExtension(String extensionFile, ClassLoader classLoader,
                                                    Set<ExtensionModel> dependencies) {
    ExtensionModelLoadingRequest loadingRequest = builder(classLoader, getDefault(dependencies))
        .addParameter(VERSION_PROPERTY_NAME, TEST_ARTIFACT_COORDINATES.getVersion())
        .addParameter(MULE_SDK_RESOURCE_PROPERTY_NAME, extensionFile)
        .addParameter(MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME, expressionLanguageMetadataService)
        .setArtifactCoordinates(TEST_ARTIFACT_COORDINATES)
        .setResolveMinMuleVersion(true)
        .build();
    return new MuleSdkPluginExtensionModelLoader().loadExtensionModel(loadingRequest);
  }

  /**
   * Creates a MuleSDK plugin's {@link ExtensionModel} for the file passed in the parameter {@code extensionFile}, with the thread
   * context class loader.
   *
   * @param extensionFile name of the file declaring an extension with the MuleSDK.
   * @param dependencies  set of extension models available for the {@link DslResolvingContext}.
   * @return the corresponding {@link ExtensionModel}.
   */
  public static ExtensionModel loadMuleSdkExtension(String extensionFile, Set<ExtensionModel> dependencies) {
    return loadMuleSdkExtension(extensionFile, currentThread().getContextClassLoader(), dependencies);
  }

  /**
   * Creates a JavaSDK plugin's {@link ExtensionModel} for the main class passed in the parameter {@code extensionFile}.
   *
   * @param extensionClass main class of the extension.
   * @param classLoader    classLoader to be used to load the plugin.
   * @param dependencies   set of extension models available for the {@link DslResolvingContext}.
   * @return the corresponding {@link ExtensionModel}.
   */
  public static ExtensionModel loadJavaSdkExtension(Class<?> extensionClass, ClassLoader classLoader,
                                                    Set<ExtensionModel> dependencies) {
    ExtensionModelLoadingRequest loadingRequest = builder(classLoader, getDefault(dependencies))
        .addParameter(TYPE_PROPERTY_NAME, extensionClass.getName())
        .addParameter(VERSION, "1.0.0-SNAPSHOT")
        .build();
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(loadingRequest);
  }

  /**
   * Creates a JavaSDK plugin's {@link ExtensionModel} for the main class passed in the parameter {@code extensionFile}, with the
   * thread context class loader.
   *
   * @param extensionClass main class of the extension.
   * @param dependencies   set of extension models available for the {@link DslResolvingContext}.
   * @return the corresponding {@link ExtensionModel}.
   */
  public static ExtensionModel loadJavaSdkExtension(Class<?> extensionClass, Set<ExtensionModel> dependencies) {
    return loadJavaSdkExtension(extensionClass, currentThread().getContextClassLoader(), dependencies);
  }
}
