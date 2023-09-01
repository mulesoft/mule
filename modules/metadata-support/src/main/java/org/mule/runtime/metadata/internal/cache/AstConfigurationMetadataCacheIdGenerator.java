/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.parameterNamesRequiredForMetadataCacheId;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.resolveComponentIdentifierMetadataCacheId;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.resolveKeyFromSimpleValue;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.resolveMetadataKeyParts;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.sourceElementName;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.getModelNameAst;
import static java.util.stream.Collectors.toList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.cache.ConfigurationMetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class AstConfigurationMetadataCacheIdGenerator implements ConfigurationMetadataCacheIdGenerator {

  // configInternals keeps the ids just considering the child elements of the Config
  private final Map<String, LazyValue<MetadataCacheId>> configInternals = new HashMap<>();
  // configIds keeps the ids of the entire config (considering the config parameters, as well as the internal ones)
  private final Map<String, LazyValue<MetadataCacheId>> configIds = new HashMap<>();

  @Override
  public Optional<MetadataCacheId> getConfigMetadataCacheId(String configName, boolean justProviders) {
    if (isBlank(configName)) {
      return empty();
    }

    Map<String, LazyValue<MetadataCacheId>> ids = justProviders ? configInternals : configIds;

    MetadataCacheId result = ids.containsKey(configName) ? ids.get(configName).get() : null;
    return ofNullable(result);
  }

  @Override
  public void addConfigurations(List<ComponentAst> configAsts) {
    configInternals.clear();
    configIds.clear();
    configAsts.forEach(config -> {
      String configLocation = config.getLocation().getRootContainerName();
      configInternals.put(configLocation, new LazyValue<>(() -> resolveInternalComponents(config)));
      configIds.put(configLocation, new LazyValue<>(() -> doResolve(config)));
    });
  }

  private MetadataCacheId doResolve(ComponentAst elementModel) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    keyParts.add(resolveComponentIdentifierMetadataCacheId(elementModel));

    elementModel.getModel(ComponentModel.class)
        .map(model -> resolveMetadataKeyParts(elementModel, model, true,
                                              configName -> getConfigMetadataCacheId(configName, false)))
        .orElseGet(() -> ofNullable(resolveInternalComponents(elementModel)))
        .ifPresent(keyParts::add);

    return new MetadataCacheId(keyParts, sourceElementName(elementModel));
  }

  private MetadataCacheId resolveInternalComponents(ComponentAst elementModel) {
    List<String> parameterNamesRequiredForMetadata = parameterNamesRequiredForMetadataCacheId(elementModel);

    List<MetadataCacheId> parts = Stream.concat(
                                                elementModel.directChildrenStream().map(this::doResolve),
                                                elementModel.getModel(ParameterizedModel.class).map(pmz -> elementModel
                                                    .getParameters()
                                                    .stream()
                                                    .filter(p -> p.getValue().getValue().isPresent())
                                                    .filter(p -> parameterNamesRequiredForMetadata
                                                        .contains((p.getModel()).getName()))
                                                    .map(p -> resolveKeyFromSimpleValue(elementModel, p,
                                                                                        configName -> getConfigMetadataCacheId(configName,
                                                                                                                               false))))
                                                    .orElse(Stream.empty()))
        .collect(toList());

    if (parts.isEmpty()) {
      return null;
    }

    return new MetadataCacheId(parts, getModelNameAst(elementModel).orElse(null));
  }

}
