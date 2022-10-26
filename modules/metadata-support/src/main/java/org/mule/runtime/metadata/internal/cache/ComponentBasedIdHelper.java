/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.internal.util.cache.CacheIdBuilderAdapter;
import org.mule.runtime.extension.api.declaration.type.annotation.TypeDslAnnotation;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ComponentBasedIdHelper {

  public static Optional<String> getModelNameAst(ComponentAst component) {
    final Optional<NamedObject> namedObjectModel = component.getModel(NamedObject.class);
    if (namedObjectModel.isPresent()) {
      try {
        return namedObjectModel.map(NamedObject::getName);
      } catch (IllegalArgumentException e) {
        return empty();
      }
    }

    final Optional<Typed> typedObjectModel = component.getModel(Typed.class);
    if (typedObjectModel.isPresent()) {
      return typedObjectModel.map(t -> ExtensionMetadataTypeUtils.getId(t.getType()).toString());
    }
    return empty();
  }

  public static String sourceElementName(ComponentAst element) {
    return getModelNameAst(element)
        .map(modelName -> element.getIdentifier().getNamespace() + ":" + modelName
            + element.getComponentId().map(n -> "[" + n + "]").orElse(""))
        .orElseGet(() -> element.getIdentifier().toString());
  }

  public static String sourceElementNameFromSimpleValue(ComponentAst element) {
    return getModelNameAst(element)
        .map(modelName -> element.getIdentifier().getNamespace() + ":" + modelName)
        .orElseGet(() -> element.getIdentifier().toString());
  }

  public static Optional<String> resolveConfigName(ComponentAst elementModel) {
    // TODO MULE-18327 Migrate to Stereotypes when config-ref is part of model
    // There seems to be something missing in the mock model from the unit tests and this fails.
    // return MuleAstUtils.parameterOfType(elementModel, MuleStereotypes.CONFIG)
    // .map(p -> p.getValue().reduce(identity(), v -> v.toString()));
    return ofNullable(elementModel.getParameter(DEFAULT_GROUP_NAME, CONFIG_ATTRIBUTE_NAME))
        .map(param -> param.getResolvedRawValue());
  }

  public static List<String> parameterNamesRequiredForMetadataCacheId(ComponentAst component) {
    return component.getModel(EnrichableModel.class)
        .flatMap(model -> model.getModelProperty(RequiredForMetadataModelProperty.class)
            .map(RequiredForMetadataModelProperty::getRequiredParameters))
        .orElse(emptyList());
  }

  public static MetadataCacheId resolveDslTagId(ComponentAst elementModel) {
    final ComponentIdentifier id = elementModel.getIdentifier();
    return new MetadataCacheId(id.hashCode(), id.toString());
  }

  /**
   * @deprecated This should not have been public.
   */
  @Deprecated
  public static int computeHashFor(ComponentParameterAst componentParameterAst) {
    return DeprecatedParameterVisitorFunctions.computeHashFor(componentParameterAst);
  }

  public static <K> K computeIdFor(ComponentAst containerComponent,
                                   ComponentParameterAst componentParameterAst,
                                   Supplier<CacheIdBuilderAdapter<K>> cacheIdBuilderSupplier) {
    return ParameterVisitorFunctions.computeIdFor(containerComponent, componentParameterAst, cacheIdBuilderSupplier);
  }

  private static class ParameterVisitorFunctions<K> {

    private static <T> T computeIdFor(ComponentAst containerComponent,
                                      ComponentParameterAst parameter,
                                      Supplier<CacheIdBuilderAdapter<T>> cacheIdBuilderSupplier) {
      return new ParameterVisitorFunctions<>(containerComponent, parameter, cacheIdBuilderSupplier).idBuilder.build();
    }

    private static <T> T computeIdFor(ComponentAst componentAst,
                                      Supplier<CacheIdBuilderAdapter<T>> cacheIdBuilderSupplier) {
      return new ParameterVisitorFunctions<>(componentAst, cacheIdBuilderSupplier).idBuilder.build();
    }

    private final Supplier<CacheIdBuilderAdapter<K>> idBuilderSupplier;
    private final CacheIdBuilderAdapter<K> idBuilder;

    private ParameterVisitorFunctions(ComponentAst containerComponent,
                                      ComponentParameterAst parameterAst,
                                      Supplier<CacheIdBuilderAdapter<K>> cacheKeyBuilderSupplier) {
      String name = parameterAst
          .getGenerationInformation()
          .getSyntax()
          .map(s -> {
            if (isEmpty(s.getElementName())) {
              return s.getAttributeName();
            }
            return s.getPrefix() + ":" + s.getElementName();
          })
          .orElse(containerComponent.getIdentifier().getNamespace() + ":" + parameterAst.getModel().getName());
      this.idBuilderSupplier = cacheKeyBuilderSupplier;
      this.idBuilder = idBuilderSupplier.get().withSourceElementName(name).withHashValue(Objects.hashCode(name));
      parameterAst.getValue().reduce(v -> hashForLeft(parameterAst.getRawValue()), this::hashForRight);
    }

    private ParameterVisitorFunctions(ComponentAst component,
                                      Supplier<CacheIdBuilderAdapter<K>> cacheKeyBuilderSupplier) {
      String name = component.getIdentifier().toString();
      this.idBuilderSupplier = cacheKeyBuilderSupplier;
      this.idBuilder = idBuilderSupplier.get().withSourceElementName(name).withHashValue(Objects.hashCode(name));
      this.idBuilder.containing(component.getParameters().stream()
          .filter(componentParameterAst -> componentParameterAst.getValue().getValue().isPresent())
          .map(p -> computeIdFor(component, p, cacheKeyBuilderSupplier)).collect(toList()));
    }

    private Void hashForLeft(String s) {
      this.idBuilder.withHashValue(Objects.hashCode(s));
      return null;
    }

    private Void hashForRight(Object o) {
      if (o instanceof Collection) {
        final Collection<ComponentAst> collection = (Collection<ComponentAst>) o;
        this.idBuilder.containing(collection.stream()
            .map(e -> computeIdFor(e, idBuilderSupplier))
            .collect(toList()));
      } else if (o instanceof ComponentAst) {
        final ComponentAst c = (ComponentAst) o;
        this.idBuilder.containing(c.getParameters()
            .stream()
            .filter(componentParameterAst -> componentParameterAst.getValue().getValue().isPresent())
            .sorted(comparing(p -> p.getModel().getName()))
            .map(p -> computeIdFor(c, p, idBuilderSupplier))
            .collect(toList()));
      } else {
        if (o != null) {
          this.idBuilder.withHashValue(Objects.hashCode(o.toString()));
        }
      }
      return null;
    }
  }

  @Deprecated
  private static class DeprecatedParameterVisitorFunctions {

    private static int computeHashFor(ComponentParameterAst parameter) {
      return Objects.hashCode(new DeprecatedParameterVisitorFunctions(parameter).hashBuilder.toString());
    }

    private StringBuilder hashBuilder = new StringBuilder();
    private final Function<String, Void> leftFunction = this::hashForLeft;
    private final Function<Object, Void> rightFunction = this::hashForRight;

    private DeprecatedParameterVisitorFunctions(ComponentParameterAst startingParameter) {
      startingParameter.getValue().reduce(leftFunction, rightFunction);
    }

    private Void hashForLeft(String s) {
      hashBuilder.append(s);
      return null;
    }

    private Void hashForRight(Object o) {
      if (o instanceof ComponentAst) {
        final ComponentAst c = (ComponentAst) o;
        c.getParameters().stream().filter(componentParameterAst -> componentParameterAst.getValue().getValue().isPresent())
            .sorted(comparing(p -> p.getModel().getName())).forEach(p -> {
              hashBuilder.append(p.getModel().getName());
              if (p.getModel().getType() instanceof ArrayType) {
                hashForList((Collection<ComponentAst>) p.getValue().getRight());
              } else {
                p.getValue().reduce(leftFunction, rightFunction);
              }
            });
      } else {
        hashBuilder.append(o);
      }
      return null;
    }

    private void hashForList(Collection<ComponentAst> collection) {
      collection.forEach(c -> {
        ComponentParameterAst parameterAst = c.getParameter(DEFAULT_GROUP_NAME, "value");
        if (parameterAst == null) {
          rightFunction.apply(c);
        } else {
          parameterAst.getValue().reduce(leftFunction, rightFunction);
        }
      });
    }
  }

  public static Optional<MetadataCacheId> resolveMetadataKeyParts(ComponentAst elementModel,
                                                                  ComponentModel componentModel,
                                                                  boolean resolveAllKeys,
                                                                  Function<String, Optional<MetadataCacheId>> calculateForElement) {
    boolean isPartialFetching = componentModel.getModelProperty(TypeResolversInformationModelProperty.class)
        .map(TypeResolversInformationModelProperty::isPartialTypeKeyResolver)
        .orElse(false);

    if (!isPartialFetching && !resolveAllKeys) {
      return empty();
    }

    List<MetadataCacheId> keyParts = elementModel.getParameters()
        .stream()
        .filter(p -> p.getValue().getValue().isPresent())
        .filter(p -> p.getModel().getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
        .sorted(comparingInt(p -> p.getModel().getModelProperty(MetadataKeyPartModelProperty.class).get().getOrder()))
        .map(p -> resolveKeyFromSimpleValue(elementModel, p, calculateForElement))
        .collect(toList());
    return keyParts.isEmpty() ? empty() : of(new MetadataCacheId(keyParts, "metadataKeyValues"));
  }

  public static MetadataCacheId resolveKeyFromSimpleValue(ComponentAst elementModel, ComponentParameterAst param,
                                                          Function<String, Optional<MetadataCacheId>> calculateForElement) {
    final MetadataCacheId notCheckingReferences = computeIdFor(elementModel, param, MetadataCacheIdBuilderAdapter::new);
    return param.getValue().reduce(v -> notCheckingReferences,
                                   v -> {
                                     Reference<MetadataCacheId> reference = new Reference<>();
                                     if (v instanceof ComponentAst) {
                                       param.getModel().getType().accept(new MetadataTypeVisitor() {

                                         @Override
                                         public void visitArrayType(ArrayType arrayType) {
                                           calculateForElement.apply(param.getResolvedRawValue()).ifPresent(reference::set);
                                         }

                                         @Override
                                         public void visitObject(ObjectType objectType) {
                                           boolean canBeGlobal = objectType.getAnnotation(TypeDslAnnotation.class)
                                               .map(TypeDslAnnotation::allowsTopLevelDefinition).orElse(false);

                                           if (canBeGlobal) {
                                             calculateForElement.apply(param.getResolvedRawValue()).ifPresent(reference::set);
                                           }
                                         }
                                       });
                                     } else {
                                       final ParameterModel paramModel = param.getModel();
                                       paramModel.getType().accept(new MetadataTypeVisitor() {

                                         @Override
                                         public void visitString(StringType stringType) {
                                           if (!paramModel.getAllowedStereotypes().isEmpty()) {
                                             calculateForElement.apply(v.toString()).ifPresent(reference::set);
                                           }
                                         }

                                         @Override
                                         public void visitArrayType(ArrayType arrayType) {
                                           if (paramModel.getDslConfiguration().allowsReferences() && v instanceof String) {
                                             calculateForElement.apply(v.toString()).ifPresent(reference::set);
                                           }
                                         }

                                         @Override
                                         public void visitObject(ObjectType objectType) {
                                           if (paramModel.getDslConfiguration().allowsReferences()) {
                                             calculateForElement.apply(v.toString()).ifPresent(reference::set);
                                           }
                                         }

                                       });
                                     }
                                     return reference.get() == null ? notCheckingReferences : reference.get();
                                   });
  }

}
