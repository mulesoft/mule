/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.construct;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isObjectType;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * A parser which returns the definition parsers for a given {@link NestedRouteModel}
 *
 * @since 4.0
 */
public class RouteComponentParser extends ExtensionDefinitionParser {

  private final ObjectType metadataType;
  private final ClassLoader classLoader;
  private final DslElementSyntax routeDsl;
  private final String name;
  private final String namespace;
  private final NestedRouteModel route;

  public RouteComponentParser(Builder definition,
                              NestedRouteModel route,
                              MetadataType metadataType,
                              ClassLoader classLoader,
                              DslElementSyntax routeDsl,
                              DslSyntaxResolver dslResolver,
                              ExtensionParsingContext context) {
    super(definition, dslResolver, context);

    checkArgument(isObjectType(metadataType),
                  format("Only an ObjectType can be parsed as a TypedParameterGroup, found [%s] instead",
                         metadataType.getClass().getName()));
    this.route = route;
    this.metadataType = (ObjectType) metadataType;
    this.classLoader = classLoader;
    this.routeDsl = routeDsl;
    this.name = routeDsl.getElementName();
    this.namespace = routeDsl.getPrefix();
  }


  @Override
  protected Builder doParse(Builder definitionBuilder) throws ConfigurationException {
    Builder finalBuilder = definitionBuilder.withIdentifier(name).withNamespace(namespace).asNamed()
        .withTypeDefinition(fromType(ValueResolver.class))
        .withObjectFactoryType(RouteComponentObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(route).build())
        .withConstructorParameterDefinition(fromFixedValue(metadataType).build())
        .withConstructorParameterDefinition(fromFixedValue(classLoader).build())
        .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        .withConstructorParameterDefinition(fromChildCollectionConfiguration(Processor.class).build());

    parseParameters(route.getAllParameterModels());

    return finalBuilder;
  }
}
