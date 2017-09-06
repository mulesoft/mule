/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.model.extension.xml;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.el.BindingContextUtils.PARAMETERS;
import static org.mule.runtime.api.el.BindingContextUtils.PROPERTIES;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.NAME_ATTRIBUTE;
import static org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
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
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.property.OperationComponentModelModelProperty;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.property.TestConnectionGlobalElementModelProperty;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.property.XmlExtensionModelProperty;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
  private final ExtensionModel extensionModel;

  /**
   * From a mutable {@code applicationModel}, it will store it to apply changes when the {@link #expand()} method is executed.
   *
   * @param applicationModel to modify given the usages of elements that belong to the {@link ExtensionModel}s contained in the
   *        {@code extensions} map.
   * @param extensionModel the {@link ExtensionModel}s to macro expand in the parametrized {@link ApplicationModel}
   */
  MacroExpansionModuleModel(ApplicationModel applicationModel, ExtensionModel extensionModel) {
    this.applicationModel = applicationModel;
    this.extensionModel = extensionModel;
  }

  public void expand() {
    final List<ComponentModel> moduleGlobalElements = getModuleGlobalElements();
    final Set<String> moduleGlobalElementsNames =
        moduleGlobalElements.stream().map(ComponentModel::getNameAttribute).collect(toSet());
    expandOperations(moduleGlobalElementsNames);
    expandGlobalElements(moduleGlobalElements, moduleGlobalElementsNames);
  }

  private void expandOperations(Set<String> moduleGlobalElementsNames) {
    applicationModel.executeOnEveryMuleComponentTree(flowComponentModel -> {
      HashMap<Integer, ComponentModel> componentModelsToReplaceByIndex = new HashMap<>();
      IntStream.range(0, flowComponentModel.getInnerComponents().size()).forEach(i -> {
        ComponentModel operationRefModel = flowComponentModel.getInnerComponents().get(i);
        lookForOperation(operationRefModel)
            .ifPresent(operationModel -> {
              final ComponentModel moduleOperationChain =
                  createModuleOperationChain(operationRefModel, operationModel, moduleGlobalElementsNames, Optional.empty());
              componentModelsToReplaceByIndex.put(i, moduleOperationChain);
            });
      });
      for (Map.Entry<Integer, ComponentModel> entry : componentModelsToReplaceByIndex.entrySet()) {
        entry.getValue().setParent(flowComponentModel);
        flowComponentModel.getInnerComponents().add(entry.getKey(), entry.getValue());
        flowComponentModel.getInnerComponents().remove(entry.getKey() + 1);
      }
      componentModelsToReplaceByIndex.clear();
    });
  }

  private void expandGlobalElements(List<ComponentModel> moduleComponentModels, Set<String> moduleGlobalElementsNames) {
    applicationModel.executeOnEveryMuleComponentTree(muleRootComponentModel -> {
      HashMap<ComponentModel, List<ComponentModel>> componentModelsToReplaceByIndex = new HashMap<>();
      for (ComponentModel configRefModel : muleRootComponentModel.getInnerComponents()) {
        looForConfiguration(configRefModel).ifPresent(configurationModel -> {
          Map<String, String> propertiesMap = extractParameters(configRefModel,
                                                                configurationModel
                                                                    .getAllParameterModels());
          final Map<String, String> literalsParameters = getLiteralParameters(propertiesMap, emptyMap());
          List<ComponentModel> replacementGlobalElements =
              createGlobalElementsInstance(configRefModel, moduleComponentModels, moduleGlobalElementsNames, literalsParameters);
          componentModelsToReplaceByIndex.put(configRefModel, replacementGlobalElements);
        });
      }
      for (Map.Entry<ComponentModel, List<ComponentModel>> entry : componentModelsToReplaceByIndex.entrySet()) {
        final int componentModelIndex = muleRootComponentModel.getInnerComponents().indexOf(entry.getKey());
        muleRootComponentModel.getInnerComponents().addAll(componentModelIndex, entry.getValue());
        muleRootComponentModel.getInnerComponents().remove(componentModelIndex + entry.getValue().size());
      }
    });
  }

  private Optional<ConfigurationModel> getConfigurationModel() {
    return extensionModel
        .getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);
  }

  private List<ComponentModel> createGlobalElementsInstance(ComponentModel configRefModel,
                                                            List<ComponentModel> moduleGlobalElements,
                                                            Set<String> moduleGlobalElementsNames,
                                                            Map<String, String> literalsParameters) {

    List<ComponentModel> globalElementsModel = new ArrayList<>();
    globalElementsModel.addAll(moduleGlobalElements.stream()
        .map(globalElementModel -> copyGlobalElementComponentModel(globalElementModel,
                                                                   configRefModel.getNameAttribute(),
                                                                   moduleGlobalElementsNames, literalsParameters))
        .collect(Collectors.toList()));

    ComponentModel muleRootElement = configRefModel.getParent();
    globalElementsModel.stream().forEach(componentModel -> {
      componentModel.setRoot(true);
      componentModel.setParent(muleRootElement);
    });

    return globalElementsModel;
  }

  private List<ComponentModel> getModuleGlobalElements() {
    List<ComponentModel> moduleGlobalElements = new ArrayList<>();
    Optional<ConfigurationModel> config = getConfigurationModel();
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
   * @param operationModel operation that provides both the <parameter/>s and content of the <body/>
   * @param moduleGlobalElementsNames collection with the global components names (such as <http:config name="a"../>, <file:config
   *        name="b"../>, <file:matcher name="c"../> and so on) that are contained within the <module/> that will be macro
   *        expanded
   * @param configRefParentTnsName parent reference to the global element if exists (it might not be global elements in the
   *                               current module). Useful when replacing {@link #TNS_PREFIX} operations, as the references to the
   *                               global elements will be those of the rootest element of the operations consumed by the app.
   * @return a new component model that represents the old placeholder but expanded with the content of the <body/>
   */
  private ComponentModel createModuleOperationChain(ComponentModel operationRefModel,
                                                    OperationModel operationModel, Set<String> moduleGlobalElementsNames,
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
    Map<String, String> propertiesMap = extractProperties(configRefName);
    Map<String, String> parametersMap = extractParameters(operationRefModel, operationModel.getAllParameterModels());
    ComponentModel propertiesComponentModel =
        getParameterChild(propertiesMap, "module-operation-properties", "module-operation-property-entry");
    ComponentModel parametersComponentModel =
        getParameterChild(parametersMap, "module-operation-parameters", "module-operation-parameter-entry");
    processorChainBuilder.addChildComponentModel(propertiesComponentModel);
    processorChainBuilder.addChildComponentModel(parametersComponentModel);

    for (ComponentModel bodyProcessor : bodyProcessors) {
      ComponentModel childMPcomponentModel =
          lookForTNSOperation(bodyProcessor)
              .map(tnsOperation -> createModuleOperationChain(bodyProcessor, tnsOperation, moduleGlobalElementsNames,
                                                              configRefName))
              .orElseGet(() -> copyOperationComponentModel(bodyProcessor, configRefName, moduleGlobalElementsNames,
                                                           getLiteralParameters(propertiesMap, parametersMap)));
      processorChainBuilder.addChildComponentModel(childMPcomponentModel);
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
   * @return a {@link Map} of <property>s and <parameter>s that could be replaced by their literal values
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

  /**
   * Extracts the properties of the current <module/> if applies (it might not have a configuration in it)
   *
   * @param configRefName current <operation/> to macro expand, from which the config-ref attribute's value will be extracted.
   * @return a map with the name and values of the <module/>'s properties.
   */
  private Map<String, String> extractProperties(Optional<String> configRefName) {
    Map<String, String> valuesMap = new HashMap<>();
    //    String configParameter = operationRefModel.getParameters().get(MODULE_OPERATION_CONFIG_REF);
    configRefName.ifPresent(configParameter -> {
      // look for the global element which "name" attribute maps to "configParameter" value
      ComponentModel configRefComponentModel = applicationModel.getRootComponentModel().getInnerComponents().stream()
          .filter(componentModel -> looForConfiguration(componentModel).isPresent()
              && configParameter.equals(componentModel.getParameters().get(NAME_ATTRIBUTE)))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(
                                                          format("There's no <%s:config> named [%s] in the current mule app",
                                                                 extensionModel.getXmlDslModel().getPrefix(), configParameter)));
      // as configParameter != null, a ConfigurationModel must exists
      final ConfigurationModel configurationModel = getConfigurationModel().get();
      valuesMap.putAll(extractParameters(configRefComponentModel, configurationModel.getAllParameterModels()));
    });
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
  private ComponentModel copyGlobalElementComponentModel(ComponentModel modelToCopy, String configRefName,
                                                         Set<String> moduleGlobalElementsNames,
                                                         Map<String, String> literalsParameters) {
    ComponentModel.Builder globalElementReplacementModel = new ComponentModel.Builder();
    globalElementReplacementModel
        .setIdentifier(modelToCopy.getIdentifier())
        .setTextContent(modelToCopy.getTextContent());
    for (Map.Entry<String, Object> entry : modelToCopy.getCustomAttributes().entrySet()) {
      globalElementReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, String> entry : modelToCopy.getParameters().entrySet()) {
      String value =
          calculateAttributeValue(configRefName, moduleGlobalElementsNames, entry.getValue());
      final String optimizedValue = literalsParameters.getOrDefault(value, value);
      globalElementReplacementModel.addParameter(entry.getKey(), optimizedValue, false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      globalElementReplacementModel.addChildComponentModel(
                                                           copyGlobalElementComponentModel(operationChildModel, configRefName,
                                                                                           moduleGlobalElementsNames,
                                                                                           literalsParameters));
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
   * @param configRefName name of the configuration being used by the current <operation/>. If the operation is a TNS one, then
   *                      it has the value of the rootest <operation/> being called from the application.
   * @param moduleGlobalElementsNames names of the <module/>s global component that will be macro expanded in the Mule application
   * @param literalsParameters {@link Map} with all he <property>s and <parameter>s that were feed with a literal value in the
   *        Mule application's code.
   * @return a transformed {@link ComponentModel} from the {@code modelToCopy}, where the global element's attributes has been
   *         updated accordingly (both global components updates plus the line number, and so on). If the value for some parameter
   *         can be optimized by replacing it for the literal's value, it will be done as well using the
   *         {@code literalsParameters}
   */
  private ComponentModel copyOperationComponentModel(ComponentModel modelToCopy, Optional<String> configRefName,
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
      String value = configRefName
          .map(s -> calculateAttributeValue(s, moduleGlobalElementsNames, entry.getValue()))
          .orElseGet(entry::getValue);
      final String optimizedValue = literalsParameters.getOrDefault(value, value);
      operationReplacementModel.addParameter(entry.getKey(), optimizedValue, false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      ComponentModel childMPcomponentModel =
          lookForTNSOperation(operationChildModel)
              .map(tnsOperation -> createModuleOperationChain(operationChildModel, tnsOperation, moduleGlobalElementsNames,
                                                              configRefName))
              .orElseGet(() -> copyOperationComponentModel(operationChildModel, configRefName, moduleGlobalElementsNames,
                                                           literalsParameters));
      operationReplacementModel.addChildComponentModel(childMPcomponentModel);
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

  private Optional<ConfigurationModel> looForConfiguration(ComponentModel componentModel) {
    final ComponentIdentifier identifier = componentModel.getIdentifier();
    return identifier.getNamespace().equals(extensionModel.getXmlDslModel().getPrefix())
        ? extensionModel.getConfigurationModel(identifier.getName()) : Optional.empty();
  }

  /**
   * Looks for an operation exposed in the current {@link ExtensionModel}.
   *
   * @param componentModel operation to look for.
   * @return the operation if found, {@link Optional#empty()} otherwise.
   */
  private Optional<OperationModel> lookForOperation(ComponentModel componentModel) {
    return lookForOperation(componentModel.getIdentifier(), extensionModel.getXmlDslModel().getPrefix());
  }

  /**
   * Looks for an operation exposed in the current {@link ExtensionModel} that's being targeted by other operation through the
   * {@link #TNS_PREFIX} prefix.
   *
   * @param componentModel to check whether targets a <module/>'s operation or not.
   * @return an {@link OperationModel} if the parametrized {@code componentModel} targets an <operation/> of the same module by
   * using the {@link #TNS_PREFIX} prefix.
   */
  private Optional<OperationModel> lookForTNSOperation(ComponentModel componentModel) {
    return lookForOperation(componentModel.getIdentifier(), TNS_PREFIX);
  }

  /**
   * Looks for an operation checking if it is defined within the scope of a {@link ConfigurationModel} or the
   * {@link ExtensionModel}.
   *
   * @param operationIdentifier element to look for in the current {@link #extensionModel}
   * @param prefix to check if the {@code operationIdentifier} namespace targets an operation of the <module/> (usually maps to
   *               the {@link ExtensionModel} prefix, or the {@link #TNS_PREFIX}.
   * @return an {@link OperationModel} if found, {@link Optional#empty()} otherwise.
   */
  private Optional<OperationModel> lookForOperation(ComponentIdentifier operationIdentifier, String prefix) {
    Optional<OperationModel> result = Optional.empty();
    if (operationIdentifier.getNamespace().equals(prefix)) {
      //As the operation can be inside the extension or the config, it has to be looked up in both elements.
      final HasOperationModels hasOperationModels =
          getConfigurationModel()
              .map(configurationModel -> (HasOperationModels) configurationModel)
              .orElse(extensionModel);
      result = hasOperationModels.getOperationModel(operationIdentifier.getName());
    }
    return result;
  }

  // TODO MULE-9849: until there's no clear way to check against the ComponentModel using the
  // org.mule.runtime.config.spring.dsl.processor.AbstractAttributeDefinitionVisitor.onReferenceSimpleParameter(), we workaround
  // the issue by checking every <module/>'s global element's name.
  private String calculateAttributeValue(String configRefNameToAppend, Set<String> moduleGlobalElementsNames,
                                         String originalValue) {
    String result;
    if ((moduleGlobalElementsNames.contains(originalValue))) {
      // current value is a global element reference
      if (originalValue.equals(getTestConnectionGlobalElement().orElse(null))) {
        //and it's also a reference to a bean that will be doing test connection, which implies no renaming must be done when macro expanding.
        result = configRefNameToAppend;
      } else {
        result = originalValue.concat("-").concat(configRefNameToAppend);
      }
    } else {
      // not a global element, returning the original value.
      result = originalValue;
    }
    return result;
  }

  /**
   * @return if present, the global element marked with {@link TestConnectionGlobalElementModelProperty} when macro expanded
   * will hold the original value.
   */
  private Optional<String> getTestConnectionGlobalElement() {
    return getConfigurationModel()
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
}
