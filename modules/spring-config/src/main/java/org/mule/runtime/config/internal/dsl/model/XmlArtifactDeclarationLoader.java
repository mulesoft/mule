/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.config.internal.dsl.declaration.DefaultXmlArtifactDeclarationLoader;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.io.InputStream;

/**
 * Loads a mule XML configuration file into an {@link ArtifactDeclaration} representation.
 *
 * @since 4.0
 */
public interface XmlArtifactDeclarationLoader {

  /**
   * Provides an instance of the default implementation of the {@link XmlArtifactDeclarationLoader}.
   *
   * @param context a {@link DslResolvingContext} that provides access to all the {@link ExtensionModel extensions}
   *                required for loading a given {@code artifact config} to an {@link ArtifactDeclaration}
   * @return an instance of the default implementation of the {@link XmlArtifactDeclarationLoader}
   */
  static XmlArtifactDeclarationLoader getDefault(DslResolvingContext context) {
    return new DefaultXmlArtifactDeclarationLoader(context);
  }

  /**
   * Creates an {@link ArtifactDeclaration} from a given mule artifact XML configuration file.
   *
   * @param configResource the input stream with the XML configuration content.
   * @return an {@link ArtifactDeclaration} that represents the given mule configuration.
   */
  ArtifactDeclaration load(InputStream configResource);

  /**
   * Creates an {@link ArtifactDeclaration} from a given mule artifact XML configuration file.
   *
   * @param name name of the file to display a better error messages (if there are any).
   * @param configResource the input stream with the XML configuration content.
   * @return an {@link ArtifactDeclaration} that represents the given mule configuration.
   */
  ArtifactDeclaration load(String name, InputStream configResource);

}
