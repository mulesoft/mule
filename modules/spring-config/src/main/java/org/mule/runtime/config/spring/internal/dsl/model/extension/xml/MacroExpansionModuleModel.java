/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.model.extension.xml;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.NAME_ATTRIBUTE;
import static org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.el.BindingContextUtils.PARAMETERS;
import static org.mule.runtime.internal.el.BindingContextUtils.PROPERTIES;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.config.spring.api.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.api.dsl.model.ComponentModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * A {@link MacroExpansionModuleModel} works tightly with a {@link ApplicationModel} to go over all the registered
 * {@link ExtensionModel}s that are XML based (see {@link XmlExtensionModelProperty}) looking for code to macro expand.
 * <p/>
 * For every occurrence that happens, it will expand the operations.
 * <p/>
 * This object works by handling {@link ComponentModel}s directly, consuming the {@link GlobalElementComponentModelModelProperty}
 * for the "config" elements while the {@link OperationComponentModelModelProperty} for the operations (aka: {@link Processor}s in
 * the XML file).
 *
 * @since 4.0
 */
public class MacroExpansionModuleModel {

  private static final String MODULE_OPERATION_CONFIG_REF = "config-ref";
  /**
   * Used to obtain the {@link ComponentIdentifier} element from the <module/>'s original {@ink ComponentModel} to be later added
   * in the macro expanded element (aka: <module-operation-chain ../>) so that the location set by the
   * {@link org.mule.runtime.config.spring.internal.dsl.model.ComponentLocationVisitor} can properly set the paths for every
   * element (even the macro expanded)
   */
  public static final String ORIGINAL_IDENTIFIER = "ORIGINAL_IDENTIFIER";

  private final ApplicationModel applicationModel;
  private final List<ExtensionModel> extensions;

  /**
   * From a mutable {@code applicationModel}, it will store it to apply changes when the {@link #expand()} method is executed.
   *
   * @param applicationModel to modify given the usages of elements that belong to the {@link ExtensionModel}s contained in the
   *        {@code extensions} map.
   * @param extensions set with all the loaded {@link ExtensionModel}s from the deployment that will be filtered by looking up
   *        only those that are coming from an XML context through the {@link XmlExtensionModelProperty} property.
   */
  public MacroExpansionModuleModel(ApplicationModel applicationModel, Set<ExtensionModel> extensions) {
    this.applicationModel = applicationModel;
    this.extensions = extensions.stream()
        .filter(extensionModel -> extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent())
        .collect(toList());
  }

  /**
   * Goes through the entire xml mule application looking for the message processors that can be expanded, and then takes care of
   * the global elements.
   */
  public void expand() {
    for (int i = 0; i < extensions.size(); i++) {
      for (ExtensionModel extensionModel : extensions) {
        expand(extensionModel);
      }
    }
  }

  private void expand(ExtensionModel extensionModel) {
    final List<ComponentModel> moduleGlobalElements = getModuleGlobalElements(extensionModel);
    final Set<String> moduleGlobalElementsNames =
        moduleGlobalElements.stream().map(ComponentModel::getNameAttribute).collect(toSet());
    createOperationRefEffectiveModel(extensionModel, moduleGlobalElementsNames);
    createConfigRefEffectiveModel(extensionModel, moduleGlobalElements, moduleGlobalElementsNames);
  }

  private void createOperationRefEffectiveModel(ExtensionModel extensionModel, Set<String> moduleGlobalElementsNames) {
    HashMap<Integer, ComponentModel> componentModelsToReplaceByIndex = new HashMap<>();
    applicationModel.executeOnEveryMuleComponentTree(flowComponentModel -> {
      for (int i = 0; i < flowComponentModel.getInnerComponents().size(); i++) {
        ComponentModel operationRefModel = flowComponentModel.getInnerComponents().get(i);
        ComponentIdentifier identifier = operationRefModel.getIdentifier();
        String identifierName = identifier.getName();
        if (identifierName.equals(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)) {
          // config elements will be worked later on, that's why we are skipping this element
          continue;
        }
        if (extensionModel.getXmlDslModel().getPrefix().equals(identifier.getNamespace())) {

          HasOperationModels hasOperationModels = extensionModel;
          final Optional<ConfigurationModel> configurationModel =
              extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);
          if (configurationModel.isPresent()) {
            hasOperationModels = configurationModel.get();
          }

          Optional<OperationModel> operationModel = hasOperationModels.getOperationModel(identifierName);
          if (operationModel.isPresent()) {
            ComponentModel replacementModel =
                createOperationInstance(operationRefModel, extensionModel, operationModel.get(), moduleGlobalElementsNames);
            componentModelsToReplaceByIndex.put(i, replacementModel);
          } else {
            // as the #executeOnEveryMuleComponentTree goes from bottom to top, before throwing an exception we need to check if
            // the current operationRefModel's parent is an operation of the current ExtensionModel, meaning that the role of the
            // parameter is either CONTENT or PRIMARY_CONTENT
            final ComponentIdentifier parentIdentifier = operationRefModel.getParent().getIdentifier();
            final String parentIdentifierName = parentIdentifier.getName();
            if (!hasOperationModels.getOperationModel(parentIdentifierName).isPresent()) {
              throw new IllegalArgumentException(format("The operation '%s' is missing in the module '%s'", identifierName,
                                                        extensionModel.getName()));
            }
          }
        }
      }
      for (Map.Entry<Integer, ComponentModel> entry : componentModelsToReplaceByIndex.entrySet()) {
        entry.getValue().setParent(flowComponentModel);
        flowComponentModel.getInnerComponents().add(entry.getKey(), entry.getValue());
        flowComponentModel.getInnerComponents().remove(entry.getKey() + 1);
      }
      componentModelsToReplaceByIndex.clear();
    });
  }

  private void createConfigRefEffectiveModel(ExtensionModel extensionModel, List<ComponentModel> moduleComponentModels,
                                             Set<String> moduleGlobalElementsNames) {

    applicationModel.executeOnEveryMuleComponentTree(componentModel -> {
      HashMap<ComponentModel, List<ComponentModel>> componentModelsToReplaceByIndex = new HashMap<>();

      for (int i = 0; i < componentModel.getInnerComponents().size(); i++) {
        ComponentModel configRefModel = componentModel.getInnerComponents().get(i);
        ComponentIdentifier identifier = configRefModel.getIdentifier();

        if (extensionModel.getXmlDslModel().getPrefix().equals(identifier.getNamespace())) {

          Map<String, String> propertiesMap = extractParameters(configRefModel,
                                                                extensionModel
                                                                    .getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)
                                                                    .get()
                                                                    .getAllParameterModels());
          final Map<String, String> literalsParameters = getLiteralParameters(propertiesMap, emptyMap());

          List<ComponentModel> replacementGlobalElements =
              createGlobalElementsInstance(configRefModel, moduleComponentModels, moduleGlobalElementsNames, literalsParameters);
          componentModelsToReplaceByIndex.put(configRefModel, replacementGlobalElements);
        }
      }
      for (Map.Entry<ComponentModel, List<ComponentModel>> entry : componentModelsToReplaceByIndex.entrySet()) {
        final int componentModelIndex = componentModel.getInnerComponents().indexOf(entry.getKey());
        componentModel.getInnerComponents().addAll(componentModelIndex, entry.getValue());
        componentModel.getInnerComponents().remove(componentModelIndex + entry.getValue().size());
      }
    });
  }

  private List<ComponentModel> createGlobalElementsInstance(ComponentModel configRefModel,
                                                            List<ComponentModel> moduleGlobalElements,
                                                            Set<String> moduleGlobalElementsNames,
                                                            Map<String, String> literalsParameters) {

    List<ComponentModel> globalElementsModel = new ArrayList<>();
    globalElementsModel.addAll(moduleGlobalElements.stream()
        .map(globalElementModel -> copyComponentModel(globalElementModel, configRefModel.getNameAttribute(),
                                                      moduleGlobalElementsNames, literalsParameters))
        .collect(Collectors.toList()));

    ComponentModel muleRootElement = configRefModel.getParent();
    globalElementsModel.stream().forEach(componentModel -> {
      componentModel.setRoot(true);
      componentModel.setParent(muleRootElement);
    });

    return globalElementsModel;
  }

  private List<ComponentModel> getModuleGlobalElements(ExtensionModel extensionModel) {
    List<ComponentModel> moduleGlobalElements = new ArrayList<>();
    Optional<ConfigurationModel> config = extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);
    if (config.isPresent() && config.get().getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent()) {
      GlobalElementComponentModelModelProperty globalElementComponentModelModelProperty =
          config.get().getModelProperty(GlobalElementComponentModelModelProperty.class).get();
      moduleGlobalElements = globalElementComponentModelModelProperty.getGlobalElements();
    }
    return moduleGlobalElements;
  }

  /**
   * Takes a one liner call to any given message processor, expand it to creating a "module-operation-chain" scope which has the
   * set of properties, the set of parameters and the list of message processors to execute.
   *
   * @param operationRefModel message processor that will be replaced by a scope element named "module-operation-chain".
   * @param extensionModel extension that holds a possible set of <property/>s that has to be parametrized to the new scope.
   * @param operationModel operation that provides both the <parameter/>s and content of the <body/>
   * @param moduleGlobalElementsNames collection with the global components names (such as <http:config name="a"../>, <file:config
   *        name="b"../>, <file:matcher name="c"../> and so on) that are contained within the <module/> that will be macro
   *        expanded
   * @return a new component model that represents the old placeholder but expanded with the content of the <body/>
   */
  private ComponentModel createOperationInstance(ComponentModel operationRefModel, ExtensionModel extensionModel,
                                                 OperationModel operationModel, Set<String> moduleGlobalElementsNames) {
    final OperationComponentModelModelProperty operationComponentModelModelProperty =
        operationModel.getModelProperty(OperationComponentModelModelProperty.class).get();
    final ComponentModel operationModuleComponentModel = operationComponentModelModelProperty
        .getBodyComponentModel();
    List<ComponentModel> bodyProcessors = operationModuleComponentModel.getInnerComponents();

    String configRefName = operationRefModel.getParameters().get(MODULE_OPERATION_CONFIG_REF);

    ComponentModel.Builder processorChainBuilder = new ComponentModel.Builder();
    processorChainBuilder
        .setIdentifier(builder().namespace(CORE_PREFIX).name("module-operation-chain").build());

    processorChainBuilder.addParameter("moduleName", extensionModel.getXmlDslModel().getPrefix(), false);
    processorChainBuilder.addParameter("moduleOperation", operationModel.getName(), false);
    Map<String, String> propertiesMap = extractProperties(operationRefModel, extensionModel);
    Map<String, String> parametersMap = extractParameters(operationRefModel, operationModel.getAllParameterModels());
    ComponentModel propertiesComponentModel =
        getParameterChild(propertiesMap, "module-operation-properties", "module-operation-property-entry");
    ComponentModel parametersComponentModel =
        getParameterChild(parametersMap, "module-operation-parameters", "module-operation-parameter-entry");
    processorChainBuilder.addChildComponentModel(propertiesComponentModel);
    processorChainBuilder.addChildComponentModel(parametersComponentModel);

    final Map<String, String> literalsParameters = getLiteralParameters(propertiesMap, parametersMap);
    for (ComponentModel bodyProcessor : bodyProcessors) {
      processorChainBuilder.addChildComponentModel(copyComponentModel(bodyProcessor, configRefName, moduleGlobalElementsNames,
                                                                      literalsParameters));
    }
    for (Map.Entry<String, Object> customAttributeEntry : operationRefModel.getCustomAttributes().entrySet()) {
      processorChainBuilder.addCustomAttribute(customAttributeEntry.getKey(), customAttributeEntry.getValue());
    }
    ComponentModel processorChainModel = processorChainBuilder.build();
    for (ComponentModel processorChainModelChild : processorChainModel.getInnerComponents()) {
      processorChainModelChild.setParent(processorChainModel);
    }

    operationRefModel.getConfigFileName().ifPresent(processorChainBuilder::setConfigFileName);
    operationRefModel.getLineNumber().ifPresent(processorChainBuilder::setLineNumber);
    processorChainBuilder.addCustomAttribute(ORIGINAL_IDENTIFIER, operationRefModel.getIdentifier());
    return processorChainModel;
  }

  /**
   * @param propertiesMap <property>s that are feed in the current usage of the <module/>
   * @param parametersMap <param>s that are feed in the current usage of the <module/>
   * @return a {@link Map} of <property>s and <parameter>s that could be replaced by their literal values, see
   *         {@link #copyComponentModel(ComponentModel, String, Set, Map)}
   */
  private Map<String, String> getLiteralParameters(Map<String, String> propertiesMap, Map<String, String> parametersMap) {
    final Map<String, String> literalsParameters = propertiesMap.entrySet().stream()
        .filter(entry -> !isExpression(entry.getValue()))
        .collect(Collectors.toMap(e -> getReplaceableExpression(e.getKey(), PROPERTIES),
                                  Map.Entry::getValue));
    literalsParameters.putAll(
                              parametersMap.entrySet().stream()
                                  .filter(entry -> !isExpression(entry.getValue()))
                                  .collect(Collectors.toMap(
                                                            e -> getReplaceableExpression(e.getKey(), PARAMETERS),
                                                            Map.Entry::getValue)));
    return literalsParameters;
  }

  /**
   * Assembly an expression to validate if the macro expansion of the current <module> can be directly replaced by the literals
   * value
   *
   * @param name of the parameter (either a <property> or a <parameter>)
   * @param prefix binding to append for the expression to be replaced in the <module>'s code
   * @return the expression that access a variable through a direct binding (aka: a "static expression", as it doesn't use the
   *         {@link Event})
   */
  private String getReplaceableExpression(String name, String prefix) {
    return "#[" + prefix + "." + name + "]";
  }

  private boolean isExpression(String value) {
    return value.startsWith("#[") && value.endsWith("]");
  }

  private ComponentModel getParameterChild(Map<String, String> parameters, String wrapperParameters, String entryParameter) {
    ComponentModel.Builder parametersBuilder = new ComponentModel.Builder();
    parametersBuilder
        .setIdentifier(builder().namespace(CORE_PREFIX).name(wrapperParameters).build());
    parameters.forEach((paramName, paramValue) -> {
      ComponentModel.Builder parameterBuilder = new ComponentModel.Builder();
      parameterBuilder.setIdentifier(builder().namespace(CORE_PREFIX)
          .name(entryParameter).build());

      parameterBuilder.addParameter(KEY_ATTRIBUTE_NAME, paramName, false);
      parameterBuilder.addParameter(VALUE_ATTRIBUTE_NAME, paramValue, false);
      parametersBuilder.addChildComponentModel(parameterBuilder.build());
    });

    ComponentModel parametersComponentModel = parametersBuilder.build();
    for (ComponentModel parameterComponentModel : parametersComponentModel.getInnerComponents()) {
      parameterComponentModel.setParent(parametersComponentModel);
    }
    return parametersComponentModel;
  }

  private Map<String, String> extractProperties(ComponentModel operationRefModel, ExtensionModel extensionModel) {
    Map<String, String> valuesMap = new HashMap<>();
    // extract the <properties>
    String configParameter = operationRefModel.getParameters().get(MODULE_OPERATION_CONFIG_REF);
    if (configParameter != null) {
      ComponentModel configRefComponentModel = applicationModel.getRootComponentModel().getInnerComponents().stream()
          .filter(componentModel -> componentModel.getIdentifier().getNamespace()
              .equals(extensionModel.getXmlDslModel().getPrefix())
              && componentModel.getIdentifier().getName().equals(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)
              && configParameter.equals(componentModel.getParameters().get(NAME_ATTRIBUTE)))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(
                                                          format("There's no <%s:config> named [%s] in the current mule app",
                                                                 extensionModel.getXmlDslModel().getPrefix(), configParameter)));
      valuesMap
          .putAll(extractParameters(configRefComponentModel,
                                    extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME).get()
                                        .getAllParameterModels()));
    }
    return valuesMap;
  }

  /**
   * Iterates over the collection of {@link ParameterModel}s making a clear distinction between {@link ParameterRole#BEHAVIOUR}
   * and {@link ParameterRole#CONTENT} or {@link ParameterRole#PRIMARY_CONTENT} roles, where the former maps to simple attributes
   * while the latter are child elements.
   * <p/>
   * If the value of the parameter is missing, then it will try to pick up a default value (also from the
   * {@link ParameterModel#getDefaultValue()})
   *
   * @param componentModel to look for the values
   * @param parameters collection of parameters to look for in the parametrized {@link ComponentModel}
   * @return a {@link Map} with the values to be macro expanded in the final mule application
   */
  private Map<String, String> extractParameters(ComponentModel componentModel, List<ParameterModel> parameters) {
    Map<String, String> valuesMap = new HashMap<>();
    for (ParameterModel parameterExtension : parameters) {
      String paramName = parameterExtension.getName();
      String value = null;

      switch (parameterExtension.getRole()) {
        case BEHAVIOUR:
          if (componentModel.getParameters().containsKey(paramName)) {
            value = componentModel.getParameters().get(paramName);
          }
          break;
        case CONTENT:
        case PRIMARY_CONTENT:
          final Optional<ComponentModel> childComponentModel = componentModel.getInnerComponents().stream()
              .filter(cm -> paramName.equals(cm.getIdentifier().getName()))
              .findFirst();
          if (childComponentModel.isPresent()) {
            value = childComponentModel.get().getTextContent();
          }
          break;
      }

      if (value == null && (parameterExtension.getDefaultValue() != null)) {
        value = String.valueOf(parameterExtension.getDefaultValue());
      }
      if (value != null) {
        valuesMap.put(paramName, value);
      }
    }
    return valuesMap;
  }

  /**
   * Goes over the {@code modelToCopy} by consuming the attributes as they are, unless some of them are actually targeting a
   * global component (such as a configuration), in which it will append the {@code configRefName} to that reference, which will
   * be the definitive name once the Mule application has been completely macro expanded in the final XML configuration.
   *
   * @param modelToCopy original source of truth that comes from the <module/>
   * @param configRefName name of the configuration being used in the Mule application
   * @param moduleGlobalElementsNames names of the <module/>s global component that will be macro expanded in the Mule application
   * @param literalsParameters {@link Map} with all he <property>s and <parameter>s that were feed with a literal value in the
   *        Mule application's code.
   * @return a transformed {@link ComponentModel} from the {@code modelToCopy}, where the global element's attributes has been
   *         updated accordingly (both global components updates plus the line number, and so on). If the value for some parameter
   *         can be optimized by replacing it for the literal's value, it will be done as well using the
   *         {@code literalsParameters}
   */
  private ComponentModel copyComponentModel(ComponentModel modelToCopy, String configRefName,
                                            Set<String> moduleGlobalElementsNames,
                                            Map<String, String> literalsParameters) {
    ComponentModel.Builder operationReplacementModel = new ComponentModel.Builder();
    operationReplacementModel
        .setIdentifier(modelToCopy.getIdentifier())
        .setTextContent(modelToCopy.getTextContent());
    for (Map.Entry<String, Object> entry : modelToCopy.getCustomAttributes().entrySet()) {
      operationReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : modelToCopy.getParameters().entrySet()) {
      validateConfigRefAttribute(modelToCopy, configRefName, entry.getValue());
      String value = calculateAttributeValue(configRefName, moduleGlobalElementsNames, entry.getValue());
      final String optimizedValue = literalsParameters.getOrDefault(value, value);
      operationReplacementModel.addParameter(entry.getKey(), optimizedValue, false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      operationReplacementModel.addChildComponentModel(
                                                       copyComponentModel(operationChildModel, configRefName,
                                                                          moduleGlobalElementsNames, literalsParameters));
    }

    final String configFileName = modelToCopy.getConfigFileName()
        .orElseThrow(() -> new IllegalArgumentException("The is no config file name for the component to macro expand"));
    final Integer lineNumber = modelToCopy.getLineNumber()
        .orElseThrow(() -> new IllegalArgumentException("The is no line number for the component to macro expand"));
    operationReplacementModel.setConfigFileName(configFileName);
    operationReplacementModel.setLineNumber(lineNumber);

    ComponentModel componentModel = operationReplacementModel.build();
    for (ComponentModel child : componentModel.getInnerComponents()) {
      child.setParent(componentModel);
    }
    return componentModel;
  }

  // TODO MULE-12526: once implemented, remove this validation as it will be done through XSD (config-ref must be mandatory in
  // each operation for Smart Connectors)
  private void validateConfigRefAttribute(ComponentModel modelToCopy, String configRefName, String originalValue) {
    if (MODULE_OPERATION_CONFIG_REF.equals(configRefName) && StringUtils.isBlank(originalValue)) {
      throw new IllegalArgumentException(format("The operation '%s' is missing the '%s' attribute",
                                                modelToCopy.getIdentifier().getName(), MODULE_OPERATION_CONFIG_REF));
    }
  }

  // TODO MULE-9849: until there's no clear way to check against the ComponentModel using the
  // org.mule.runtime.config.spring.dsl.processor.AbstractAttributeDefinitionVisitor.onReferenceSimpleParameter(), we workaround
  // the issue by checking every <module/>'s global element's name.
  private String calculateAttributeValue(String configRefNameToAppend, Set<String> moduleGlobalElementsNames,
                                         String originalValue) {
    return moduleGlobalElementsNames.contains(originalValue)
        ? originalValue.concat("-").concat(configRefNameToAppend)
        : originalValue;
  }

}
