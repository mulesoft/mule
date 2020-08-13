/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config.metadata;

import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.tooling.internal.config.params.ParameterSimpleValueExtractor.extractSimpleValue;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.extension.api.metadata.NullMetadataKey;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolver that creates a {@link MetadataKey} from a {@link ComponentElementDeclaration}.
 *
 * @since 4.4
 */
public class MetadataKeyDeclarationResolver {

  private ParameterizedModel parameterizedModel;
  private ComponentElementDeclaration componentElementDeclaration;

  public MetadataKeyDeclarationResolver(ParameterizedModel parameterizedModel,
                                        ComponentElementDeclaration componentElementDeclaration) {
    this.parameterizedModel = parameterizedModel;
    this.componentElementDeclaration = componentElementDeclaration;
  }

  public MetadataKey resolveKey() {
    List<ParameterModel> keyPartModels = getMetadataKeyParts(parameterizedModel);

    if (keyPartModels.isEmpty()) {
      return MetadataKeyBuilder.newKey(NullMetadataKey.ID).build();
    }

    MetadataKeyBuilder rootMetadataKeyBuilder = null;
    MetadataKeyBuilder metadataKeyBuilder = null;
    Map<String, String> keyPartValues =
        getMetadataKeyPartsValuesFromComponentDeclaration(componentElementDeclaration, parameterizedModel);
    for (ParameterModel parameterModel : keyPartModels) {
      String id;
      if (keyPartValues.containsKey(parameterModel.getName())) {
        id = keyPartValues.get(parameterModel.getName());
      } else {
        // It is only supported to defined parts in order
        break;
      }

      if (id != null) {
        if (metadataKeyBuilder == null) {
          metadataKeyBuilder = MetadataKeyBuilder.newKey(id).withPartName(parameterModel.getName());
          rootMetadataKeyBuilder = metadataKeyBuilder;
        } else {
          MetadataKeyBuilder metadataKeyChildBuilder = MetadataKeyBuilder.newKey(id).withPartName(parameterModel.getName());
          metadataKeyBuilder.withChild(metadataKeyChildBuilder);
          metadataKeyBuilder = metadataKeyChildBuilder;
        }
      }
    }

    if (metadataKeyBuilder == null) {
      return MetadataKeyBuilder.newKey(NullMetadataKey.ID).build();
    }
    return rootMetadataKeyBuilder.build();
  }

  private List<ParameterModel> getMetadataKeyParts(ParameterizedModel parameterizedModel) {
    return parameterizedModel.getAllParameterModels().stream()
        .filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
        .sorted(comparingInt(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).get().getOrder()))
        .collect(toList());
  }

  private Map<String, String> getMetadataKeyPartsValuesFromComponentDeclaration(ComponentElementDeclaration componentElementDeclaration,
                                                                                ParameterizedModel parameterizedModel) {
    Map<String, String> parametersMap = new HashMap<>();

    Map<String, ParameterGroupModel> parameterGroups =
        parameterizedModel.getParameterGroupModels().stream().collect(toMap(NamedObject::getName, identity()));

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
          parametersMap.put(parameterName, extractSimpleValue(parameterElement.getValue()));
        }
      }
    }

    return parametersMap;
  }

}
