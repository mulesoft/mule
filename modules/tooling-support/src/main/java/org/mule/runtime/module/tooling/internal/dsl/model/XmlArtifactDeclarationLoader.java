/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.model;

import static java.lang.Class.forName;
import static java.lang.System.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.module.tooling.internal.dsl.declaration.AstXmlArtifactDeclarationLoader;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;

/**
 * Loads a mule XML configuration file into an {@link ArtifactDeclaration} representation.
 *
 * @since 4.0
 */
public interface XmlArtifactDeclarationLoader {

  // This mechanism is in place for reverting to the previous implementation just in case.
  public static final class Initializer {

    private static final Logger LOGGER = getLogger(Initializer.class);

    private static Class<?> LOADER_CLASS;

    static {
      String loaderClassName = getProperty(XmlArtifactDeclarationLoader.class.getName() + ".loaderClassName");

      if (loaderClassName == null) {
        LOADER_CLASS = null;
      } else {
        try {
          LOADER_CLASS = forName(loaderClassName);
        } catch (ClassNotFoundException e) {
          LOGGER.warn(e.getClass().getName() + ": " + e.getMessage());
          LOADER_CLASS = null;
        }
      }
    }

    private Initializer() {
      // Nothing to do
    }
  }

  /**
   * Provides an instance of the default implementation of the {@link XmlArtifactDeclarationLoader}.
   *
   * @param context a {@link DslResolvingContext} that provides access to all the {@link ExtensionModel extensions} required for
   *                loading a given {@code artifact config} to an {@link ArtifactDeclaration}
   * @return an instance of the default implementation of the {@link XmlArtifactDeclarationLoader}
   */
  static XmlArtifactDeclarationLoader getDefault(DslResolvingContext context) {
    if (Initializer.LOADER_CLASS == null) {
      return new AstXmlArtifactDeclarationLoader(context);
    } else {
      try {
        return (XmlArtifactDeclarationLoader) Initializer.LOADER_CLASS.getConstructor(DslResolvingContext.class)
            .newInstance(context);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
          | NoSuchMethodException | SecurityException e) {
        Initializer.LOGGER.warn(e.getClass().getName() + ": " + e.getMessage());
        return new AstXmlArtifactDeclarationLoader(context);
      }
    }
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
   * @param name           name of the file to display a better error messages (if there are any).
   * @param configResource the input stream with the XML configuration content.
   * @return an {@link ArtifactDeclaration} that represents the given mule configuration.
   */
  ArtifactDeclaration load(String name, InputStream configResource);

}
