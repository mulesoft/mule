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
import static org.mule.runtime.api.el.BindingContextUtils.PARAMETERS;
import static org.mule.runtime.api.el.BindingContextUtils.PROPERTIES;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.NAME_ATTRIBUTE;
import static org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import org.apache.commons.lang3.StringUtils;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.config.spring.api.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.api.dsl.model.ComponentModel;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

  /**
   * Reserved prefix in a <module/> to define a reference an operation of the same module (no circular dependencies allowed)
   */
  public static final String TNS_PREFIX = "tns";

  /**
   * literal that represents the name of the connection element for any given module. If the module's name is github, then the
   * value of this field will name the global element as <github:connection ../>. As an example, think of the following snippet:
   *
   * <code>
   *    <github:config configParameter="someFood" ...>
   *      <github:connection username="myUsername" .../>
   *    </github:config>
   * </code>
   */
  public static final String MODULE_CONNECTION_GLOBAL_ELEMENT_NAME = "connection";

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
    Optional<String> testConnectionGlobalElementName = getTestConnectionGlobalElement(extensionModel);
    applicationModel.executeOnEveryMuleComponentTree(flowComponentModel -> {
      HashMap<Integer, ComponentModel> componentModelsToReplaceByIndex = new HashMap<>();
      for (int i = 0; i < flowComponentModel.getInnerComponents().size(); i++) {
        ComponentModel operationRefModel = flowComponentModel.getInnerComponents().get(i);
        ComponentIdentifier identifier = operationRefModel.getIdentifier();
        String identifierName = identifier.getName();
        if (identifierName.equals(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)) {
          // config elements will be worked later on, that's why we are skipping this element
          continue;
        }
        if (extensionModel.getXmlDslModel().getPrefix().equals(identifier.getNamespace())) {
          Optional<OperationModel> operationModel = lookForOperation(extensionModel, operationRefModel);
          if (operationModel.isPresent()) {
            ComponentModel replacementModel =
                createModuleOperationChain(operationRefModel, extensionModel, operationModel.get(), moduleGlobalElementsNames,
                                           testConnectionGlobalElementName, Optional.empty());
            componentModelsToReplaceByIndex.put(i, replacementModel);
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

  private Optional<String> getTestConnectionGlobalElement(ExtensionModel extensionModel) {
    return getConfigurationModel(extensionModel)
        .flatMap(this::getTestConnectionGlobalElement);
  }

  private Optional<String> getTestConnectionGlobalElement(ConfigurationModel configurationModel) {
    final Optional<ConnectionProviderModel> connectionProviderModel =
        configurationModel.getConnectionProviderModel(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME);
    if (connectionProviderModel.isPresent()) {
      final Optional<TestConnectionGlobalElementModelProperty> modelProperty =
          connectionProviderModel.get().getModelProperty(TestConnectionGlobalElementModelProperty.class);
      return modelProperty.map(TestConnectionGlobalElementModelProperty::getGlobalElementName);
    } else {
      return Optional.empty();
    }

  }

  private void createConfigRefEffectiveModel(ExtensionModel extensionModel, List<ComponentModel> moduleComponentModels,
                                             Set<String> moduleGlobalElementsNames) {
    applicationModel.executeOnEveryMuleComponentTree(componentModel -> {
      HashMap<ComponentModel, List<ComponentModel>> componentModelsToReplaceByIndex = new HashMap<>();

      for (int i = 0; i < componentModel.getInnerComponents().size(); i++) {
        ComponentModel configRefModel = componentModel.getInnerComponents().get(i);
        ComponentIdentifier identifier = configRefModel.getIdentifier();

        if (extensionModel.getXmlDslModel().getPrefix().equals(identifier.getNamespace())) {

          final ConfigurationModel configurationModel = getConfigurationModel(extensionModel).get();
          Map<String, String> propertiesMap = extractParameters(configRefModel,
                                                                configurationModel
                                                                    .getAllParameterModels());
          final Map<String, String> literalsParameters = getLiteralParameters(propertiesMap, emptyMap());

          List<ComponentModel> replacementGlobalElements =
              createGlobalElementsInstance(configRefModel, moduleComponentModels, moduleGlobalElementsNames, literalsParameters,
                                           configurationModel);
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

  private Optional<ConfigurationModel> getConfigurationModel(ExtensionModel extensionModel) {
    return extensionModel
        .getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);
  }

  private List<ComponentModel> createGlobalElementsInstance(ComponentModel configRefModel,
                                                            List<ComponentModel> moduleGlobalElements,
                                                            Set<String> moduleGlobalElementsNames,
                                                            Map<String, String> literalsParameters,
                                                            ConfigurationModel configurationModel) {

    List<ComponentModel> globalElementsModel = new ArrayList<>();
    globalElementsModel.addAll(moduleGlobalElements.stream()
        .map(globalElementModel -> copyGlobalElementComponentModel(globalElementModel,
                                                                   Optional.of(configRefModel.getNameAttribute()),
                                                                   moduleGlobalElementsNames, literalsParameters,
                                                                   configurationModel))
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
    Optional<ConfigurationModel> config = getConfigurationModel(extensionModel);
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
   * @param configRefParentTnsName todo Lautaro add javadoc
   * @return a new component model that represents the old placeholder but expanded with the content of the <body/>
   */
  private ComponentModel createModuleOperationChain(ComponentModel operationRefModel, ExtensionModel extensionModel,
                                                    OperationModel operationModel, Set<String> moduleGlobalElementsNames,
                                                    Optional<String> testConnectionGlobalElementName,
                                                    Optional<String> configRefParentTnsName) {
    final OperationComponentModelModelProperty operationComponentModelModelProperty =
        operationModel.getModelProperty(OperationComponentModelModelProperty.class).get();
    final ComponentModel operationModuleComponentModel = operationComponentModelModelProperty
        .getBodyComponentModel();
    List<ComponentModel> bodyProcessors = operationModuleComponentModel.getInnerComponents();
    Optional<String> configRefName =
        referencesOperationsWithinModule(operationRefModel) ? configRefParentTnsName
            : Optional.ofNullable(operationRefModel.getParameters().get(MODULE_OPERATION_CONFIG_REF));
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
      if (referencesOperationsWithinModule(bodyProcessor)) {
        final Optional<OperationModel> tnsOperationOptional = lookForOperation(extensionModel, bodyProcessor);
        tnsOperationOptional.ifPresent(tnsOperation -> processorChainBuilder.addChildComponentModel(
                                                                                                    createModuleOperationChain(bodyProcessor,
                                                                                                                               extensionModel,
                                                                                                                               tnsOperation,
                                                                                                                               moduleGlobalElementsNames,
                                                                                                                               testConnectionGlobalElementName,
                                                                                                                               configRefName)));
      } else {
        ComponentModel childMPcomponentModel =
            copyOperationComponentModel(bodyProcessor, configRefName, moduleGlobalElementsNames, literalsParameters,
                                        extensionModel, testConnectionGlobalElementName);
        processorChainBuilder.addChildComponentModel(childMPcomponentModel);
      }
    }
    copyErrorMappings(operationRefModel, processorChainBuilder);

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
   * If the current operation contains any {@link ApplicationModel#ERROR_MAPPING} as a child, it will copy them to the macro
   * expanded <module-operation-chain/> as childs after the list of message processors.
   *
   * @param operationRefModel {@link ComponentModel} to look for the possible child elements {@link ApplicationModel#ERROR_MAPPING_IDENTIFIER}
   * @param processorChainBuilder the <module-operation-chain/> where the errors mappings will be copied to
   */
  private void copyErrorMappings(ComponentModel operationRefModel, ComponentModel.Builder processorChainBuilder) {
    operationRefModel.getInnerComponents().stream()
        .filter(componentModel -> componentModel.getIdentifier().equals(ApplicationModel.ERROR_MAPPING_IDENTIFIER))
        .forEach(errorMappingComponentModel -> processorChainBuilder
            .addChildComponentModel(copyComponentModel(errorMappingComponentModel)));
  }

  /**
   * Goes over the {@code modelToCopy} by consuming the attributes as they are.
   *
   * @param modelToCopy original source of truth that comes from the <module/>
   * @return a transformed {@link ComponentModel} from the {@code modelToCopy}, where the element's attributes has been
   *         updated accordingly (both global components updates plus the line number, and so on). If the value for some parameter
   */
  private ComponentModel copyComponentModel(ComponentModel modelToCopy) {
    ComponentModel.Builder operationReplacementModel = new ComponentModel.Builder();
    operationReplacementModel
        .setIdentifier(modelToCopy.getIdentifier())
        .setTextContent(modelToCopy.getTextContent());
    for (Map.Entry<String, Object> entry : modelToCopy.getCustomAttributes().entrySet()) {
      operationReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : modelToCopy.getParameters().entrySet()) {
      operationReplacementModel.addParameter(entry.getKey(), entry.getValue(), false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      operationReplacementModel.addChildComponentModel(
                                                       copyComponentModel(operationChildModel));
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

  /**
   * @param propertiesMap <property>s that are feed in the current usage of the <module/>
   * @param parametersMap <param>s that are feed in the current usage of the <module/>
   * @return a {@link Map} of <property>s and <parameter>s that could be replaced by their literal values, see
   *         {@link #copyGlobalElementComponentModel(ComponentModel, Optional, Set, Map, ConfigurationModel)}
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
   *         {@link InternalEvent})
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
                                    getConfigurationModel(extensionModel).get()
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
   * @param configurationModel element used to check whether or not the macro expanded element must have the same name as the one
   *                           defined in the application by calling {@link #getTestConnectionGlobalElement(ConfigurationModel)}
   * @return a transformed {@link ComponentModel} from the {@code modelToCopy}, where the global element's attributes has been
   *         updated accordingly (both global components updates plus the line number, and so on). If the value for some parameter
   *         can be optimized by replacing it for the literal's value, it will be done as well using the
   *         {@code literalsParameters}
   */
  private ComponentModel copyGlobalElementComponentModel(ComponentModel modelToCopy, Optional<String> configRefName,
                                                         Set<String> moduleGlobalElementsNames,
                                                         Map<String, String> literalsParameters,
                                                         ConfigurationModel configurationModel) {
    ComponentModel.Builder globalElementReplacementModel = new ComponentModel.Builder();
    globalElementReplacementModel
        .setIdentifier(modelToCopy.getIdentifier())
        .setTextContent(modelToCopy.getTextContent());
    for (Map.Entry<String, Object> entry : modelToCopy.getCustomAttributes().entrySet()) {
      globalElementReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }

    Optional<String> testConnectionGlobalElementName = getTestConnectionGlobalElement(configurationModel);

    for (Map.Entry<String, String> entry : modelToCopy.getParameters().entrySet()) {
      String value =
          calculateAttributeValue(configRefName, moduleGlobalElementsNames, entry.getValue(), testConnectionGlobalElementName);
      final String optimizedValue = literalsParameters.getOrDefault(value, value);
      globalElementReplacementModel.addParameter(entry.getKey(), optimizedValue, false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      globalElementReplacementModel.addChildComponentModel(
                                                           copyGlobalElementComponentModel(operationChildModel, configRefName,
                                                                                           moduleGlobalElementsNames,
                                                                                           literalsParameters,
                                                                                           configurationModel));
    }

    final String configFileName = modelToCopy.getConfigFileName()
        .orElseThrow(() -> new IllegalArgumentException("The is no config file name for the component to macro expand"));
    final Integer lineNumber = modelToCopy.getLineNumber()
        .orElseThrow(() -> new IllegalArgumentException("The is no line number for the component to macro expand"));
    globalElementReplacementModel.setConfigFileName(configFileName);
    globalElementReplacementModel.setLineNumber(lineNumber);

    ComponentModel componentModel = globalElementReplacementModel.build();
    for (ComponentModel child : componentModel.getInnerComponents()) {
      child.setParent(componentModel);
    }
    return componentModel;
  }

  /**
   * Goes over the {@code modelToCopy} by consuming the attributes as they are, unless some of them are actually targeting a
   * global component (such as a configuration), in which it will append the {@code configRefName} to that reference, which will
   * be the definitive name once the Mule application has been completely macro expanded in the final XML configuration.
   *
   * @param modelToCopy original <operation/> source of truth that comes from the <module/>
   * @param configRefName name of the configuration being used in the Mule application
   * @param moduleGlobalElementsNames names of the <module/>s global component that will be macro expanded in the Mule application
   * @param literalsParameters {@link Map} with all he <property>s and <parameter>s that were feed with a literal value in the
   *        Mule application's code.
   * @param extensionModel used to resolve any {@link #TNS_PREFIX} (by looking up <operation/>s in the same <module/>).
   * @param testConnectionGlobalElementName if present, the global element marked with
   *        {@link TestConnectionGlobalElementModelProperty} when macro expanded will hold the original value.
   * @return a transformed {@link ComponentModel} from the {@code modelToCopy}, where the global element's attributes has been
   *         updated accordingly (both global components updates plus the line number, and so on). If the value for some parameter
   *         can be optimized by replacing it for the literal's value, it will be done as well using the
   *         {@code literalsParameters}
   */
  private ComponentModel copyOperationComponentModel(ComponentModel modelToCopy, Optional<String> configRefName,
                                                     Set<String> moduleGlobalElementsNames,
                                                     Map<String, String> literalsParameters,
                                                     ExtensionModel extensionModel,
                                                     Optional<String> testConnectionGlobalElementName) {
    ComponentModel.Builder operationReplacementModel = new ComponentModel.Builder();
    operationReplacementModel
        .setIdentifier(modelToCopy.getIdentifier())
        .setTextContent(modelToCopy.getTextContent());
    for (Map.Entry<String, Object> entry : modelToCopy.getCustomAttributes().entrySet()) {
      operationReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : modelToCopy.getParameters().entrySet()) {
      validateConfigRefAttribute(modelToCopy, configRefName, entry.getValue());
      String value =
          calculateAttributeValue(configRefName, moduleGlobalElementsNames, entry.getValue(), testConnectionGlobalElementName);
      final String optimizedValue = literalsParameters.getOrDefault(value, value);
      operationReplacementModel.addParameter(entry.getKey(), optimizedValue, false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      if (referencesOperationsWithinModule(operationChildModel)) {
        // Referencing operation of the same module, needs to macro expand again.
        final Optional<OperationModel> tnsOperationOptional = lookForOperation(extensionModel, operationChildModel);
        tnsOperationOptional.ifPresent(tnsOperation -> {
          final ComponentModel moduleOperationChain = createModuleOperationChain(operationChildModel,
                                                                                 extensionModel,
                                                                                 tnsOperation,
                                                                                 moduleGlobalElementsNames,
                                                                                 testConnectionGlobalElementName,
                                                                                 configRefName);
          operationReplacementModel.addChildComponentModel(moduleOperationChain);
        });
      } else {
        operationReplacementModel
            .addChildComponentModel(copyOperationComponentModel(operationChildModel, configRefName, moduleGlobalElementsNames,
                                                                literalsParameters, extensionModel,
                                                                testConnectionGlobalElementName));
      }
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

  /**
   * True if an <operation/> A calls an <operation/> B defined in the same <module/> by using <tns:B .../>
   *
   * @param operationComponentModel operation that might or might not be referencing operations of the same module.
   * @return true if it's an <operation/> reference in the same <module/>, false otherwise
   */
  private boolean referencesOperationsWithinModule(ComponentModel operationComponentModel) {
    return TNS_PREFIX.equals(operationComponentModel.getIdentifier().getNamespace());
  }

  /**
   * Looks for an operation within checking if it is defined within the scope of a {@link ConfigurationModel} or the
   * {@link ExtensionModel}.
   *
   * @param extensionModel model to look the operation for.
   * @param operationComponentModel operation to look for.
   * @return the operation if found, {@link Optional#empty()} otherwise.
   */
  private Optional<OperationModel> lookForOperation(ExtensionModel extensionModel, ComponentModel operationComponentModel) {
    Optional<OperationModel> result = Optional.empty();
    //As the operation can be inside the extension or the config, it has to be looked up in both elements.
    final HasOperationModels hasOperationModels =
        getConfigurationModel(extensionModel)
            .map(configurationModel -> (HasOperationModels) configurationModel)
            .orElse(extensionModel);

    final String identifierName = operationComponentModel.getIdentifier().getName();
    Optional<OperationModel> operationModel = hasOperationModels.getOperationModel(identifierName);
    if (operationModel.isPresent()) {
      result = Optional.of(operationModel.get());
    } else {
      // as the #executeOnEveryMuleComponentTree goes from bottom to top, before throwing an exception we need to check if
      // the current operationRefModel's parent is an operation of the current ExtensionModel, meaning that the role of the
      // parameter is either CONTENT or PRIMARY_CONTENT
      final ComponentIdentifier parentIdentifier = operationComponentModel.getParent().getIdentifier();
      final String parentIdentifierName = parentIdentifier.getName();
      if (!hasOperationModels.getOperationModel(parentIdentifierName).isPresent()
          && (!extensionModel.getConfigurationModel(parentIdentifierName).isPresent())) {
        throw new IllegalArgumentException(format("The operation/connection element '%s' is missing in the module '%s'",
                                                  identifierName,
                                                  extensionModel.getName()));
      }
    }
    return result;
  }

  // TODO MULE-12526: once implemented, remove this validation as it will be done through XSD (config-ref must be mandatory in
  // each operation for Smart Connectors)
  private void validateConfigRefAttribute(ComponentModel modelToCopy, Optional<String> configRefName, String originalValue) {
    if (configRefName.isPresent() && MODULE_OPERATION_CONFIG_REF.equals(configRefName) && StringUtils.isBlank(originalValue)) {
      throw new IllegalArgumentException(format("The operation '%s' is missing the '%s' attribute",
                                                modelToCopy.getIdentifier().getName(), MODULE_OPERATION_CONFIG_REF));
    }
  }

  // TODO MULE-9849: until there's no clear way to check against the ComponentModel using the
  // org.mule.runtime.config.spring.dsl.processor.AbstractAttributeDefinitionVisitor.onReferenceSimpleParameter(), we workaround
  // the issue by checking every <module/>'s global element's name.
  private String calculateAttributeValue(Optional<String> configRefNameToAppend, Set<String> moduleGlobalElementsNames,
                                         String originalValue,
                                         Optional<String> testConnectionGlobalElementName) {
    String result;
    if ((moduleGlobalElementsNames.contains(originalValue))) {
      // current value is a global element reference
      if (testConnectionGlobalElementName.isPresent() && testConnectionGlobalElementName.get().equals(originalValue)) {
        //and it's also a reference to a bean that will be doing test connection, which implies no renaming must be done when macro expanding.
        result = configRefNameToAppend.get();
      } else {
        result = originalValue.concat("-").concat(configRefNameToAppend.get());
      }
    } else {
      result = originalValue;
    }

    return result;


  }
}
