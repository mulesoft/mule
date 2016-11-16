/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.join;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.isVoid;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.metadata.NullMetadataResolver.NULL_CATEGORY_NAME;
import static org.mule.runtime.module.extension.internal.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getMetadataResolverFactory;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.model.property.MetadataKeyPartModelProperty;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Validates that all {@link OperationModel operations} which return type is a {@link Object} or a {@link Map} have defined a
 * {@link OutputTypeResolver}. The {@link OutputTypeResolver} can't be the {@link NullMetadataResolver}.
 *
 * @since 4.0
 */
public class MetadataComponentModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    if (!(extensionModel instanceof ExtensionModel)) {
      return;
    }
    new ExtensionWalker() {

      @Override
      public void onOperation(HasOperationModels owner, OperationModel model) {
        validateComponent(model);
      }

      @Override
      public void onSource(HasSourceModels owner, SourceModel model) {
        validateComponent(model);
      }

      private void validateComponent(ComponentModel model) {
        validateMetadataReturnType(extensionModel, model);
        MetadataResolverFactory resolverFactory = getMetadataResolverFactory(model);
        validateMetadataOutputAttributes(model, resolverFactory);
        validateMetadataKeyId(model, resolverFactory);
        validateCategoriesInScope(model, resolverFactory);
      }
    }.walk(extensionModel);
  }

  private void validateMetadataKeyId(ComponentModel model, MetadataResolverFactory resolverFactory) {
    Optional<MetadataKeyIdModelProperty> keyId = model.getModelProperty(MetadataKeyIdModelProperty.class);
    if (keyId.isPresent()) {

      if (resolverFactory.getOutputResolver() instanceof NullMetadataResolver &&
          getAllInputResolvers(model, resolverFactory).isEmpty()) {

        throw new IllegalModelDefinitionException(format("Component [%s] defines a MetadataKeyId parameter but neither"
            + " an Output nor Type resolver that makes use of it was defined",
                                                         model.getName()));
      }

      keyId.get().getType().accept(new MetadataTypeVisitor() {

        public void visitObject(ObjectType objectType) {
          List<ParameterModel> parts = model.getAllParameterModels().stream()
              .filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent()).collect(toList());

          List<ParameterModel> defaultParts = parts.stream().filter(p -> p.getDefaultValue() != null).collect(toList());

          if (!defaultParts.isEmpty() && defaultParts.size() != parts.size()) {
            throw new IllegalModelDefinitionException(
                                                      format("[%s] type multilevel key defines [%s] MetadataKeyPart with default values, but the type contains [%s] "
                                                          + "MetadataKeyParts. All the annotated MetadataKeyParts should have a default value if at least one part "
                                                          + "has a default value.", getType(objectType).getSimpleName(),
                                                             defaultParts.size(), parts.size()));
          }
        }
      });
    } else {
      if (!(resolverFactory.getKeyResolver() instanceof NullMetadataResolver)) {
        throw new IllegalModelDefinitionException(format("Component [%s] does not define a MetadataKeyId parameter but "
            + "a type keys resolver of type [%s] was associated to it",
                                                         model.getName(), resolverFactory.getKeyResolver().getClass().getName()));
      }
    }

  }

  private void validateMetadataOutputAttributes(ComponentModel component, MetadataResolverFactory resolverFactory) {
    if (isVoid(component.getOutputAttributes().getType())
        && !(resolverFactory.getOutputAttributesResolver() instanceof NullMetadataResolver)) {
      throw new IllegalModelDefinitionException(format("%s '%s' has an attributes metadata resolver defined but it doesn't set any attributes",
                                                       getComponentModelTypeName(component), component.getName()));
    }
  }


  private void validateMetadataReturnType(ExtensionModel extensionModel, ComponentModel component) {
    if (getMetadataResolverFactory(component).getOutputResolver() instanceof NullMetadataResolver) {
      component.getOutput().getType().accept(new MetadataTypeVisitor() {

        @Override
        public void visitObject(ObjectType objectType) {
          failIfTypeIsObject(component, extensionModel, objectType);
        }

        @Override
        public void visitDictionary(DictionaryType dictionaryType) {
          failIfTypeIsObject(component, extensionModel, dictionaryType.getValueType());
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          arrayType.getType().accept(this);
        }
      });
    }
  }

  private void validateCategoriesInScope(ComponentModel componentModel, MetadataResolverFactory metadataResolverFactory) {

    ImmutableList.Builder<NamedTypeResolver> resolvers = ImmutableList.<NamedTypeResolver>builder()
        .add(metadataResolverFactory.getKeyResolver())
        .add(metadataResolverFactory.getOutputResolver())
        .addAll(getAllInputResolvers(componentModel, metadataResolverFactory));

    validateCategoryNames(componentModel, resolvers.build().toArray(new NamedTypeResolver[] {}));
  }

  private List<InputTypeResolver<Object>> getAllInputResolvers(ComponentModel componentModel,
                                                               MetadataResolverFactory resolverFactory) {
    return componentModel.getAllParameterModels().stream().map(NamedObject::getName)
        .map(resolverFactory::getInputResolver).collect(toList());
  }

  private void validateCategoryNames(ComponentModel componentModel, NamedTypeResolver... resolvers) {
    stream(resolvers).filter(r -> isBlank(r.getCategoryName()))
        .findFirst().ifPresent(r -> {
          throw new IllegalModelDefinitionException(
                                                    format("%s '%s' specifies a metadata resolver [%s] which has an empty category name",
                                                           getComponentModelTypeName(componentModel), componentModel.getName(),
                                                           r.getClass().getSimpleName()));
        });

    Set<String> names = stream(resolvers)
        .map(NamedTypeResolver::getCategoryName)
        .filter(r -> !r.equals(NULL_CATEGORY_NAME))
        .collect(toSet());

    if (names.size() > 1) {
      throw new IllegalModelDefinitionException(format(
                                                       "%s '%s' specifies metadata resolvers that doesn't belong to the same category. The following categories were the ones found [%s]",
                                                       getComponentModelTypeName(componentModel), componentModel.getName(),
                                                       join(names, ",")));
    }
  }

  private void failIfTypeIsObject(ComponentModel componentModel, ExtensionModel extensionModel, MetadataType type) {
    if (Object.class.equals(getType(type))) {
      String componentTypeName = getComponentModelTypeName(componentModel);
      throw new IllegalModelDefinitionException(format("Extension '%s' specifies a/an %s named '%s' with type '%s' as return type. Operations and Sources with "
          + "return type such as Object or Map (or a collection of any of those) must have defined a not null OutputTypeResolver",
                                                       extensionModel.getName(), componentTypeName, componentModel.getName(),
                                                       getId(type)));
    }
  }
}
