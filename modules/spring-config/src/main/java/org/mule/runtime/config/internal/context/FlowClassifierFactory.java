/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONFIG;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.APIKIT;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.GENERIC;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.SOAPKIT;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.internal.management.stats.FlowClassifier;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates instances of {@link FlowClassifier} from the information contained in a given {@link ArtifactAst}.
 *
 * @since 4.10
 */
public class FlowClassifierFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(FlowClassifierFactory.class);

  private static final String APIKIT_EXTENSION_NAME = "APIKit";
  private static final String SOAPKIT_EXTENSION = "APIKit for SOAP";
  private static final String KIT_CONFIG_NAME = "config";
  private static final String APIKIT_FLOW_MAPPINGS_PARAM_NAME = "flowMappings";
  private static final String APIKIT_FLOW_REF_PARAM_GROUP_NAME = "FlowMapping";
  private static final String APIKIT_FLOW_REF_PARAM_NAME = "flow-ref";

  private final Set<String> apiKitConfigNames;
  private final Set<String> soapKitConfigNames;
  private final Set<String> apiKitMappedFlows;
  // (SoapKit does not support flow mappings)

  public FlowClassifierFactory(ArtifactAst artifactAst) {
    List<ComponentAst> allApiKitConfigs = getAllConfigs(artifactAst, APIKIT_EXTENSION_NAME);
    apiKitConfigNames = getNamesFromConfigs(allApiKitConfigs.stream());
    apiKitMappedFlows = getFlowMappingsFromConfigs(allApiKitConfigs.stream());

    List<ComponentAst> allSoapKitConfigs = getAllConfigs(artifactAst, SOAPKIT_EXTENSION);
    soapKitConfigNames = getNamesFromConfigs(allSoapKitConfigs.stream());
  }

  private static List<ComponentAst> getAllConfigs(ArtifactAst artifactAst, String extensionName) {
    Stream<ComponentAst> topLevelsFromParent = artifactAst.getParent()
        .map(ArtifactAst::topLevelComponentsStream)
        .orElse(Stream.empty());

    return concat(artifactAst.topLevelComponentsStream(), topLevelsFromParent)
        .filter(c -> componentIsConfigOf(c, extensionName))
        .collect(toList());
  }

  private static boolean componentIsConfigOf(ComponentAst componentAst, String extensionName) {
    return CONFIG.equals(componentAst.getComponentType())
        && componentAst.getExtensionModel().getName().equals(extensionName)
        && componentAst.getModel(ConfigurationModel.class)
            .map(ConfigurationModel::getName)
            .filter(KIT_CONFIG_NAME::equals).isPresent();
  }

  private static Set<String> getNamesFromConfigs(Stream<ComponentAst> configs) {
    return configs
        .filter(c -> c.getComponentId().isPresent())
        .map(c -> c.getComponentId().get())
        .collect(toSet());
  }

  private static Set<String> getFlowMappingsFromConfigs(Stream<ComponentAst> configs) {
    return configs
        .map(FlowClassifierFactory::getFlowMappings)
        .flatMap(Set::stream)
        .collect(toSet());
  }

  private static Set<String> getFlowMappings(ComponentAst apiKitConfig) {
    Set<String> flowMappings = new HashSet<>();
    ComponentParameterAst flowMappingsParameter = apiKitConfig.getParameter(DEFAULT_GROUP_NAME, APIKIT_FLOW_MAPPINGS_PARAM_NAME);
    Object flowMappingsValue = flowMappingsParameter.getValue().getRight();
    if (flowMappingsValue instanceof Iterable<?>) {
      for (Object flowMapping : (Iterable<?>) flowMappingsValue) {
        if (flowMapping instanceof ComponentAst) {
          ComponentParameterAst flowMappingParameter =
              ((ComponentAst) flowMapping).getParameter(APIKIT_FLOW_REF_PARAM_GROUP_NAME, APIKIT_FLOW_REF_PARAM_NAME);
          if (flowMappingParameter.getValue().getRight() instanceof String) {
            flowMappings.add((String) flowMappingParameter.getValue().getRight());
          } else {
            LOGGER
                .error("Component `{}` has a flow-ref that is not a String value, the model `{}` must have changed incompatibly",
                       apiKitConfig.getComponentId(),
                       apiKitConfig.getExtensionModel().getName());
          }
        } else {
          LOGGER.error("Component `{}` has a flow-mapping that is not of the expected type, "
              + "the model `{}` must have changed incompatibly",
                       apiKitConfig.getComponentId(),
                       apiKitConfig.getExtensionModel().getName());
        }
      }
    } else {
      LOGGER.error("Component `{}` has flow mappings but they are not iterable, the model `{}` must have changed incompatibly",
                   apiKitConfig.getComponentId(),
                   apiKitConfig.getExtensionModel().getName());
    }
    return flowMappings;
  }

  public FlowClassifier create() {
    return new DefaultFlowClassifier(apiKitConfigNames, soapKitConfigNames, apiKitMappedFlows);
  }

  private static class DefaultFlowClassifier implements FlowClassifier {

    private final Set<String> apiKitConfigNames;
    private final Set<String> soapKitConfigNames;
    private final Set<String> mappedApiKitFlows;

    private DefaultFlowClassifier(Set<String> apiKitConfigNames, Set<String> soapKitConfigNames,
                                  Set<String> mappedApiKitFlows) {
      this.apiKitConfigNames = apiKitConfigNames;
      this.soapKitConfigNames = soapKitConfigNames;
      this.mappedApiKitFlows = mappedApiKitFlows;
    }

    @Override
    public FlowType getFlowType(String flowName) {
      if (mappedApiKitFlows.contains(flowName)) {
        return APIKIT;
      }

      Optional<String> candidateKitConfigName = tryExtractConfigNameFromFlowName(flowName);
      if (!candidateKitConfigName.isPresent()) {
        return GENERIC;
      }

      String kitConfigName = candidateKitConfigName.get();

      return kitConfigName.startsWith("\\")
          ? soapKitConfigNames.contains(kitConfigName.substring(1)) ? SOAPKIT : GENERIC
          : apiKitConfigNames.contains(kitConfigName) ? APIKIT : GENERIC;
    }

    private Optional<String> tryExtractConfigNameFromFlowName(String flowName) {
      int posFound = flowName.lastIndexOf(":");
      if (posFound == -1) {
        return empty();
      } else {
        return of(flowName.substring(posFound + 1));
      }
    }
  }
}
