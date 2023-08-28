/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.api.ast;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.xml.AstXmlParser;

import java.util.Set;

/**
 * A component capable of supplying {@link AstXmlParser} instances
 *
 * @since 4.5.0
 */
@FunctionalInterface
@NoImplement
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
