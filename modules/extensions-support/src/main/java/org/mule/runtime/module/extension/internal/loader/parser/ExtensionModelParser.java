/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;

import java.util.List;
import java.util.Optional;

/**
 * Parses the syntactic definition of an {@link ExtensionModel} so that the semantics reflected in it can be extracted in a
 * uniform way, regardless of the actual syntax used by the extension developer.
 * <p>
 * This parser also makes explicit the need for certain {@link ModelProperty model properties} which despite not being explicit in
 * the {@link ExtensionModel} are a must for Mule to be able to execute the extension. A typical example of this is the
 * {@link OperationModelParser#getExecutorModelProperty()}, which allows Mule to instantiate the
 * {@link CompletableComponentExecutor} which will bring the operation to life.
 * <p>
 * This and all component parsers access through it ({@link ConfigurationModelParser}, {@link OperationModelParser}, etc)
 * <b>MUST</b> implement {@link Object#equals(Object)} and {@link Object#hashCode()} so that two parseres which represent the SAME
 * component or element are considered to be equal.
 *
 * @see ConfigurationModelParser
 * @see OperationModelParser
 * @see SourceModelParser
 * @see ConnectionProviderModelParser
 * @see FunctionModelParser
 * @see ParameterModelParser
 * @since 4.5.0
 */
public interface ExtensionModelParser {

  /**
   * @return The Extension's Name
   */
  String getName();

  /**
   * @return The Extension's {@link Category}
   */
  Category getCategory();

  /**
   * @return The Extension's Vendor
   */
  String getVendor();

  /**
   * @return a list with a {@link ConfigurationModelParser} per each configuration defined in the extension.
   */
  List<ConfigurationModelParser> getConfigurationParsers();

  /**
   * @return a list with a {@link OperationModelParser} per each operation defined in the extension.
   */
  List<OperationModelParser> getOperationModelParsers();

  /**
   * @return a list with a {@link SourceModelParser} per each event source defined in the extension.
   */
  List<SourceModelParser> getSourceModelParsers();

  /**
   * @return a list with a {@link ConnectionProviderModelParser} per each connection provider defined in the extension.
   */
  List<ConnectionProviderModelParser> getConnectionProviderModelParsers();

  /**
   * @return a list with a {@link FunctionModelParser} per each expression function defined in the extension.
   */
  List<FunctionModelParser> getFunctionModelParsers();

  /**
   * @return a {@link LicenseModelProperty} which describes the extension's licensing.
   */
  LicenseModelProperty getLicenseModelProperty();

  /**
   * @return a list with an {@link ExternalLibraryModel} per each external library defined at the extension level.
   */
  List<ExternalLibraryModel> getExternalLibraryModels();

  /**
   * @return an {@link Optional} {@link ExceptionHandlerModelProperty} is an exception handler was defined at the extension level.
   */
  Optional<ExceptionHandlerModelProperty> getExtensionHandlerModelProperty();

  /**
   * Returns a list with all the {@link ModelProperty model properties} to be applied at the extension level which are
   * specifically linked to the type of syntax used to define the extension.
   *
   * @return a list with {@link ModelProperty} instances.
   */
  List<ModelProperty> getAdditionalModelProperties();

}
