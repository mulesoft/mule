/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * Parses the syntactic definition of the output metadata so that the semantics reflected in it can be extracted in a uniform way,
 * regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface OutputResolverModelParser {

  boolean hasOutputResolver();

  OutputTypeResolver<?> getOutputResolver();
}
