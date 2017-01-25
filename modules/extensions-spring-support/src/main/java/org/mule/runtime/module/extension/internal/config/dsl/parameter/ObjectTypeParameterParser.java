/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static java.util.Collections.emptySet;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.declaration.type.TypeUtils.acceptsReferences;
import static org.mule.runtime.extension.api.declaration.type.TypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.declaration.type.TypeUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isParameterGroup;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;

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
                                   DslSyntaxResolver dslResolver, ExtensionParsingContext context,
                                   MuleContext muleContext) {
    super(definition, dslResolver, context, muleContext);
    this.type = type;
    this.classLoader = classLoader;
    this.typeDsl = dslResolver.resolve(type).orElseThrow(() -> new IllegalArgumentException("Non parseable object"));
    this.name = typeDsl.getElementName();
    this.namespace = typeDsl.getNamespace();
  }

  public ObjectTypeParameterParser(Builder definition, String name, String namespace, ObjectType type,
                                   ClassLoader classLoader,
                                   DslSyntaxResolver dslResolver, ExtensionParsingContext context,
                                   MuleContext muleContext) {
    super(definition, dslResolver, context, muleContext);
    this.type = type;
    this.classLoader = classLoader;
    this.typeDsl = dslResolver.resolve(type).orElseThrow(() -> new IllegalArgumentException("Non parseable object"));
    this.name = name;
    this.namespace = namespace;
  }

  @Override
  protected void doParse(Builder definitionBuilder) throws ConfigurationException {
    definitionBuilder.withIdentifier(name).withNamespace(namespace).asNamed().withTypeDefinition(fromType(ValueResolver.class))
        .withObjectFactoryType(TopLevelParameterObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(type).build())
        .withConstructorParameterDefinition(fromFixedValue(classLoader).build())
        .withConstructorParameterDefinition(fromFixedValue(muleContext).build());

    type.getFields().forEach(this::parseField);
  }

  private void parseField(ObjectFieldType objectField) {
    final MetadataType fieldType = objectField.getValue();
    final String fieldName = objectField.getKey().getName().getLocalPart();
    final boolean acceptsReferences = acceptsReferences(objectField);
    final Object defaultValue = getDefaultValue(fieldType).orElse(null);
    final ExpressionSupport expressionSupport = getExpressionSupport(objectField);
    Optional<DslElementSyntax> fieldDsl = typeDsl.getContainedElement(fieldName);
    if (!fieldDsl.isPresent() && !isParameterGroup(objectField)) {
      return;
    }

    Optional<String> keyName = getInfrastructureParameterName(fieldType);
    if (keyName.isPresent()) {
      parseObject(fieldName, keyName.get(), (ObjectType) fieldType, defaultValue, expressionSupport, false, acceptsReferences,
                  fieldDsl.get(), emptySet());
      return;
    }

    final boolean isContent = isContent(objectField);
    fieldType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        parseAttributeParameter(fieldName, fieldName, metadataType, defaultValue, expressionSupport, false, emptySet());
      }

      @Override
      public void visitString(StringType stringType) {
        if (fieldDsl.get().supportsChildDeclaration()) {
          addParameter(fieldName, fromChildConfiguration(String.class).withWrapperIdentifier(fieldName));
          addDefinition(baseDefinitionBuilder.copy()
              .withIdentifier(fieldName)
              .withTypeDefinition(fromType(String.class))
              .withTypeConverter(value -> resolverOf(fieldName, stringType, value, defaultValue,
                                                     expressionSupport, false,
                                                     emptySet(), acceptsReferences))
              .build());
        } else {
          defaultVisit(stringType);
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (isParameterGroup(objectField)) {
          objectType.getFields().forEach(field -> parseField(field));
          return;
        }

        if (parseAsContent(isContent, objectType)) {
          return;
        }

        DslElementSyntax dsl = fieldDsl.get();
        if (!parsingContext.isRegistered(dsl.getElementName(), dsl.getNamespace())) {
          parsingContext.registerObjectType(dsl.getElementName(), dsl.getNamespace(), type);
          parseObjectParameter(fieldName, fieldName, objectType, defaultValue, expressionSupport, false, acceptsReferences,
                               dsl, emptySet());
        } else {
          parseObject(fieldName, fieldName, objectType, defaultValue, expressionSupport, false, acceptsReferences,
                      dsl, emptySet());
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        if (!parseAsContent(isContent, arrayType)) {
          parseCollectionParameter(fieldName, fieldName, arrayType, defaultValue, expressionSupport, false, fieldDsl.get(),
                                   emptySet());
        }
      }

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        if (!parseAsContent(isContent, dictionaryType)) {
          parseMapParameters(fieldName, fieldName, dictionaryType, defaultValue, expressionSupport, false, fieldDsl.get(),
                             emptySet());
        }
      }

      private boolean parseAsContent(boolean isContent, MetadataType type) {
        if (isContent) {
          parseFromTextExpression(fieldName, fieldDsl.get(), () -> value -> resolverOf(fieldName, type, value, defaultValue,
                                                                                       expressionSupport, false, emptySet(),
                                                                                       false));

          return true;
        }

        return false;
      }
    });
  }
}
