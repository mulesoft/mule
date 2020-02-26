/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.mule.runtime.core.api.config.MuleProperties.PROPERTY_PREFIX;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

/**
 * Constants for the Extensions Framework
 *
 * @since 4.0
 */
public class ExtensionProperties {

  /**
   * The key of an execution context variable on which the connection to be used was set
   */
  public static final String CONNECTION_PARAM = PROPERTY_PREFIX + "CONNECTION_PARAM";

  /**
   * The key of an execution context variable on which a {@link SourceCallbackContext} was set
   */
  public static final String SOURCE_CALLBACK_CONTEXT_PARAM = PROPERTY_PREFIX + "SOURCE_CALLBACK_CONTEXT_PARAM";

  /**
   * The key of an execution context variable on which a {@link SourceCompletionCallback} was set
   */
  public static final String SOURCE_COMPLETION_CALLBACK_PARAM = PROPERTY_PREFIX + "SOURCE_COMPLETION_CALLBACK_PARAM";

  /**
   * The key of an execution context variable which holds the back pressure action that was applied on an event
   *
   * @since 4.1
   */
  public static final String BACK_PRESSURE_ACTION_CONTEXT_PARAM = PROPERTY_PREFIX + "BACK_PRESSURE_ACTION_CONTEXT_PARAM";

  /**
   * The name of a parameter that allows configuring the mimeType that should be applied
   */
  public static final String MIME_TYPE_PARAMETER_NAME = "outputMimeType";

  /**
   * The name of a parameter that allows configuring the encoding that should be applied
   */
  public static final String ENCODING_PARAMETER_NAME = "outputEncoding";

  /**
   * The name mask of a file which contains the {@link ExtensionModel} elements descriptions.
   * <p>
   * The final name of the file is formed like this: {extension-namespace}-extension-descriptions.xml.
   */
  public static final String EXTENSION_DESCRIPTIONS_FILE_NAME_MASK = "%s-extension-descriptions.xml";

  /**
   * The name of a file which contains a json representation of the extension's model
   */
  public static final String EXTENSION_MODEL_JSON_FILE_NAME = "extension-model.json";

  /**
   * The name of the tab in which advanced parameters should appear
   */
  public static final String ADVANCED_TAB_NAME = "Advanced";


  public static final String DEFAULT_CONNECTION_PROVIDER_NAME = "connection";

  /**
   * The name to be used in a property of a {@link ValueResolvingContext} to store the {@link ConfigurationModel} being
   * used in that context.
   * @since 4.3.0
   */
  public static final String CONFIGURATION_MODEL_PROPERTY_NAME = "Configuration model";

  /**
   * The key of an execution context variable indicating that RETRY should not be attempted
   * @since 4.1.6 - 4.2.1 - 4.3.0
   */
  public static final String DO_NOT_RETRY = PROPERTY_PREFIX + "DO_NOT_RETRY";

  /**
   * The key of a property or variable indicating that a component was executed while participating in a transaction
   * @since 4.2.3 - 4.3.0
   */
  public static final String IS_TRANSACTIONAL = PROPERTY_PREFIX + "IS_TRANSACTIONAL";

  /**
   * The key of a property or variable holding the name of a component's config object
   * @since 4.2.3 - 4.3.0
   */
  public static final String COMPONENT_CONFIG_NAME = PROPERTY_PREFIX + "COMPONENT_CONFIG_NAME";


  private ExtensionProperties() {}
}
