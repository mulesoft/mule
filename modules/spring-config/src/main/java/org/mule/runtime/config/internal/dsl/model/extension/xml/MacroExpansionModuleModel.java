/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.model.ApplicationModel.MODULE_OPERATION_CHAIN;
import static org.mule.runtime.config.internal.model.ApplicationModel.NAME_ATTRIBUTE;
import static org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.MODULE_CONNECTION_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.OperationComponentModelModelProperty;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.PrivateOperationsModelProperty;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.TestConnectionGlobalElementModelProperty;
import org.mule.runtime.config.internal.dsl.spring.CommonBeanDefinitionCreator;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;

import java.util.ArrayList;
import java.util.Collections;
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
   * {@link org.mule.runtime.config.internal.dsl.model.ComponentLocationVisitor} can properly set the paths for every element
   * (even the macro expanded)
   */
  public static final String ORIGINAL_IDENTIFIER = "ORIGINAL_IDENTIFIER";

  /**
   * Reserved prefix in a <module/> to define a reference an operation of the same module (no circular dependencies allowed)
   */
  public static final String TNS_PREFIX = "tns";

  /**
   * Used to leave breadcrumbs of which is the flow's name containing the macro expanded chain.
   *
   * @see CommonBeanDefinitionCreator#processMacroExpandedAnnotations(ComponentModel, java.util.Map)
   */
  public static final String ROOT_MACRO_EXPANDED_FLOW_CONTAINER_NAME = "ROOT_MACRO_EXPANDED_FLOW_CONTAINER_NAME";

  /**
   * Used when the <module/> contains global elements without <property/>ies to be expanded, thus the macro expansion will take
   * care of the default global elements macro expanding them ONCE, and replacing the {@link #MODULE_OPERATION_CONFIG_REF} in the
   * <operation/>'s <body/> accordingly.
   */
  private static final String DEFAULT_CONFIG_GLOBAL_ELEMENT_SUFFIX = "%s-default-config-global-element-suffix";

  private final ApplicationModel applicationModel;
  private final ExtensionModel extensionModel;
  private final DslSyntaxResolver dslSyntaxResolver;

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
    final DslResolvingContext dslResolvingContext = DslResolvingContext.getDefault(Collections.singleton(extensionModel));
    this.dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel, dslResolvingContext);
  }

  public void expand() {
    final List<ComponentModel> moduleGlobalElements = getModuleGlobalElements();
    final Set<String> moduleGlobalElementsNames =
        moduleGlobalElements.stream().map(ComponentModel::getNameAttribute).collect(toSet());
    expandOperations(moduleGlobalElementsNames);
    expandGlobalElements(moduleGlobalElements, moduleGlobalElementsNames);
  }

  private void expandOperations(Set<String> moduleGlobalElementsNames) {
    applicationModel.executeOnEveryMuleComponentTree(containerComponentModel -> {
      HashMap<Integer, ComponentModel> componentModelsToReplaceByIndex = new HashMap<>();
      IntStream.range(0, containerComponentModel.getInnerComponents().size()).forEach(i -> {
        ComponentModel operationRefModel = containerComponentModel.getInnerComponents().get(i);
        lookForOperation(operationRefModel)
            .ifPresent(operationModel -> {
              final String containerName = calculateContainerRootName(containerComponentModel, operationModel);
              final ComponentModel moduleOperationChain =
                  createModuleOperationChain(operationRefModel, operationModel, moduleGlobalElementsNames, empty(),
                                             containerName);
              componentModelsToReplaceByIndex.put(i, moduleOperationChain);
            });
      });
      for (Map.Entry<Integer, ComponentModel> entry : componentModelsToReplaceByIndex.entrySet()) {
        entry.getValue().setParent(containerComponentModel);
        containerComponentModel.getInnerComponents().add(entry.getKey(), entry.getValue());
        containerComponentModel.getInnerComponents().remove(entry.getKey() + 1);
      }
      componentModelsToReplaceByIndex.clear();
    });
  }

  /**
   * Returns the rootest flow/subflow/munit:test/etc.'s name for the module chain that will be macro expanded. By default, it will
   * assume it's a flow or even an already macro expanded element, but if not it will ask for the parent component model, making
   * any scope (such as foreach, async, etc.) look for the flow in which is contained.
   *
   * @param containerComponentModel container element to look for the root element that contains it
   * @param operationModel the operation just to log if something went bad
   * @return the name of the root element that contains the {@code containerComponentModel}. Not null.
   * @throws MuleRuntimeException if it cannot find the root element. It should never happen, as the macro expansion for
   *         operations ONLY happens when being consumed from within a flow/subflow/etc.
   */
  private String calculateContainerRootName(ComponentModel containerComponentModel, OperationModel operationModel) {
    String nameAttribute;
    if (containerComponentModel.isRoot()) {
      nameAttribute = containerComponentModel.getNameAttribute();
    } else if (MODULE_OPERATION_CHAIN.equals(containerComponentModel.getIdentifier())) {
      nameAttribute =
          (String) containerComponentModel.getMetadata().getParserAttributes().get(ROOT_MACRO_EXPANDED_FLOW_CONTAINER_NAME);
    } else if (containerComponentModel.getParent() != null) {
      nameAttribute = calculateContainerRootName(containerComponentModel.getParent(), operationModel);
    } else {
      throw new MuleRuntimeException(createStaticMessage(format("Should have not reach here. There was no root container element while doing the macro expansion for the module [%s], operation [%s]",
                                                                extensionModel.getName(), operationModel.getName())));
    }
    return nameAttribute;
  }

  private Optional<String> defaultGlobalElementName() {
    Optional<String> defaultElementName = empty();
    if (extensionModel.getConfigurationModels().isEmpty() &&
        extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent()) {
      defaultElementName = extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class)
          .map(globalElementComponentModelModelProperty -> format(
                                                                  DEFAULT_CONFIG_GLOBAL_ELEMENT_SUFFIX,
                                                                  extensionModel.getName()));
    }
    return defaultElementName;
  }

  private void expandGlobalElements(List<ComponentModel> moduleComponentModels, Set<String> moduleGlobalElementsNames) {
    if (defaultGlobalElementName().isPresent()) {
      addDefaultGlobalElements(moduleGlobalElementsNames);
    } else {
      macroExpandGlobalElements(moduleComponentModels, moduleGlobalElementsNames);
    }
  }

  private void addDefaultGlobalElements(Set<String> moduleGlobalElementsNames) {
    // scenario where it will macro expand the default elements of a <module/>
    String defaultGlobalElementSuffix = defaultGlobalElementName().get();
    ComponentModel rootComponentModel = applicationModel.getRootComponentModel();
    List<ComponentModel> globalElements =
        extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).get().getGlobalElements();

    globalElements.forEach(globalElementComponenModel -> {
      final ComponentModel macroExpandedImplicitGlobalElement =
          copyGlobalElementComponentModel(globalElementComponenModel, defaultGlobalElementSuffix, moduleGlobalElementsNames,
                                          new HashMap<>());
      macroExpandedImplicitGlobalElement.setRoot(true);
      macroExpandedImplicitGlobalElement.setParent(rootComponentModel);
      rootComponentModel.getInnerComponents().add(macroExpandedImplicitGlobalElement);
    });
  }

  private void macroExpandGlobalElements(List<ComponentModel> moduleComponentModels, Set<String> moduleGlobalElementsNames) {
    // scenario where it will macro expand as many times as needed all the references of the smart connector configurations
    applicationModel.executeOnEveryMuleComponentTree(muleRootComponentModel -> {
      HashMap<ComponentModel, List<ComponentModel>> componentModelsToReplaceByIndex = new HashMap<>();
      for (ComponentModel configRefModel : muleRootComponentModel.getInnerComponents()) {
        looForConfiguration(configRefModel).ifPresent(configurationModel -> {
          Map<String, String> propertiesMap = extractParameters(configRefModel,
                                                                configurationModel
                                                                    .getAllParameterModels());
          Map<String, String> connectionPropertiesMap = extractConnectionProperties(configRefModel, configurationModel);
          propertiesMap.putAll(connectionPropertiesMap);
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
    ComponentModel muleRootElement = configRefModel.getParent();
    return moduleGlobalElements.stream()
        .map(globalElementModel -> {
          final ComponentModel macroExpandedGlobalElement =
              copyGlobalElementComponentModel(globalElementModel, configRefModel.getNameAttribute(), moduleGlobalElementsNames,
                                              literalsParameters);
          macroExpandedGlobalElement.setRoot(true);
          macroExpandedGlobalElement.setParent(muleRootElement);
          return macroExpandedGlobalElement;
        }).collect(Collectors.toList());

  }

  private List<ComponentModel> getModuleGlobalElements() {
    return extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class)
        .map(GlobalElementComponentModelModelProperty::getGlobalElements)
        .orElse(new ArrayList<>());
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
   *        current module). Useful when replacing {@link #TNS_PREFIX} operations, as the references to the global elements will
   *        be those of the rootest element of the operations consumed by the app.
   * @param containerName name of the container that contains the operation to be macro expanded. Not null nor empty.
   * @return a new component model that represents the old placeholder but expanded with the content of the <body/>
   */
  private ComponentModel createModuleOperationChain(ComponentModel operationRefModel,
                                                    OperationModel operationModel, Set<String> moduleGlobalElementsNames,
                                                    Optional<String> configRefParentTnsName,
                                                    String containerName) {
    final OperationComponentModelModelProperty operationComponentModelModelProperty =
        operationModel.getModelProperty(OperationComponentModelModelProperty.class).get();
    final ComponentModel operationModuleComponentModel = operationComponentModelModelProperty
        .getBodyComponentModel();
    List<ComponentModel> bodyProcessors = operationModuleComponentModel.getInnerComponents();
    Optional<String> configRefName =
        referencesOperationsWithinModule(operationRefModel) ? configRefParentTnsName : getConfigRefName(operationRefModel);
    ComponentModel.Builder processorChainBuilder = new ComponentModel.Builder();
    processorChainBuilder
        .setIdentifier(builder()
            .namespace(CORE_PREFIX)
            .namespaceUri(CORE_NAMESPACE)
            .name("module-operation-chain").build());

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
    processorChainBuilder.setSourceCode(operationRefModel.getSourceCode());

    for (ComponentModel bodyProcessor : bodyProcessors) {
      ComponentModel childMPcomponentModel =
          lookForTNSOperation(bodyProcessor)
              .map(tnsOperation -> createModuleOperationChain(bodyProcessor, tnsOperation, moduleGlobalElementsNames,
                                                              configRefName, containerName))
              .orElseGet(() -> copyOperationComponentModel(bodyProcessor, configRefName, moduleGlobalElementsNames,
                                                           getLiteralParameters(propertiesMap, parametersMap),
                                                           containerName));
      processorChainBuilder.addChildComponentModel(childMPcomponentModel);
    }
    copyErrorMappings(operationRefModel, processorChainBuilder);

    for (Map.Entry<String, Object> customAttributeEntry : operationRefModel.getMetadata().getParserAttributes().entrySet()) {
      processorChainBuilder.addCustomAttribute(customAttributeEntry.getKey(), customAttributeEntry.getValue());
    }
    for (Map.Entry<String, String> customAttributeEntry : operationRefModel.getMetadata().getDocAttributes().entrySet()) {
      processorChainBuilder.addCustomAttribute(customAttributeEntry.getKey(), customAttributeEntry.getValue());
    }
    processorChainBuilder.addCustomAttribute(ROOT_MACRO_EXPANDED_FLOW_CONTAINER_NAME, containerName);

    operationRefModel.getConfigFileName().ifPresent(processorChainBuilder::setConfigFileName);
    operationRefModel.getLineNumber().ifPresent(processorChainBuilder::setLineNumber);
    operationRefModel.getStartColumn().ifPresent(processorChainBuilder::setStartColumn);
    processorChainBuilder.addCustomAttribute(ORIGINAL_IDENTIFIER, operationRefModel.getIdentifier());

    ComponentModel processorChainModel = processorChainBuilder.build();
    for (ComponentModel processorChainModelChild : processorChainModel.getInnerComponents()) {
      processorChainModelChild.setParent(processorChainModel);
    }

    return processorChainModel;
  }

  /**
   * Looks for the value of the {@link #MODULE_OPERATION_CONFIG_REF} in the current <operation/>, if not found then tries to
   * fallback to the default global element name. See {@link #defaultGlobalElementName()} method.
   *
   * @param operationRefModel <operaton/> to lookup the expected string reference, if exists.
   * @return the suffix needed to be used when macro expanding elements, or {@link Optional#empty()} otherwise.
   */
  private Optional<String> getConfigRefName(ComponentModel operationRefModel) {
    return operationRefModel.getParameters().containsKey(MODULE_OPERATION_CONFIG_REF)
        ? of(operationRefModel.getParameters().get(MODULE_OPERATION_CONFIG_REF))
        : defaultGlobalElementName();
  }

  /**
   * If the current operation contains any {@link ApplicationModel#ERROR_MAPPING} as a child, it will copy them to the macro
   * expanded <module-operation-chain/> as childs after the list of message processors.
   *
   * @param operationRefModel {@link ComponentModel} to look for the possible child elements
   *        {@link ApplicationModel#ERROR_MAPPING_IDENTIFIER}
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
   * @return a transformed {@link ComponentModel} from the {@code modelToCopy}, where the element's attributes has been updated
   *         accordingly (both global components updates plus the line number, and so on). If the value for some parameter
   */
  private ComponentModel copyComponentModel(ComponentModel modelToCopy) {
    ComponentModel.Builder operationReplacementModel = getComponentModelBuilderFrom(modelToCopy);
    for (Map.Entry<String, String> entry : modelToCopy.getParameters().entrySet()) {
      operationReplacementModel.addParameter(entry.getKey(), entry.getValue(), false);
    }
    for (ComponentModel operationChildModel : modelToCopy.getInnerComponents()) {
      operationReplacementModel.addChildComponentModel(
                                                       copyComponentModel(operationChildModel));
    }
    return buildFrom(modelToCopy, operationReplacementModel);
  }

  /**
   * @param propertiesMap <property>s that are feed in the current usage of the <module/>
   * @param parametersMap <param>s that are feed in the current usage of the <module/>
   * @return a {@link Map} of <property>s and <parameter>s that could be replaced by their literal values
   */
  private Map<String, String> getLiteralParameters(Map<String, String> propertiesMap, Map<String, String> parametersMap) {
    final Map<String, String> literalsParameters = propertiesMap.entrySet().stream()
        .filter(entry -> !isExpression(entry.getValue()))
        .collect(Collectors.toMap(e -> getReplaceableExpression(e.getKey(), VARS),
                                  Map.Entry::getValue));

    literalsParameters.putAll(
                              parametersMap.entrySet().stream()
                                  .filter(entry -> !isExpression(entry.getValue()))
                                  .collect(Collectors.toMap(
                                                            e -> getReplaceableExpression(e.getKey(), VARS),
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
   *         {@link CoreEvent})
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
        .setIdentifier(builder()
            .namespace(CORE_PREFIX)
            .namespaceUri(CORE_NAMESPACE)
            .name(wrapperParameters).build());
    parameters.forEach((paramName, paramValue) -> {
      ComponentModel.Builder parameterBuilder = new ComponentModel.Builder();
      parameterBuilder.setIdentifier(builder()
          .namespace(CORE_PREFIX)
          .namespaceUri(CORE_NAMESPACE)
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
    configRefName
        .filter(configParameter -> defaultGlobalElementName()
            .map(defaultGlobalElementName -> !defaultGlobalElementName.equals(configParameter)).orElse(true))
        .ifPresent(configParameter -> {
          // look for the global element which "name" attribute maps to "configParameter" value
          ComponentModel configRefComponentModel = applicationModel.getRootComponentModel().getInnerComponents().stream()
              .filter(componentModel -> looForConfiguration(componentModel).isPresent()
                  && configParameter.equals(componentModel.getParameters().get(NAME_ATTRIBUTE)))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException(
                                                              format("There's no <%s:config> named [%s] in the current mule app",
                                                                     extensionModel.getXmlDslModel().getPrefix(),
                                                                     configParameter)));
          // as configParameter != null, a ConfigurationModel must exists
          final ConfigurationModel configurationModel = getConfigurationModel().get();
          valuesMap.putAll(extractParameters(configRefComponentModel, configurationModel.getAllParameterModels()));
          valuesMap.putAll(extractConnectionProperties(configRefComponentModel, configurationModel));
        });
    return valuesMap;
  }

  /**
   * If the current {@link ExtensionModel} does have a {@link ConnectionProviderModel}, then it will check if the current XML does
   * contain a child of it under the connection name (see
   * {@link ModuleOperationMessageProcessorChainBuilder#MODULE_CONNECTION_GLOBAL_ELEMENT_NAME}.
   *
   * @param configRefComponentModel root element of the current XML config (global element of the parametrized operation)
   * @param configurationModel configuration model of the current element
   * @return a map of properties to be added in the macro expanded <operation/>
   */
  private Map<String, String> extractConnectionProperties(ComponentModel configRefComponentModel,
                                                          ConfigurationModel configurationModel) {
    Map<String, String> connectionValuesMap = new HashMap<>();
    configurationModel.getConnectionProviderModel(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME)
        .ifPresent(
                   connectionProviderModel -> configRefComponentModel.getInnerComponents().stream()
                       .filter(componentModel -> MODULE_CONNECTION_GLOBAL_ELEMENT_NAME
                           .equals(componentModel.getIdentifier().getName()))
                       .findFirst()
                       .ifPresent(connectionComponentModel -> connectionValuesMap
                           .putAll(extractParameters(connectionComponentModel,
                                                     connectionProviderModel
                                                         .getAllParameterModels()))));

    return connectionValuesMap;
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
          final DslResolvingContext dslResolvingContext = DslResolvingContext.getDefault(emptySet());
          final DslSyntaxResolver dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel, dslResolvingContext);
          final String resolvedName = dslSyntaxResolver.resolve(parameterExtension).getElementName();
          final Optional<ComponentModel> childComponentModel = componentModel.getInnerComponents().stream()
              .filter(cm -> resolvedName.equals(cm.getIdentifier().getName()))
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
    ComponentModel.Builder globalElementReplacementModel = getComponentModelBuilderFrom(modelToCopy);

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
    return buildFrom(modelToCopy, globalElementReplacementModel);
  }

  /**
   * Goes over the {@code modelToCopy} by consuming the attributes as they are, unless some of them are actually targeting a
   * global component (such as a configuration), in which it will append the {@code configRefName} to that reference, which will
   * be the definitive name once the Mule application has been completely macro expanded in the final XML configuration.
   *
   * @param modelToCopy original <operation/> source of truth that comes from the <module/>
   * @param configRefName name of the configuration being used by the current <operation/>. If the operation is a TNS one, then it
   *        has the value of the rootest <operation/> being called from the application.
   * @param moduleGlobalElementsNames names of the <module/>s global component that will be macro expanded in the Mule application
   * @param literalsParameters {@link Map} with all he <property>s and <parameter>s that were feed with a literal value in the
   *        Mule application's code.
   * @param containerName name of the container that contains the operation to be macro expanded. Not null nor empty.
   * @return a transformed {@link ComponentModel} from the {@code modelToCopy}, where the global element's attributes has been
   *         updated accordingly (both global components updates plus the line number, and so on). If the value for some parameter
   *         can be optimized by replacing it for the literal's value, it will be done as well using the
   *         {@code literalsParameters}
   */
  private ComponentModel copyOperationComponentModel(ComponentModel modelToCopy, Optional<String> configRefName,
                                                     Set<String> moduleGlobalElementsNames,
                                                     Map<String, String> literalsParameters, String containerName) {
    ComponentModel.Builder operationReplacementModel = getComponentModelBuilderFrom(modelToCopy);
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
                                                              configRefName, containerName))
              .orElseGet(() -> copyOperationComponentModel(operationChildModel, configRefName, moduleGlobalElementsNames,
                                                           literalsParameters, containerName));
      operationReplacementModel.addChildComponentModel(childMPcomponentModel);
    }
    return buildFrom(modelToCopy, operationReplacementModel);
  }

  private ComponentModel.Builder getComponentModelBuilderFrom(ComponentModel componentModelOrigin) {
    ComponentModel.Builder operationReplacementModel = new ComponentModel.Builder();
    operationReplacementModel
        .setIdentifier(componentModelOrigin.getIdentifier())
        .setTextContent(componentModelOrigin.getTextContent());
    for (Map.Entry<String, Object> entry : componentModelOrigin.getMetadata().getParserAttributes().entrySet()) {
      operationReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : componentModelOrigin.getMetadata().getDocAttributes().entrySet()) {
      operationReplacementModel.addCustomAttribute(entry.getKey(), entry.getValue());
    }
    return operationReplacementModel;
  }

  private ComponentModel buildFrom(ComponentModel componentModelOrigin, ComponentModel.Builder operationReplacementModel) {
    componentModelOrigin.getConfigFileName().ifPresent(operationReplacementModel::setConfigFileName);
    componentModelOrigin.getLineNumber().ifPresent(operationReplacementModel::setLineNumber);
    componentModelOrigin.getStartColumn().ifPresent(operationReplacementModel::setStartColumn);
    operationReplacementModel.setSourceCode(componentModelOrigin.getSourceCode());
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
        ? extensionModel.getConfigurationModel(identifier.getName())
        : empty();
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
   *         using the {@link #TNS_PREFIX} prefix.
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
   *        the {@link ExtensionModel} prefix, or the {@link #TNS_PREFIX}.
   * @return an {@link OperationModel} if found, {@link Optional#empty()} otherwise.
   */
  private Optional<OperationModel> lookForOperation(ComponentIdentifier operationIdentifier, String prefix) {
    Optional<OperationModel> result = empty();
    final String opName = operationIdentifier.getName();
    if (operationIdentifier.getNamespace().equals(prefix)) {
      final HyphenatedOperationFinder extensionWalker = new HyphenatedOperationFinder(opName);
      extensionWalker.walk(extensionModel);
      result = extensionWalker.result;
    }
    // If the operation is not present, it might be a private one and it must be looked inside of the model property
    if (!result.isPresent() && extensionModel.getModelProperty(PrivateOperationsModelProperty.class).isPresent()) {
      result = extensionModel.getModelProperty(PrivateOperationsModelProperty.class).get().getOperationModel(opName);
    }
    return result;
  }

  /**
   * Looks an operation within the ExtensionModel by matching it with the hyphenated one that the SDK generates.
   */
  private class HyphenatedOperationFinder extends IdempotentExtensionWalker {

    private final String hyphenatedOpName;
    private Optional<OperationModel> result = Optional.empty();

    private HyphenatedOperationFinder(final String hyphenatedOpName) {
      this.hyphenatedOpName = hyphenatedOpName;
    }

    @Override
    protected void onOperation(OperationModel model) {
      DslElementSyntax dsl = dslSyntaxResolver.resolve(model);
      if (dsl.getElementName().equals(hyphenatedOpName)) {
        result = Optional.of(model);
        stop();
      }
    }
  }

  // TODO MULE-9849: until there's no clear way to check against the ComponentModel using the
  // org.mule.runtime.config.dsl.processor.AbstractAttributeDefinitionVisitor.onReferenceSimpleParameter(), we workaround
  // the issue by checking every <module/>'s global element's name.
  private String calculateAttributeValue(String configRefNameToAppend, Set<String> moduleGlobalElementsNames,
                                         String originalValue) {
    String result;
    if ((moduleGlobalElementsNames.contains(originalValue))) {
      // current value is a global element reference
      if (originalValue.equals(getTestConnectionGlobalElement().orElse(null))) {
        // and it's also a reference to a bean that will be doing test connection, which implies no renaming must be done when
        // macro expanding.
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
   * @return if present, the global element marked with {@link TestConnectionGlobalElementModelProperty} when macro expanded will
   *         hold the original value.
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
      return empty();
    }
  }
}
