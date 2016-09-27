/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.singularize;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.hasExposedFields;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isInstantiable;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.Described;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.HasConnectionProviderModels;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.xml.dsl.api.property.XmlHintsModelProperty;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Validates names clashes in the model by comparing:
 * <ul>
 * <li>The {@link Named#getName()} value of all the {@link ConfigurationModel}, {@link OperationModel} and
 * {@link ConnectionProviderModel}</li>
 * <li>Makes sure that there no two {@link ParameterModel}s with the same name but different types, for those which represent an
 * object</li>
 * <li>Makes sure that no {@link ConfigurationModel}, {@link OperationModel} or {@link ConnectionProviderModel} have parameters
 * with repeated name</li>
 * </ul>
 *
 * @since 4.0
 */
public final class NameClashModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel model) throws IllegalModelDefinitionException {
    new ValidationDelegate(model).validate(model);
  }

  private class ValidationDelegate {

    private final ExtensionModel extensionModel;
    private final Set<DescribedReference<Named>> namedObjects = new HashSet<>();
    private final Map<String, DescribedParameter> singularizedObjects = new HashMap<>();
    private final Multimap<String, TopLevelParameter> topLevelParameters = LinkedListMultimap.create();

    public ValidationDelegate(ExtensionModel extensionModel) {
      this.extensionModel = extensionModel;
    }

    private void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {

      new ExtensionWalker() {

        @Override
        public void onConfiguration(ConfigurationModel model) {
          defaultValidation(model);
        }

        @Override
        public void onOperation(HasOperationModels owner, OperationModel model) {
          validateOperation(model);
          registerNamedObject(model);
          validateSingularizingClash(model);
        }

        @Override
        public void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
          defaultValidation(model);
        }

        @Override
        public void onSource(HasSourceModels owner, SourceModel model) {
          defaultValidation(model);
        }

        @Override
        public void onParameter(ParameterizedModel owner, ParameterModel model) {
          validateTopLevelParameter(model, owner);
        }

        private void defaultValidation(ParameterizedModel model) {
          validateParameterNames(model);
          registerNamedObject(model);
          validateSingularizingClash(model);
        }

        private void registerNamedObject(Named named) {
          namedObjects.add(new DescribedReference<>(named));
        }
      }.walk(extensionModel);

      validateNameClashes(namedObjects, topLevelParameters.values(),
                          topLevelParameters.values().stream().map(TypedTopLevelParameter::new).collect(toSet()));
    }

    private void validateOperation(OperationModel operation) {
      validateParameterNames(operation);
      // Check clash between each operation and its parameters type
      operation.getParameterModels().forEach(parameterModel -> validateClash(operation.getName(),
                                                                             getType(parameterModel.getType()).getName(),
                                                                             "operation", "argument"));
    }

    private void validateParameterNames(ParameterizedModel model) {
      Set<String> repeatedParameters = collectRepeatedNames(model.getParameterModels());
      if (!repeatedParameters.isEmpty()) {
        throw new IllegalModelDefinitionException(format("Extension '%s' defines the %s '%s' which has parameters "
            + "with repeated names. Offending parameters are: [%s]",
                                                         extensionModel.getName(), model.getClass().getSimpleName(),
                                                         model.getName(), Joiner.on(",").join(repeatedParameters)));
      }
    }

    private void validateTopLevelParameter(ParameterModel parameter, ParameterizedModel owner) {
      MetadataType metadataType = parameter.getType();
      if (!isInstantiable(metadataType) || !hasExposedFields(metadataType)) {
        return;
      }

      final Class<?> parameterType = getType(metadataType);
      final String ownerName = owner.getName();
      final String ownerType = owner.getClass().getSimpleName();

      Collection<TopLevelParameter> foundParameters = topLevelParameters.get(parameter.getName());
      if (CollectionUtils.isEmpty(foundParameters)) {
        Optional<XmlHintsModelProperty> hints = parameter.getModelProperty(XmlHintsModelProperty.class);
        boolean allowsInline = hints.map(XmlHintsModelProperty::allowsInlineDefinition).orElse(true);

        if (allowsInline) {
          topLevelParameters.put(parameter.getName(), new TopLevelParameter(parameter, ownerName, ownerType));
        }
      } else {
        Optional<TopLevelParameter> repeated =
            foundParameters.stream().filter(topLevelParameter -> !topLevelParameter.type.equals(parameterType)).findFirst();

        if (repeated.isPresent()) {
          TopLevelParameter tp = repeated.get();
          throw new IllegalModelDefinitionException(
                                                    format("Extension '%s' defines an %s of name '%s' which contains parameter '%s' of complex type '%s'. However, "
                                                        + "%s of name '%s' defines a parameter of the same name but type '%s'. Complex parameter of different types cannot have the same name.",
                                                           extensionModel.getName(),
                                                           ownerType, ownerName,
                                                           parameter.getName(),
                                                           parameterType,
                                                           tp.ownerType, tp.owner, tp.type.getName()));
        }
      }
    }

    private Set<String> collectRepeatedNames(List<? extends Named> namedObject) {
      Set<String> names = new HashSet<>();
      return namedObject.stream()
          .filter(parameter -> !names.add(parameter.getName()))
          .map(Named::getName).collect(toSet());
    }

    private void validateNameClashes(Collection<? extends Named>... collections) {
      Multimap<String, Named> names = LinkedListMultimap.create();
      stream(collections).flatMap(Collection::stream).forEach(named -> names.put(named.getName(), named));

      Map<String, Named> singularClashes = names
          .entries()
          .stream()
          .filter(e -> singularizedObjects.keySet().contains(e.getKey()))
          .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

      if (!singularClashes.isEmpty()) {
        StringBuilder builder = new StringBuilder(format("Extension '%s' contains %d parameters that clash when singularized. ",
                                                         extensionModel.getName(), singularClashes.size()));

        builder.append(singularClashes.keySet()
            .stream()
            .map(k -> {
              DescribedParameter reference = singularizedObjects.get(k);
              Collection<Named> namedElements = names.get(k);
              if (!namedElements.isEmpty()) {
                Named next = namedElements.iterator().next();
                return format(
                              "%s name '%s' contains parameter '%s' that clash when singularized with parameter '%s' in '%s'",
                              reference.parent.getDescription(), reference.parent.getName(), reference.getName(), k,
                              next.getName());
              } else {
                return "";
              }
            })
            .collect(joining(", ")));

        throw new IllegalModelDefinitionException(builder.toString());
      }

      names.asMap().entrySet().forEach(entry -> {
        List<Named> values = (List<Named>) entry.getValue();
        if (values.size() > 1) {
          Set<String> offendingTypes = values.stream().map(Named::getName).collect(toSet());
          StringBuilder errorMessage =
              new StringBuilder(format("Extension '%s' contains %d ", extensionModel.getName(), values.size()));

          final int top = offendingTypes.size() - 1;
          int i = 0;
          for (String offender : offendingTypes) {
            errorMessage.append(offender);

            if (i + 1 == top) {
              errorMessage.append(" and ");
            } else if (i != top) {
              errorMessage.append(", ");
            }

            i++;
          }

          errorMessage.append(format(" which name is '%s'. Names should be unique", entry.getKey()));
          throw new IllegalModelDefinitionException(errorMessage.toString());
        }
      });
    }

    private void validateClash(String existingNamingModel, String newNamingModel, String typeOfExistingNamingModel,
                               String typeOfNewNamingModel) {
      if (StringUtils.equalsIgnoreCase(existingNamingModel, newNamingModel)) {
        throw new IllegalModelDefinitionException(format("Extension '%s' has a %s named '%s' with an %s type named equally.",
                                                         extensionModel.getName(), typeOfExistingNamingModel, existingNamingModel,
                                                         typeOfNewNamingModel));
      }
    }

    private void validateSingularizingClash(ParameterizedModel model) {
      List<ParameterModel> parameters = model.getParameterModels();
      parameters.forEach(
                         p -> p.getType().accept(new MetadataTypeVisitor() {

                           @Override
                           public void visitArrayType(ArrayType arrayType) {
                             validateSingular();
                           }

                           @Override
                           public void visitDictionary(DictionaryType dictionaryType) {
                             validateSingular();
                           }

                           private void validateSingular() {
                             String singularName = singularize(p.getName());
                             boolean allowInline = p.getModelProperty(XmlHintsModelProperty.class)
                                 .map(XmlHintsModelProperty::allowsInlineDefinition)
                                 .orElse(true);

                             if (singularName.equals(p.getName()) || !allowInline) {
                               return;
                             }

                             Optional<ParameterModel> clashingParam =
                                 parameters.stream().filter(p -> p.getName().equals(singularName)).findAny();
                             String describedReference = new DescribedReference<>(model).getDescription();
                             Class<Object> type = getType(p.getType());
                             String extensionName = extensionModel.getName();

                             if (clashingParam.isPresent()) {
                               throw new IllegalModelDefinitionException(format("Extension '%s' defines an %s of name '%s' which contains a '%s' "
                                   + "parameter '%s' that when singularized clashes with another "
                                   + "parameter '%s' in the same %s", extensionName,
                                                                                describedReference, model.getName(),
                                                                                type.getSimpleName(),
                                                                                p.getName(), clashingParam.get().getName(),
                                                                                describedReference));
                             }

                             Optional<String> nameClash =
                                 topLevelParameters.keySet().stream().filter(name -> name.equals(singularName)).findAny();
                             if (nameClash.isPresent()) {
                               Collection<TopLevelParameter> tps = topLevelParameters.get(nameClash.get());
                               if (!tps.isEmpty()) {
                                 TopLevelParameter tp = tps.iterator().next();
                                 throw new IllegalModelDefinitionException(
                                                                           format("Extension '%s' defines an %s of name '%s' which contains a '%s' parameter '%s' that when singularized "
                                                                               + "clashes with a parameter '%s' defined in the %s '%s'",
                                                                                  extensionName,
                                                                                  describedReference, model.getName(),
                                                                                  type.getSimpleName(), p.getName(),
                                                                                  tp.parameterModel.getName(), tp.owner,
                                                                                  tp.ownerType));
                               }
                             }
                             singularizedObjects.put(singularName, new DescribedParameter(p, model));
                           }
                         }));
    }
  }


  private class TopLevelParameter implements Named, Described {

    protected final ParameterModel parameterModel;
    protected final String owner;
    protected final String ownerType;
    protected final Class<?> type;

    private TopLevelParameter(ParameterModel parameterModel, String owner, String ownerType) {
      this.parameterModel = parameterModel;
      this.owner = owner;
      this.ownerType = ownerType;
      type = getType(parameterModel.getType());
    }

    @Override
    public String getName() {
      return parameterModel.getName();
    }

    @Override
    public String getDescription() {
      return "top level parameter";
    }
  }


  private class TypedTopLevelParameter extends TopLevelParameter {

    public TypedTopLevelParameter(TopLevelParameter parameter) {
      super(parameter.parameterModel, parameter.owner, parameter.ownerType);
    }

    @Override
    public String getName() {
      return type.getName();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TypedTopLevelParameter && type.equals(((TypedTopLevelParameter) obj).type);

    }

    @Override
    public int hashCode() {
      return type.hashCode();
    }
  }


  private class DescribedReference<T extends Named> extends Reference<T> implements Named, Described {

    private DescribedReference(T value) {
      super(value);
    }

    @Override
    public String getName() {
      return get().getName();
    }

    @Override
    public String getDescription() {
      Named value = get();
      if (value instanceof ConfigurationModel) {
        return "configuration";
      } else if (value instanceof OperationModel) {
        return "operation";
      } else if (value instanceof SourceModel) {
        return "message source";
      } else if (value instanceof ConnectionProviderModel) {
        return "connection provider";
      }

      return "";
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof DescribedReference && super.equals(obj);

    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }


  private class DescribedParameter extends DescribedReference<ParameterModel> {

    private DescribedReference<? extends Named> parent;

    private DescribedParameter(ParameterModel value, ParameterizedModel parent) {
      super(value);
      this.parent = new DescribedReference<>(parent);
    }
  }
}
