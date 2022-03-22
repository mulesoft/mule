/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.ast;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.xml.AstXmlParser;

import java.util.Set;

/**
 * A component capable of supplying {@link AstXmlParser} instances
 *
 * @since 4.5.0
 */
@FunctionalInterface
public interface AstXmlParserSupplier {

  /**
   * Returns an {@link AstXmlParser}. Each invocation might return either new instances or reuse previously yielded ones, but must
   * always honour the supplied arguments.
   *
   * @param extensions         the {@link ExtensionModel} instances configured in the parser.
   * @param disableValidations whether to disable validations.
   * @return an {@link AstXmlParser}
   */
  AstXmlParser getParser(Set<ExtensionModel> extensions, boolean disableValidations);
}
