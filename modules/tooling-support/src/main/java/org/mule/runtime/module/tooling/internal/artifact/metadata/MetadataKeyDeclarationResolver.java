/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.metadata;

import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;
import static org.mule.runtime.module.tooling.internal.artifact.params.ParameterSimpleValueExtractor.extractSimpleValue;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.extension.api.metadata.NullMetadataKey;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Resolver that creates a {@link MetadataKey} from a {@link ComponentElementDeclaration}.
 * Exposes a {@link MetadataKeyResult} that enables checking if the key is complete or not.
 * Complete meaning that all required parts of the key have a value assigned.
 *
 * @since 4.4
 */
public class MetadataKeyDeclarationResolver {

  private ComponentModel componentModel;
  private ComponentElementDeclaration componentElementDeclaration;

  public MetadataKeyDeclarationResolver(ComponentModel componentModel,
                                        ComponentElementDeclaration componentElementDeclaration) {
    this.componentModel = componentModel;
    this.componentElementDeclaration = componentElementDeclaration;
  }

  public MetadataKey resolveKey() {
    return resolveKeyResult().getMetadataKey();
  }

  public MetadataKeyResult resolveKeyResult() {
    List<MetadataKeyInfo> keyPartModelsInfo = getMetadataKeyPartsInfo(componentModel);

    if (keyPartModelsInfo.isEmpty()) {
      return new MetadataKeyResult(MetadataKeyBuilder.newKey(NullMetadataKey.ID).build());
    }

    MetadataKeyBuilder rootMetadataKeyBuilder = null;
    MetadataKeyBuilder metadataKeyBuilder = null;
    Map<String, String> keyPartValues =
        getMetadataKeyPartsValuesFromComponentDeclaration(componentElementDeclaration, componentModel);
    for (MetadataKeyInfo keyInfo : keyPartModelsInfo) {
      String id;
      if (keyPartValues.containsKey(keyInfo.parameterModel.getName())) {
        id = keyPartValues.get(keyInfo.parameterModel.getName());
      } else {
        // It is only supported to defined parts in order
        break;
      }

      if (id != null) {
        if (metadataKeyBuilder == null) {
          metadataKeyBuilder = MetadataKeyBuilder.newKey(id).withPartName(keyInfo.parameterModel.getName());
          rootMetadataKeyBuilder = metadataKeyBuilder;
        } else {
          MetadataKeyBuilder metadataKeyChildBuilder =
              MetadataKeyBuilder.newKey(id).withPartName(keyInfo.parameterModel.getName());
          metadataKeyBuilder.withChild(metadataKeyChildBuilder);
          metadataKeyBuilder = metadataKeyChildBuilder;
        }
      }
    }

    //TODO MULE-18680 remove `keyPartModels.size() > 1` once bug is fixed to accept optionals in multi-level keys
    List<MetadataKeyInfo> missingPartsInfo = keyPartModelsInfo.stream()
        .filter(ki -> (keyPartModelsInfo.size() > 1 || ki.parameterModel.isRequired())
            && !keyPartValues.containsKey(ki.parameterModel.getName()))
        .collect(toList());
    String partialMessage = null;
    MetadataKey metadataKey = MetadataKeyBuilder.newKey(NullMetadataKey.ID).build();
    if (!missingPartsInfo.isEmpty()) {
      MetadataKeyInfo firstKeyPartInfo = missingPartsInfo.get(0);
      if (missingPartsInfo.size() == 1 && firstKeyPartInfo.level == 1 && firstKeyPartInfo.totalLevels == 1) {
        //Single level key
        partialMessage = format("Missing MetadataKey: %s", firstKeyPartInfo.parameterModel.getName());
      } else {
        //Multi Level key
        partialMessage = format("The given MetadataKey does not provide all the required levels. Missing levels: %s",
                                missingPartsInfo.stream().map(ki -> ki.parameterModel.getName()).collect(toList()));
      }
    }
    if (metadataKeyBuilder != null) {
      metadataKey = rootMetadataKeyBuilder.build();
    }
    return new MetadataKeyResult(metadataKey, partialMessage);
  }

  private List<MetadataKeyInfo> getMetadataKeyPartsInfo(ComponentModel componentModel) {
    List<MetadataKeyInfo> metadataKeyPartsInfo = new LinkedList<>();
    componentModel
        .getParameterGroupModels()
        .forEach(
                 pg -> {
                   List<ParameterModel> keysInGroup = pg.getParameterModels()
                       .stream()
                       .filter(pm -> pm.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
                       .sorted(comparingInt(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).get().getOrder()))
                       .collect(toList());
                   keysInGroup.forEach(k -> metadataKeyPartsInfo.add(new MetadataKeyInfo(k,
                                                                                         k.getModelProperty(MetadataKeyPartModelProperty.class)
                                                                                             .get().getOrder(),
                                                                                         keysInGroup.size())));
                 });
    return metadataKeyPartsInfo;
  }

  private Map<String, String> getMetadataKeyPartsValuesFromComponentDeclaration(ComponentElementDeclaration componentElementDeclaration,
                                                                                ComponentModel componentModel) {
    Map<String, String> parametersMap = new HashMap<>();

    Map<String, ParameterGroupModel> parameterGroups =
        componentModel.getParameterGroupModels().stream().collect(toMap(NamedObject::getName, identity()));

    for (ParameterGroupElementDeclaration parameterGroupElement : componentElementDeclaration.getParameterGroups()) {
      final String parameterGroupName = parameterGroupElement.getName();
      final ParameterGroupModel parameterGroupModel = parameterGroups.get(parameterGroupName);
      if (parameterGroupModel == null) {
        throw new MuleRuntimeException(createStaticMessage("Could not find parameter group with name: %s in model",
                                                           parameterGroupName));
      }

      for (ParameterElementDeclaration parameterElement : parameterGroupElement.getParameters()) {
        final String parameterName = parameterElement.getName();
        final ParameterModel parameterModel = parameterGroupModel.getParameter(parameterName)
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find parameter with name: %s in parameter group: %s",
                                                                            parameterName, parameterGroupName)));
        if (parameterModel.getModelProperty(MetadataKeyPartModelProperty.class).isPresent()) {
          String value = extractSimpleValue(parameterElement.getValue());
          if (!parameterModel.getExpressionSupport().equals(NOT_SUPPORTED) && isExpression(value)) {
            throw new ExpressionNotSupportedException(format("Error resolving value for parameter: '%s' from declaration, it cannot be an EXPRESSION value",
                                                             parameterName));
          }
          parametersMap.put(parameterName, value);
        }
      }
    }

    return parametersMap;
  }

  private static class MetadataKeyInfo {

    private int level;
    private int totalLevels;
    private ParameterModel parameterModel;

    private MetadataKeyInfo(ParameterModel parameterModel, int level, int totalLevels) {
      this.parameterModel = parameterModel;
      this.level = level;
      this.totalLevels = totalLevels;
    }

  }

}
