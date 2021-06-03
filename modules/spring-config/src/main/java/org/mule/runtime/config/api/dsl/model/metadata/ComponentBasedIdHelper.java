/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.util.Comparator.comparing;
import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.aValueProviderCacheId;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.fromElementWithName;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.metadata.api.model.ArrayType;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class ComponentBasedIdHelper {

  static String getSourceElementName(ComponentAst elementModel) {
    return elementModel.getIdentifier().toString()
        + getModelNameAst(elementModel).orElse(elementModel.getComponentId().map(n -> "[" + n + "]").orElse(""));
  }

  static Optional<String> getModelNameAst(ComponentAst component) {
    final Optional<NamedObject> namedObjectModel = component.getModel(NamedObject.class);
    if (namedObjectModel.isPresent()) {
      return namedObjectModel.map(n -> n.getName());
    }

    final Optional<Typed> typedObjectModel = component.getModel(Typed.class);
    if (typedObjectModel.isPresent()) {
      return typedObjectModel.map(t -> ExtensionMetadataTypeUtils.getId(t.getType()).toString());
    }

    return empty();
  }

  static String sourceElementNameFromSimpleValue(ComponentAst element) {
    return getModelNameAst(element)
        .map(modelName -> element.getIdentifier().getNamespace() + ":" + modelName)
        .orElseGet(() -> element.getIdentifier().toString());
  }

  static String sourceElementNameFromSimpleValue(ComponentAst owner, ComponentParameterAst element) {
    return owner.getIdentifier().getNamespace() + ":" + element.getModel().getName();
  }

  static Optional<String> resolveConfigName(ComponentAst elementModel) {
    // TODO MULE-18327 Migrate to Stereotypes when config-ref is part of model
    // There seems to be something missing in the mock model from the unit tests and this fails.
    // return MuleAstUtils.parameterOfType(elementModel, MuleStereotypes.CONFIG)
    // .map(p -> p.getValue().reduce(identity(), v -> v.toString()));
    return elementModel.getRawParameterValue(CONFIG_ATTRIBUTE_NAME);
  }

  /**
   * @deprecated Use {@link ComponentBasedIdHelper#computeIdFor(ComponentAst, ComponentParameterAst)} and get the value.
   */
  @Deprecated
  public static int computeHashFor(ComponentParameterAst componentParameterAst) {
    return DeprecatedParameterVisitorFunctions.computeHashFor(componentParameterAst);
  }

  static ValueProviderCacheId computeIdFor(ComponentAst containerComponent,
                                           ComponentParameterAst componentParameterAst) {
    return ParameterVisitorFunctions.computeIdFor(containerComponent, componentParameterAst);
  }

  private static class ParameterVisitorFunctions {

    private static ValueProviderCacheId computeIdFor(ComponentAst containerComponent, ComponentParameterAst parameter) {
      return aValueProviderCacheId(new ParameterVisitorFunctions(containerComponent, parameter).idBuilder);
    }

    private static ValueProviderCacheId computeIdFor(ComponentAst componentAst) {
      return aValueProviderCacheId(new ParameterVisitorFunctions(componentAst).idBuilder);
    }

    private ValueProviderCacheId.ValueProviderCacheIdBuilder idBuilder;
    private final Function<String, Void> leftFunction = this::hashForLeft;
    private final Function<Object, Void> rightFunction = this::hashForRight;

    private ParameterVisitorFunctions(ComponentAst containerComponent, ComponentParameterAst parameterAst) {
      String name = parameterAst
          .getGenerationInformation()
          .getSyntax()
          .map(s -> {
            if (isEmpty(s.getElementName())) {
              return s.getAttributeName();
            }
            return s.getPrefix() + ":" + s.getElementName();
          })
          .orElse(sourceElementNameFromSimpleValue(containerComponent, parameterAst));
      this.idBuilder = fromElementWithName(name).withHashValueFrom(name);
      parameterAst.getValue().reduce(leftFunction, rightFunction);
    }

    private ParameterVisitorFunctions(ComponentAst component) {
      String name = component.getIdentifier().toString();
      this.idBuilder = fromElementWithName(name).withHashValueFrom(name);
      this.idBuilder.containing(component.getParameters().stream().map(p -> computeIdFor(component, p)).collect(toList()));
    }

    private Void hashForLeft(String s) {
      this.idBuilder.withHashValueFrom(s);
      return null;
    }

    private Void hashForRight(Object o) {
      if (o instanceof Collection) {
        final Collection<ComponentAst> collection = (Collection<ComponentAst>) o;
        this.idBuilder.containing(
                                  collection.stream()
                                      .map(ParameterVisitorFunctions::computeIdFor)
                                      .collect(toList()));
      } else if (o instanceof ComponentAst) {
        final ComponentAst c = (ComponentAst) o;
        this.idBuilder.containing(c.getParameters()
            .stream()
            .sorted(comparing(p -> p.getModel().getName()))
            .map(p -> computeIdFor(c, p))
            .collect(toList()));
      } else {
        this.idBuilder.withHashValueFrom(o.toString());
      }
      return null;
    }
  }

  @Deprecated
  private static class DeprecatedParameterVisitorFunctions {

    private static int computeHashFor(ComponentParameterAst parameter) {
      return hash(new DeprecatedParameterVisitorFunctions(parameter).hashBuilder.toString());
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
        c.getParameters().stream().sorted(comparing(p -> p.getModel().getName())).forEach(p -> {
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
        ComponentParameterAst parameterAst = c.getParameter("value");
        if (parameterAst == null) {
          rightFunction.apply(c);
        } else {
          parameterAst.getValue().reduce(leftFunction, rightFunction);
        }
      });
    }
  }

}
