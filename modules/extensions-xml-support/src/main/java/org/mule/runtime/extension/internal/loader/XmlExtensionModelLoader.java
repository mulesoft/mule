/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.lang.String.format;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

/**
 * Implementation of {@link ExtensionModelLoader} for those plugins that have an ID that matches with {@link #DESCRIBER_ID},
 * which implies that are extensions built through XML.
 *
 * @since 4.0
 */
public class XmlExtensionModelLoader extends ExtensionModelLoader {

  /**
   * Attribute to look for in the parametrized attributes picked up from the descriptor.
   */
  public static final String RESOURCE_XML = "resource-xml";

  /**
   * The ID which represents {@code this} loader that will be used to execute the lookup when reading the descriptor file.
   * @see MulePluginModel#getExtensionModelLoaderDescriptor()
   */
  public static final String DESCRIBER_ID = "xml-based";

  @Override
  public String getId() {
    return DESCRIBER_ID;
  }

  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    final String modulePath = context.<String>getParameter(RESOURCE_XML)
        .orElseThrow(() -> new IllegalArgumentException(format("The attribute '%s' is missing", RESOURCE_XML)));

    final XmlExtensionLoaderDelegate delegate = new XmlExtensionLoaderDelegate(modulePath);
    delegate.declare(context);
  }
}
