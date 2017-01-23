/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension.xml;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isVoid;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.NAME_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.REFERENCE_ATTRIBUTE;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.config.ComponentIdentifier;

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
 * for the "config" elements while the {@link OperationComponentModelModelProperty} for the operations
 * (aka: {@link Processor}s in the XML file).
 * <p/>
 * TODO(fernandezlautaro) MULE-10355: the following comment must be deleted once implemented.
 * Bear in mind that at the moment, the current implementation assumes there will not be dependencies among macro
 * expansions, the following example WONT work until MULE-10355 is done:
 * B depends on A, the application uses B, which means that the macro expanded app must expand B elements, discover
 * that are A ones, and expand A elements as well.
 *
 * @since 4.0
 */
public class MacroExpansionModuleModel {

  /**
   * literal that represents the name of the global element for any given module. If the module's name is math, then
   * the value of this field will name the global element as <math:config ../>
   */
  private static final String MODULE_CONFIG_GLOBAL_ELEMENT_NAME = "config";
  private static final String MODULE_OPERATION_CONFIG_REF = "config-ref";

  private final ApplicationModel applicationModel;
  private final Map<String, ExtensionModel> extensions;

  /**
   * From a mutable {@code applicationModel}, it will store it to apply changes when the {@link #expand()} method
   * is executed.
   *
   * @param applicationModel to modify given the usages of elements that belong to the {@link ExtensionModel}s contained
   *                         in the {@code extensions} map.
   * @param extensions set with all the loaded {@link ExtensionModel}s from the deployment that will be filtered by
   *                   looking up only those that are coming from an XML context through the {@link XmlExtensionModelProperty}
   *                   property.
   */
  public MacroExpansionModuleModel(ApplicationModel applicationModel, Set<ExtensionModel> extensions) {
    this.applicationModel = applicationModel;
    this.extensions = extensions.stream()
        .filter(extensionModel -> extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent())
        .collect(toMap(extensionModel -> extensionModel.getName(), identity()));
  }

  /**
   * Goes through the entire xml mule application looking for the message processors that can be expanded, and then takes
   * care of the global elements.
   * <p/>
   * TODO(fernandezlautaro) MULE-10355: current implementation does not plays at all with <module/> within <module/>
   */
  public void expand() {
    if (!extensions.isEmpty()) {
      createOperationRefEffectiveModel(extensions);
      createConfigRefEffectiveModel(extensions);
    }
  }


  private void createOperationRefEffectiveModel(Map<String, ExtensionModel> extensionManager) {
    HashMap<Integer, ComponentModel> componentModelsToReplaceByIndex = new HashMap<>();

    applicationModel.executeOnEveryMuleComponentTree(componentModel -> {
      for (int i = 0; i < componentModel.getInnerComponents().size(); i++) {
        ComponentModel operationRefModel = componentModel.getInnerComponents().get(i);
        ComponentIdentifier identifier = operationRefModel.getIdentifier();
        String identifierName = identifier.getName();
        if (identifierName.equals(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)) {
          continue;
        }
        ExtensionModel extensionModel = extensionManager.get(identifier.getNamespace());
        if (extensionModel != null) {

          HasOperationModels hasOperationModels = extensionModel;
          final Optional<ConfigurationModel> configurationModel =
              extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);
          if (configurationModel.isPresent()) {
            hasOperationModels = configurationModel.get();
          }
          //config elements will be worked later on, that's why we are skipping this element
          OperationModel operationModel = hasOperationModels.getOperationModel(identifierName)
              .orElseThrow(() -> new IllegalArgumentException(format("The operation '%s' is missing in the module '%s'",
                                                                     identifierName, extensionModel.getName())));
          ComponentModel replacementModel = createOperationInstance(operationRefModel, extensionModel, operationModel);
          componentModelsToReplaceByIndex.put(i, replacementModel);
        }
      }
      for (Map.Entry<Integer, ComponentModel> entry : componentModelsToReplaceByIndex.entrySet()) {
        entry.getValue().setParent(componentModel);
        componentModel.getInnerComponents().add(entry.getKey(), entry.getValue());
        componentModel.getInnerComponents().remove(entry.getKey() + 1);
      }
      componentModelsToReplaceByIndex.clear();
    });
  }

  private void createConfigRefEffectiveModel(Map<String, ExtensionModel> extensionManager) {
    applicationModel.executeOnEveryMuleComponentTree(componentModel -> {
      HashMap<Integer, List<ComponentModel>> componentModelsToReplaceByIndex = new HashMap<>();

      for (int i = 0; i < componentModel.getInnerComponents().size(); i++) {
        ComponentModel configRefModel = componentModel.getInnerComponents().get(i);
        ComponentIdentifier identifier = configRefModel.getIdentifier();
        ExtensionModel extensionModel = extensionManager.get(identifier.getNamespace());
        if (extensionModel != null) {
          List<ComponentModel> replacementGlobalElements =
              createGlobalElementsInstance(configRefModel, extensionModel);
          componentModelsToReplaceByIndex.put(i, replacementGlobalElements);
        }
      }
      for (Map.Entry<Integer, List<ComponentModel>> entry : componentModelsToReplaceByIndex.entrySet()) {
        componentModel.getInnerComponents().addAll(entry.getKey(), entry.getValue());
        componentModel.getInnerComponents().remove(entry.getKey() + entry.getValue().size());
      }
    });
  }

  private List<ComponentModel> createGlobalElementsInstance(ComponentModel configRefModel, ExtensionModel extensionModel) {
    List<ComponentModel> globalElementsModel = new ArrayList<>();

    Optional<ConfigurationModel> config = extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);
    if (config.isPresent()
        && config.get().getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent()) {
      GlobalElementComponentModelModelProperty globalElementComponentModelModelProperty =
          config.get().getModelProperty(GlobalElementComponentModelModelProperty.class).get();

      globalElementsModel.addAll(globalElementComponentModelModelProperty.getGlobalElements().stream()
          .map(globalElementModel -> copyComponentModel(globalElementModel, configRefModel.getNameAttribute()))
          .collect(Collectors.toList()));

      ComponentModel muleRootElement = configRefModel.getParent();
      globalElementsModel.stream().forEach(componentModel -> {
        componentModel.setRoot(true);
        componentModel.setParameter(NAME_ATTRIBUTE, componentModel.getNameAttribute() + "-" + configRefModel.getNameAttribute());
        componentModel.setParent(muleRootElement);
      });
    }
    return globalElementsModel;
  }

  /**
   * Takes a one liner call to any given message processor, expand it to creating a "module-operation-chain" scope which
   * has the set of properties, the set of parameters and the list of message processors to execute.
   *
   * @param operationRefModel message processor that will be replaced by a scope element named "module-operation-chain".
   * @param extensionModel extension that holds a possible set of <property/>s that has to be parametrized to the new scope.
   * @param operationModel operation that provides both the <parameter/>s and content of the <body/>
   * @return a new component model that represents the old placeholder but expanded with the content of the <body/>
   */
  private ComponentModel createOperationInstance(ComponentModel operationRefModel, ExtensionModel extensionModel,
                                                 OperationModel operationModel) {
    List<ComponentModel> bodyProcessors = operationModel.getModelProperty(OperationComponentModelModelProperty.class).get()
        .getComponentModel().getInnerComponents();

    String configRefName = operationRefModel.getParameters().get(MODULE_OPERATION_CONFIG_REF);

    ComponentModel.Builder processorChainBuilder = new ComponentModel.Builder();
    processorChainBuilder
        .setIdentifier(new ComponentIdentifier.Builder().withNamespace("mule").withName("module-operation-chain").build());


    processorChainBuilder.addParameter("returnsVoid", String.valueOf(isVoid(operationModel.getOutput().getType())), false);
    Map<String, String> propertiesMap = extractProperties(operationRefModel, extensionModel);
    Map<String, String> parametersMap = extractParameters(operationRefModel, operationModel.getAllParameterModels());
    ComponentModel propertiesComponentModel =
        getParameterChild(propertiesMap, "module-operation-properties", "module-operation-property-entry");
    ComponentModel parametersComponentModel =
        getParameterChild(parametersMap, "module-operation-parameters", "module-operation-parameter-entry");
    processorChainBuilder.addChildComponentModel(propertiesComponentModel);
    processorChainBuilder.addChildComponentModel(parametersComponentModel);

    for (ComponentModel bodyProcessor : bodyProcessors) {
      processorChainBuilder.addChildComponentModel(copyComponentModel(bodyProcessor, configRefName));
    }
    for (Map.Entry<String, Object> customAttributeEntry : operationRefModel.getCustomAttributes().entrySet()) {
      processorChainBuilder.addCustomAttribute(customAttributeEntry.getKey(), customAttributeEntry.getValue());
    }
    ComponentModel processorChainModel = processorChainBuilder.build();
    for (ComponentModel processoChainModelChild : processorChainModel.getInnerComponents()) {
      processoChainModelChild.setParent(processorChainModel);
    }
    return processorChainModel;
  }

  private ComponentModel getParameterChild(Map<String, String> parameters, String wrapperParameters, String entryParameter) {
    ComponentModel.Builder parametersBuilder = new ComponentModel.Builder();
    parametersBuilder
        .setIdentifier(new ComponentIdentifier.Builder().withNamespace("mule").withName(wrapperParameters).build());
    parameters.forEach((paramName, paramValue) -> {
      ComponentModel.Builder parameterBuilder = new ComponentModel.Builder();
      parameterBuilder.setIdentifier(new ComponentIdentifier.Builder().withNamespace("mule")
          .withName(entryParameter).build());

      parameterBuilder.addParameter("key", paramName, false);
      parameterBuilder.addParameter("value", paramValue, false);
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
    //extract the <properties>
    String configParameter = operationRefModel.getParameters().get(MODULE_OPERATION_CONFIG_REF);
    if (configParameter != null) {
      ComponentModel configRefComponentModel = applicationModel.getRootComponentModel().getInnerComponents().stream()
          .filter(componentModel -> componentModel.getIdentifier().getNamespace().equals(extensionModel.getName())
              && componentModel.getIdentifier().getName().equals(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)
              && configParameter.equals(componentModel.getParameters().get(NAME_ATTRIBUTE)))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(String
              .format("There's no <%s:config> named [%s] in the current mule app", extensionModel.getName(), configParameter)));
      valuesMap
          .putAll(extractParameters(configRefComponentModel,
                                    extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME).get()
                                        .getAllParameterModels()));
    }
    return valuesMap;
  }

  private Map<String, String> extractParameters(ComponentModel componentModel, List<ParameterModel> parameters) {
    Map<String, String> valuesMap = new HashMap<>();
    for (ParameterModel parameterExtension : parameters) {
      String paramName = parameterExtension.getName();
      String value = null;
      if (componentModel.getParameters().containsKey(paramName)) {
        value = componentModel.getParameters().get(paramName);
      } else if (parameterExtension.getDefaultValue() != null) {
        value = (String) parameterExtension.getDefaultValue();
      }

      if (value != null) {
        valuesMap.put(paramName, value);
      }
    }
    return valuesMap;
  }

  private ComponentModel copyComponentModel(ComponentModel modelToCopy,
                                            String configRefName) {
    ComponentModel.Builder operationReplacementModel = new ComponentModel.Builder();
    operationReplacementModel
        .setIdentifier(modelToCopy.getIdentifier())
        .setTextContent(modelToCopy.getTextContent());
    for (Map.Entry<String, Object> entry : modelToCopy.getCustomAttributes().entrySet()) {
      operationReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : modelToCopy.getParameters().entrySet()) {
      String value = entry.getKey().endsWith(REFERENCE_ATTRIBUTE)
          ? entry.getValue().concat("-").concat(configRefName)
          : entry.getValue();
      operationReplacementModel.addParameter(entry.getKey(), value, false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      operationReplacementModel.addChildComponentModel(copyComponentModel(operationChildModel, configRefName));
    }
    ComponentModel componentModel = operationReplacementModel.build();
    for (ComponentModel child : componentModel.getInnerComponents()) {
      child.setParent(componentModel);
    }
    return componentModel;
  }

}
