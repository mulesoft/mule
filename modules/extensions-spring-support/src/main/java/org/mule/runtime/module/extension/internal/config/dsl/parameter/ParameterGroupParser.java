/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * A {@link ExtensionDefinitionParser} for parsing extension objects that are expressed as an inline {@link ParameterGroupModel}
 * <p>
 * These objects are parsed as {@link ValueResolver}s which are later resolved by a {@link TopLevelParameterObjectFactory}
 * instance
 *
 * @since 4.0
 */
public abstract class ParameterGroupParser extends ExtensionDefinitionParser {

  protected final ParameterGroupModel group;
  protected final ClassLoader classLoader;
  protected final DslElementSyntax groupDsl;
  protected final String name;
  protected final String namespace;

  public ParameterGroupParser(Builder definition, ParameterGroupModel group, ClassLoader classLoader, DslElementSyntax groupDsl,
                              DslSyntaxResolver dslResolver, ExtensionParsingContext context) {
    super(definition, dslResolver, context);

    checkArgument(group.isShowInDsl(), "Cannot parse an implicit group");
    this.group = group;

    this.classLoader = classLoader;
    this.groupDsl = groupDsl;
    this.name = groupDsl.getElementName();
    this.namespace = groupDsl.getPrefix();
  }
}
