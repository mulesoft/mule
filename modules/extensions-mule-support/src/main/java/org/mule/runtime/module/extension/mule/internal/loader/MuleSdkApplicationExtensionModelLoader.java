/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_APPLICATION_LOADER_ID;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.enricher.BooleanParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkApplicationExtensionModelParserFactory;

import java.util.List;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Extensions defined as part of the same artifact.
 *
 * @since 4.5.0
 */
public class MuleSdkApplicationExtensionModelLoader extends AbstractExtensionModelLoader {

  private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
                                                                                               new BooleanParameterDeclarationEnricher()));

  @Override
  public String getId() {
    return MULE_SDK_APPLICATION_LOADER_ID;
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new MuleSdkApplicationExtensionModelParserFactory();
  }

  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    super.configureContextBeforeDeclaration(context);
    context.addCustomDeclarationEnrichers(customDeclarationEnrichers);
  }
}
