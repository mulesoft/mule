/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isVoid;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.metadata.MetadataResolverUtils.getAllResolvers;
import static org.mule.runtime.extension.api.metadata.MetadataResolverUtils.isNullResolver;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import org.mule.metadata.api.model.ArrayType;
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
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.extension.internal.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.internal.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.LinkedList;
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
public class MetadataComponentModelValidator implements ExtensionModelValidator {

  private static final String EMPTY_RESOLVER_NAME = "%s '%s' specifies a metadata resolver [%s] which has an empty %s name";

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {

    final Table<String, String, Class<?>> names = HashBasedTable.create();
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
        validateMetadataReturnType(extensionModel, model, problemsReporter);
        MetadataResolverFactory resolverFactory = MuleExtensionUtils.getMetadataResolverFactory(model);
        validateMetadataOutputAttributes(model, resolverFactory, problemsReporter);
        validateMetadataKeyId(model, resolverFactory, problemsReporter);
        validateCategoriesInScope(model, resolverFactory, problemsReporter);
        validateResolversName(model, resolverFactory, names, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void validateResolversName(ComponentModel model, MetadataResolverFactory resolverFactory,
                                     Table<String, String, Class<?>> names, ProblemsReporter problemsReporter) {
    List<NamedTypeResolver> resolvers = new LinkedList<>();
    resolvers.addAll(getAllInputResolvers(model, resolverFactory));
    resolvers.add(resolverFactory.getOutputResolver());

    resolvers.stream()
        .filter(r -> !r.getClass().equals(NullMetadataResolver.class))
        .forEach(r -> {
          if (isBlank(r.getResolverName())) {
            problemsReporter.addError(new Problem(model,
                                                  format(EMPTY_RESOLVER_NAME,
                                                         getComponentModelTypeName(model), model.getName(),
                                                         r.getClass().getSimpleName(), "resolver")));
          } else {
            if (names.get(r.getCategoryName(), r.getResolverName()) != null
                && names.get(r.getCategoryName(), r.getResolverName()) != r.getClass()) {
              problemsReporter
                  .addError(new Problem(model,
                                        format("%s [%s] specifies metadata resolvers with repeated name [%s] for the same category [%s]. Resolver names should be unique for a given category. Affected resolvers are '%s' and '%s'",
                                               getComponentModelTypeName(model), model.getName(),
                                               r.getResolverName(), r.getCategoryName(),
                                               names.get(r.getCategoryName(), r.getResolverName()).getSimpleName(),
                                               r.getClass().getSimpleName())));

            }
            names.put(r.getCategoryName(), r.getResolverName(), r.getClass());
          }
        });
  }

  private void validateMetadataKeyId(ComponentModel model, MetadataResolverFactory resolverFactory,
                                     ProblemsReporter problemsReporter) {
    Optional<MetadataKeyIdModelProperty> keyId = model.getModelProperty(MetadataKeyIdModelProperty.class);
    if (keyId.isPresent()) {

      if (resolverFactory.getOutputResolver() instanceof NullMetadataResolver &&
          getAllInputResolvers(model, resolverFactory).isEmpty()) {
        problemsReporter.addError(new Problem(model, format("Component [%s] defines a MetadataKeyId parameter but neither"
            + " an Output nor Type resolver that makes use of it was defined",
                                                            model.getName())));
      }

      keyId.get().getType().accept(new MetadataTypeVisitor() {

        public void visitObject(ObjectType objectType) {
          List<ParameterModel> parts = model.getAllParameterModels().stream()
              .filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent()).collect(toList());

          List<ParameterModel> defaultParts = parts.stream().filter(p -> p.getDefaultValue() != null).collect(toList());

          if (!defaultParts.isEmpty() && defaultParts.size() != parts.size()) {
            problemsReporter
                .addError(new Problem(model,
                                      format("[%s] type multilevel key defines [%s] MetadataKeyPart with default values, but the type contains [%s] "
                                          + "MetadataKeyParts. All the annotated MetadataKeyParts should have a default value if at least one part "
                                          + "has a default value.", getType(objectType).getSimpleName(),
                                             defaultParts.size(), parts.size())));
          }
        }
      });
    } else {
      if (!(resolverFactory.getKeyResolver() instanceof NullMetadataResolver)) {
        problemsReporter.addError(new Problem(model, format("Component [%s] does not define a MetadataKeyId parameter but "
            + "a type keys resolver of type [%s] was associated to it",
                                                            model.getName(),
                                                            resolverFactory.getKeyResolver().getClass().getName())));
      }
    }

  }

  private void validateMetadataOutputAttributes(ComponentModel component, MetadataResolverFactory resolverFactory,
                                                ProblemsReporter problemsReporter) {
    if (isVoid(component.getOutputAttributes().getType())
        && !(resolverFactory.getOutputAttributesResolver() instanceof NullMetadataResolver)) {
      problemsReporter.addError(new Problem(component, String
          .format("%s '%s' has an attributes metadata resolver defined but it doesn't set any attributes",
                  getComponentModelTypeName(component), component.getName())));
    }
  }


  private void validateMetadataReturnType(ExtensionModel extensionModel, ComponentModel component,
                                          ProblemsReporter problemsReporter) {
    if (MuleExtensionUtils.getMetadataResolverFactory(component).getOutputResolver() instanceof NullMetadataResolver) {
      component.getOutput().getType().accept(new MetadataTypeVisitor() {

        @Override
        public void visitObject(ObjectType objectType) {
          objectType.getOpenRestriction().ifPresent(t -> failIfTypeIsObject(component, extensionModel, t, problemsReporter));
          failIfTypeIsObject(component, extensionModel, objectType, problemsReporter);
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          arrayType.getType().accept(this);
        }
      });
    }
  }

  // todo refactor with metadata factory getCategoryName()
  private void validateCategoriesInScope(ComponentModel componentModel, MetadataResolverFactory metadataResolverFactory,
                                         ProblemsReporter problemsReporter) {

    validateCategoryNames(componentModel, problemsReporter, getAllResolvers(metadataResolverFactory));
  }

  private List<InputTypeResolver<Object>> getAllInputResolvers(ComponentModel componentModel,
                                                               MetadataResolverFactory resolverFactory) {
    return componentModel.getAllParameterModels().stream().map(NamedObject::getName)
        .map(resolverFactory::getInputResolver).collect(toList());
  }

  private void validateCategoryNames(ComponentModel componentModel, ProblemsReporter problemsReporter,
                                     List<NamedTypeResolver> resolvers) {
    resolvers.stream().filter(r -> StringUtils.isBlank(r.getCategoryName()))
        .findFirst().ifPresent(r -> problemsReporter.addError(new Problem(componentModel, String
            .format(EMPTY_RESOLVER_NAME,
                    getComponentModelTypeName(componentModel), componentModel.getName(),
                    r.getClass().getSimpleName(), "category"))));

    Set<String> names = resolvers.stream()
        .filter(r -> !isNullResolver(r))
        .map(NamedTypeResolver::getCategoryName)
        .collect(toSet());

    if (names.size() > 1) {
      problemsReporter.addError(new Problem(componentModel, String.format(
                                                                          "%s '%s' specifies metadata resolvers that doesn't belong to the same category. The following categories were the ones found [%s]",
                                                                          getComponentModelTypeName(componentModel),
                                                                          componentModel.getName(),
                                                                          join(names, ","))));
    }
  }

  private void failIfTypeIsObject(ComponentModel componentModel, ExtensionModel extensionModel, MetadataType type,
                                  ProblemsReporter problemsReporter) {
    if (Object.class.equals(getType(type))) {
      String componentTypeName = getComponentModelTypeName(componentModel);
      problemsReporter
          .addError(new Problem(extensionModel,
                                format("Extension '%s' specifies a/an %s named '%s' with type '%s' as return type. Operations and Sources with "
                                    + "return type such as Object or Map (or a collection of any of those) must have defined a not null OutputTypeResolver",
                                       extensionModel.getName(), componentTypeName, componentModel.getName(),
                                       ExtensionMetadataTypeUtils.getId(type))));
    }
  }
}
