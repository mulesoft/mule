/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static java.lang.String.format;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * A {@link ExtensionDefinitionParser} for parsing extension objects that can be defined as named top level elements and be placed
 * in the mule registry.
 * <p>
 * These objects are parsed as {@link ValueResolver}s which are later resolved by a {@link TopLevelParameterObjectFactory}
 * instance
 *
 * @since 4.0
 */
public class ObjectTypeParameterParser extends ExtensionDefinitionParser {

  private final ObjectType type;
  private final ClassLoader classLoader;
  private final DslElementSyntax typeDsl;
  private final String name;
  private final String namespace;

  public ObjectTypeParameterParser(Builder definition, ObjectType type, ClassLoader classLoader,
                                   DslSyntaxResolver dslResolver, ExtensionParsingContext context) {
    super(definition, dslResolver, context);
    this.type = type;
    this.classLoader = classLoader;
    this.typeDsl = dslResolver.resolve(type)
        .orElseThrow(() -> new IllegalArgumentException(format("Non parseable object of type [%s]", getId(type))));
    this.name = typeDsl.getElementName();
    this.namespace = typeDsl.getPrefix();
  }

  public ObjectTypeParameterParser(Builder definition, String name, String namespace, ObjectType type,
                                   ClassLoader classLoader,
                                   DslSyntaxResolver dslResolver, ExtensionParsingContext context) {
    super(definition, dslResolver, context);
    this.name = name;
    this.namespace = namespace;
    this.type = type;
    this.classLoader = classLoader;
    this.typeDsl = dslResolver.resolve(type)
        .orElseThrow(() -> new IllegalArgumentException(format("Non parseable object [%s:%s] of type [%s]",
                                                               name, namespace, getId(type))));

  }

  @Override
  protected Builder doParse(Builder definitionBuilder) throws ConfigurationException {
    Builder finalBuilder = definitionBuilder.withIdentifier(name).withNamespace(namespace).asNamed()
        .withTypeDefinition(fromType(ValueResolver.class))
        .withObjectFactoryType(TopLevelParameterObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(type).build())
        .withConstructorParameterDefinition(fromFixedValue(classLoader).build())
        .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build());

    parseFields(type, typeDsl);

    return finalBuilder;
  }


}
