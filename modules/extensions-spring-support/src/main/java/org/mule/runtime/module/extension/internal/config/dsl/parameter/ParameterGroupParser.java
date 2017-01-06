/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * A {@link ExtensionDefinitionParser} for parsing extension objects that are expressed as an inline {@link ParameterGroupModel}
 * <p>
 * These objects are parsed as {@link ValueResolver}s which are later resolved by a {@link TopLevelParameterObjectFactory}
 * instance
 *
 * @since 4.0
 */
public class ParameterGroupParser extends ExtensionDefinitionParser {

  private final ParameterGroupModel group;
  private final ClassLoader classLoader;
  private final DslElementSyntax groupDsl;
  private final String name;
  private final String namespace;
  private final ParameterGroupDescriptor groupDescriptor;
  private final MetadataType metadataType;

  public ParameterGroupParser(Builder definition, ParameterGroupModel group, ParameterGroupDescriptor groupDescriptor,
                              ClassLoader classLoader, DslElementSyntax groupDsl, DslSyntaxResolver dslResolver,
                              ExtensionParsingContext context,
                              MuleContext muleContext) {
    super(definition, dslResolver, context, muleContext);

    checkArgument(group.isShowInDsl(), "Cannot parse an implicit group");
    this.group = group;
    this.groupDescriptor = groupDescriptor;
    this.classLoader = classLoader;
    this.groupDsl = groupDsl;
    this.name = groupDsl.getElementName();
    this.namespace = groupDsl.getNamespace();
    this.metadataType = getDefault().createTypeLoader(classLoader).load(groupDescriptor.getType().getDeclaringClass());
  }

  @Override
  protected void doParse(Builder definitionBuilder) throws ConfigurationException {
    definitionBuilder.withIdentifier(name).withNamespace(namespace).asNamed().withTypeDefinition(fromType(ValueResolver.class))
        .withObjectFactoryType(TopLevelParameterObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(metadataType).build())
        .withConstructorParameterDefinition(fromFixedValue(classLoader).build())
        .withConstructorParameterDefinition(fromFixedValue(muleContext).build());

    this.parseParameters(group.getParameterModels());
  }

}
