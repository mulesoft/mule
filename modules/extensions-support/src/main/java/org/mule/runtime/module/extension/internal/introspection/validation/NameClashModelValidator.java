/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.DescribedObject;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.xml.dsl.api.resolver.SingleExtensionImportTypesStrategy;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates names clashes in the model by comparing:
 * <ul>
 * <li>The {@link NamedObject#getName()} value of all the {@link ConfigurationModel}, {@link OperationModel} and
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

    public static final String SINGULARIZED_CLASH_MESSAGE =
        "Extension '%s' contains %d parameters that clash when singularized. %s";
    public static final String NAME_CLASH_MESSAGE =
        "%s '%s' contains parameter '%s' that when transformed into DSL language clashes with parameter '%s' from '%s'";

    private final ExtensionModel extensionModel;
    private final Set<DescribedReference<NamedObject>> namedObjects = new HashSet<>();
    private final Map<String, DescribedParameter> singularizedObjects = new HashMap<>();
    private final Multimap<String, TopLevelParameter> topLevelParameters = LinkedListMultimap.create();
    private final DslSyntaxResolver dslSyntaxResolver;

    public ValidationDelegate(ExtensionModel extensionModel) {
      this.extensionModel = extensionModel;
      this.dslSyntaxResolver = new DslSyntaxResolver(extensionModel,
                                                     new SingleExtensionImportTypesStrategy());
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
          validateSingularizedNameClash(model, dslSyntaxResolver.resolve(model).getElementName());
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
        public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
          validateTopLevelParameter(model, owner);
        }

        private void defaultValidation(ParameterizedModel model) {
          validateParameterNames(model);
          registerNamedObject(model);
          validateSingularizedNameClash(model, dslSyntaxResolver.resolve(model).getElementName());
        }

        private void registerNamedObject(ParameterizedModel named) {
          namedObjects.add(new DescribedReference<>(named, dslSyntaxResolver.resolve(named).getElementName()));
        }
      }.walk(extensionModel);

      validateSingularizeNameClashesWithTopLevels();
      validateSingularizeNameClashesWithNamedObjects();
      validateNameClashes(namedObjects, topLevelParameters.values(),
                          topLevelParameters.values().stream().map(TypedTopLevelParameter::new).collect(toSet()));
    }

    private void validateOperation(OperationModel operation) {
      validateParameterNames(operation);
      String operationName = dslSyntaxResolver.resolve(operation).getElementName();
      operation.getAllParameterModels().stream().map(parameterModel -> dslSyntaxResolver.resolve(parameterModel))
          .filter(DslElementSyntax::supportsChildDeclaration)
          .forEach(parameterElement -> {

            validateClash(operationName,
                          parameterElement.getElementName(),
                          getComponentModelTypeName(operation), "argument");

            namedObjects.forEach(namedObject -> validateClash(namedObject.getName(), parameterElement.getElementName(),
                                                              namedObject.getDescription(),
                                                              format("%s named %s with an argument", operationName,
                                                                     getComponentModelTypeName(operation))));
          });
    }

    private void validateParameterNames(ParameterizedModel model) {
      Set<String> repeatedParameters = collectRepeatedNames(model.getAllParameterModels());
      if (!repeatedParameters.isEmpty()) {
        throw new IllegalModelDefinitionException(format("Extension '%s' defines the %s '%s' which has parameters "
            + "with repeated names. Offending parameters are: [%s]",
                                                         extensionModel.getName(), model.getClass().getSimpleName(),
                                                         model.getName(), Joiner.on(",").join(repeatedParameters)));
      }
    }

    private void validateTopLevelParameter(ParameterModel parameter, ParameterizedModel owner) {

      DslElementSyntax parameterElement = dslSyntaxResolver.resolve(parameter);
      if (parameterElement.supportsTopLevelDeclaration() && parameterElement.supportsChildDeclaration()) {
        final Class<?> parameterType = getType(parameter.getType());
        final String ownerName = owner.getName();
        final String ownerType = getComponentModelTypeName(owner);

        Collection<TopLevelParameter> foundParameters = topLevelParameters.get(parameterElement.getElementName());
        if (isEmpty(foundParameters)) {
          topLevelParameters.put(parameterElement.getElementName(), new TopLevelParameter(parameter, ownerName, ownerType));
        } else {
          foundParameters.stream()
              .filter(topLevelParameter -> !topLevelParameter.type.equals(parameterType))
              .findAny().ifPresent(
                                   tp -> {
                                     throw new IllegalModelDefinitionException(
                                                                               format("Extension '%s' defines an %s of name '%s' which contains parameter '%s' of complex type '%s'. However, "
                                                                                   + "%s of name '%s' defines a parameter of the same name but type '%s'. Complex parameter of different types cannot have the same name.",
                                                                                      extensionModel
                                                                                          .getName(),
                                                                                      ownerType,
                                                                                      ownerName,
                                                                                      parameterElement
                                                                                          .getElementName(),
                                                                                      parameterType,
                                                                                      tp.ownerType,
                                                                                      tp.owner,
                                                                                      tp.type
                                                                                          .getName()));
                                   });
        }
      }

    }

    private Set<String> collectRepeatedNames(List<? extends NamedObject> namedObject) {
      Set<String> names = new HashSet<>();
      return namedObject.stream().map(parameter -> dslSyntaxResolver.resolve(parameter).getElementName())
          .filter(parameter -> !names.add(parameter)).collect(toSet());
    }

    private void validateNameClashes(Collection<? extends NamedObject>... collections) {
      Multimap<String, NamedObject> names = LinkedListMultimap.create();
      stream(collections).flatMap(Collection::stream)
          .forEach(named -> names.put(dslSyntaxResolver.resolve(named).getElementName(), named));
      validateNameClashBetweenElements(names);
    }

    private void validateNameClashBetweenElements(Multimap<String, NamedObject> names) {
      names.asMap().entrySet().forEach(entry -> {
        List<NamedObject> values = (List<NamedObject>) entry.getValue();
        if (values.size() > 1) {
          Set<String> offendingTypes = values.stream().map(NamedObject::getName).collect(toSet());
          StringBuilder errorMessage =
              new StringBuilder(format("Extension '%s' contains %d components ", extensionModel.getName(), values.size()));

          final int top = offendingTypes.size() - 1;
          int i = 0;
          for (String offender : offendingTypes) {
            errorMessage.append(format("'%s'", offender));

            if (i + 1 == top) {
              errorMessage.append(" and ");
            } else if (i != top) {
              errorMessage.append(", ");
            }

            i++;
          }

          errorMessage.append(format(" which it's transformed DSL name is '%s'. DSL Names should be unique", entry.getKey()));
          throw new IllegalModelDefinitionException(errorMessage.toString());
        }
      });
    }

    private void validateSingularizeNameClashesWithTopLevels() {
      Map<String, Collection<TopLevelParameter>> singularClashes = topLevelParameters.keySet().stream()
          .filter(k -> singularizedObjects.containsKey(k) && !topLevelParameters.get(k).isEmpty())
          .collect(toMap(identity(), topLevelParameters::get));

      if (!singularClashes.isEmpty()) {
        List<String> errorMessages = new ArrayList<>();
        singularClashes.entrySet().forEach(e -> {
          DescribedParameter reference = singularizedObjects.get(e.getKey());
          e.getValue().stream()
              .filter(tp -> !Objects.equals(getType(reference.getDescribedType()), tp.type))
              .forEach(tp -> errorMessages
                  .add(format(NAME_CLASH_MESSAGE, reference.parent.getDescription(), reference.parent.getName(),
                              reference.getName(), tp.getName(), tp.ownerType)));
        });

        if (!errorMessages.isEmpty()) {
          throw new IllegalModelDefinitionException(format(SINGULARIZED_CLASH_MESSAGE,
                                                           extensionModel.getName(), singularClashes.size(),
                                                           errorMessages.stream().collect(joining(", "))));
        }
      }
    }


    private void validateSingularizeNameClashesWithNamedObjects() {
      Set<DescribedReference<NamedObject>> singularClashes = namedObjects.stream()
          .filter(k -> singularizedObjects.containsKey(k.getName()))
          .collect(toSet());

      if (!singularClashes.isEmpty()) {
        List<String> errorMessages = new ArrayList<>();
        singularClashes.forEach(namedObject -> {
          DescribedParameter reference = singularizedObjects.get(namedObject.getName());
          errorMessages.add(format(NAME_CLASH_MESSAGE, reference.parent.getDescription(), reference.parent.getName(),
                                   reference.getName(), namedObject.getDescription(), namedObject.getName()));
        });

        if (!errorMessages.isEmpty()) {
          throw new IllegalModelDefinitionException(format(SINGULARIZED_CLASH_MESSAGE,
                                                           extensionModel.getName(), singularClashes.size(),
                                                           errorMessages.stream().collect(joining(", "))));
        }
      }
    }

    private void validateClash(String existingNamingModel, String newNamingModel, String typeOfExistingNamingModel,
                               String typeOfNewNamingModel) {
      if (equalsIgnoreCase(existingNamingModel, newNamingModel)) {
        throw new IllegalModelDefinitionException(format("Extension '%s' has a %s named '%s' and an %s type named equally.",
                                                         extensionModel.getName(), typeOfExistingNamingModel, existingNamingModel,
                                                         typeOfNewNamingModel));
      }
    }

    private void validateSingularizedNameClash(ParameterizedModel model, String modelElementName) {
      List<ParameterModel> parameters = model.getAllParameterModels();
      parameters.forEach(
                         parameter -> {
                           parameter.getType().accept(new MetadataTypeVisitor() {

                             @Override
                             public void visitDictionary(DictionaryType dictionaryType) {
                               validateSingularizedChildName(dictionaryType.getValueType());
                             }

                             @Override
                             public void visitArrayType(ArrayType arrayType) {
                               validateSingularizedChildName(arrayType.getType());
                             }

                             private void validateSingularizedChildName(MetadataType type) {
                               DslElementSyntax parameterSyntax = dslSyntaxResolver.resolve(parameter);
                               String describedReference = new DescribedReference<>(model, modelElementName).getDescription();

                               parameterSyntax.getGeneric(type).filter(t -> parameterSyntax.supportsChildDeclaration())
                                   .ifPresent(childSyntax -> {
                                     singularizedObjects.put(childSyntax.getElementName(),
                                                             new DescribedParameter(parameter, parameterSyntax.getElementName(),
                                                                                    model, modelElementName, type));

                                     parameters.stream()
                                         .filter(p -> Objects.equals(dslSyntaxResolver.resolve(p).getElementName(),
                                                                     childSyntax.getElementName()))
                                         .filter(p -> !Objects.equals(getType(p.getType()), getType(type))).findAny()
                                         .ifPresent(clashParam -> {
                                           throw new IllegalModelDefinitionException(
                                                                                     format("Extension '%s' defines an %s of name '%s' which contains a parameter '%s' that when transformed to"
                                                                                         + "DSL language clashes with another parameter '%s' in the same %s",
                                                                                            extensionModel.getName(),
                                                                                            describedReference,
                                                                                            model.getName(), parameter.getName(),
                                                                                            clashParam.getName(),
                                                                                            describedReference));
                                         });
                                   });
                             }
                           });
                         });
    }
  }


  private class TopLevelParameter implements NamedObject, DescribedObject {

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

  private class DescribedReference<T extends NamedObject> extends Reference<T> implements NamedObject, DescribedObject {

    private final String elementName;

    private DescribedReference(T value, String elementName) {
      super(value);
      this.elementName = elementName;
    }

    @Override
    public String getName() {
      return elementName;
    }

    @Override
    public String getDescription() {
      NamedObject value = get();
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

    private DescribedReference<? extends NamedObject> parent;

    private MetadataType describedType;

    private DescribedParameter(ParameterModel value, String elementName, ParameterizedModel parent, String parentElementName,
                               MetadataType describedType) {
      super(value, elementName);
      this.parent = new DescribedReference<>(parent, parentElementName);
      this.describedType = describedType;
    }

    public MetadataType getDescribedType() {
      return describedType;
    }
  }

}
