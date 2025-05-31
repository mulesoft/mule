/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONFIG;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.management.stats.FlowClassifier.FlowType.APIKIT;
import static org.mule.runtime.core.api.management.stats.FlowClassifier.FlowType.GENERIC;
import static org.mule.runtime.core.api.management.stats.FlowClassifier.FlowType.SOAPKIT;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.api.management.stats.FlowClassifier;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Creates instances of {@link FlowClassifier} from the information contained in a given {@link ArtifactAst}.
 *
 * @since 4.10
 */
public class FlowClassifierFactory {

  private static final String APIKIT_EXTENSION_NAME = "APIKit";
  private static final String SOAPKIT_EXTENSION = "APIKit for SOAP";

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
        .toList();
  }

  private static boolean componentIsConfigOf(ComponentAst componentAst, String extensionName) {
    return CONFIG.equals(componentAst.getComponentType())
        && componentAst.getExtensionModel().getName().equals(extensionName)
        && componentAst.getModel(ConfigurationModel.class)
            .map(ConfigurationModel::getName)
            .filter("config"::equals).isPresent();
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
    ComponentParameterAst flowMappingsParameter = apiKitConfig.getParameter(DEFAULT_GROUP_NAME, "flowMappings");
    Object flowMappingsValue = flowMappingsParameter.getValue().getRight();
    if (flowMappingsValue instanceof List) {
      for (Object flowMapping : (List<?>) flowMappingsValue) {
        if (flowMapping instanceof ComponentAst) {
          ComponentParameterAst flowMappingParameter = ((ComponentAst) flowMapping).getParameter("FlowMapping", "flow-ref");
          if (flowMappingParameter.getValue().getRight() instanceof String) {
            flowMappings.add((String) flowMappingParameter.getValue().getRight());
          }
        }
      }
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
      if (candidateKitConfigName.isEmpty()) {
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
