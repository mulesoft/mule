/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ANY;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ERROR_HANDLER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule's Runtime
 *
 * @since 4.0
 */
public class MuleExtensionModelProvider {

  public static final String MULE_NAME = "Mule Core";
  public static final String MULE_VERSION = getProductVersion();

  public static final StereotypeModel ANY_STEREOTYPE = newStereotype(ANY.getName(), CORE_PREFIX.toUpperCase()).build();
  public static final StereotypeModel PROCESSOR_STEREOTYPE = newStereotype(PROCESSOR.getName(), CORE_PREFIX.toUpperCase())
      .withParent(ANY_STEREOTYPE).build();
  public static final StereotypeModel ERROR_HANDLER_STEREOTYPE = newStereotype(ERROR_HANDLER.getName(), CORE_PREFIX.toUpperCase())
      .withParent(ANY_STEREOTYPE).build();
  public static final StereotypeModel SOURCE_STEREOTYPE = newStereotype(SOURCE.getName(), CORE_PREFIX.toUpperCase())
      .withParent(ANY_STEREOTYPE).build();
  public static final StereotypeModel FLOW_STEREOTYPE = newStereotype(FLOW.getName(), CORE_PREFIX.toUpperCase())
      .withParent(ANY_STEREOTYPE).build();

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(new DefaultExtensionLoadingContext(new MuleExtensionModelDeclarer().createExtensionModel(),
                                                 MuleExtensionModelProvider.class.getClassLoader(),
                                                 new NullDslResolvingContext())));

  /**
   * @return the {@link ExtensionModel} definition for Mule's Runtime
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }
}
