/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.mule.runtime.core.api.config.MuleProperties.PROPERTY_PREFIX;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;

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
   * The key of an execution context variable which holds the {@link CompletionCallback} that a non blocking component will use to
   * notify completion or failure
   */
  public static final String COMPLETION_CALLBACK_CONTEXT_PARAM = PROPERTY_PREFIX + "COMPLETION_CALLBACK_CONTEXT_PARAM";

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

  private ExtensionProperties() {}
}
