/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.mule.runtime.core.api.config.MuleProperties.PROPERTY_PREFIX;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;

/**
 * Constants for the Extensions Framework
 *
 * @since 4.0
 */
public class ExtensionProperties {

  /**
   * The key of an operation's variable on which the connection to be used was set
   */
  public static final String CONNECTION_PARAM = PROPERTY_PREFIX + "CONNECTION_PARAM";

  /**
   * The key of an operation's variable on which the {@link InterceptingCallback} is stored
   */
  public static final String INTERCEPTING_CALLBACK_PARAM = PROPERTY_PREFIX + "INTERCEPTING_CALLBACK_PARAM";

  /**
   * The name of a parameter that allows configuring the mimeType that should be applied
   */
  public static final String MIME_TYPE_PARAMETER_NAME = "outputMimeType";

  /**
   * The name of a parameter that allows configuring the encoding that should be applied
   */
  public static final String ENCODING_PARAMETER_NAME = "outputEncoding";

  /**
   * The name of a synthetic parameter that's automatically added to all non void operations. The meaning of it is requesting the
   * runtime to place the resulting {@link MuleMessage} on a flowVar pointed by this parameter instead of replacing the message
   * carried by the {@link MuleEvent} that's travelling through the pipeline
   */
  public static final String TARGET_ATTRIBUTE = "target";

  /**
   * The name of an attribute which allows referencing a {@link TlsContextFactory}
   */
  public static final String TLS_ATTRIBUTE_NAME = "tlsContext";

  /**
   * The name of an attribute which allows specifying a {@link ThreadingProfile}
   */
  public static final String THREADING_PROFILE_ATTRIBUTE_NAME = "threadingProfile";

  /**
   * The name of a file which contains each plugin's {@link ExtensionManifest}
   */
  public static final String EXTENSION_MANIFEST_FILE_NAME = "extension-manifest.xml";

  /**
   * The name of a file which contains a json representation of the extension's model
   */
  public static final String EXTENSION_MODEL_JSON_FILE_NAME = "extension-model.json";

  /**
   * The name of a property which points to the {@link ClassLoader} that an extension should use
   */
  public static final String EXTENSION_CLASSLOADER = "extension_classloader";

  /**
   * The name of the parameter for configuring transactional actions
   */
  public static final String TRANSACTIONAL_ACTION_PARAMETER_NAME = "transactionalAction";

  /**
   * The description of the parameter for configuring transactional actions
   */
  public static final String TRANSACTIONAL_ACTION_PARAMETER_DESCRIPTION =
      "The type of joining action that operations can take regarding transactions.";

  private ExtensionProperties() {}
}
