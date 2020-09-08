/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.component.Component.NS_MULE_PARSER_METADATA;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.builder.ComponentAstBuilder;
import org.mule.runtime.dsl.api.xml.parser.ConfigLine;
import org.mule.runtime.dsl.api.xml.parser.SimpleConfigAttribute;
import org.mule.runtime.internal.dsl.DslConstants;

import java.util.Properties;

import javax.xml.namespace.QName;

/**
 * Class used to read xml files from {@link ConfigLine}s, unifying knowledge on how to properly read the files returning the
 * {@link ComponentAst} object.
 *
 * It also replaces the values of the attributes by using the {@link Properties} object parametrized in its constructor.
 */
public class ComponentModelReader {

  public void extractComponentDefinitionModel(ConfigLine configLine, String configFileName,
                                              ComponentAstBuilder componentAstBuilder) {

    String namespace = configLine.getNamespace() == null ? CORE_PREFIX : configLine.getNamespace();
    String namespaceUri = configLine.getNamespaceUri() == null ? DslConstants.CORE_NAMESPACE : configLine.getNamespaceUri();

    ComponentMetadataAst.Builder metadataBuilder = ComponentMetadataAst.builder()
        .setFileName(configFileName)
        .setStartLine(configLine.getLineNumber())
        .setEndLine(configLine.getLineNumber())
        .setStartColumn(configLine.getStartColumn())
        .setEndColumn(configLine.getStartColumn())
        .setSourceCode(configLine.getSourceCode());
    configLine.getCustomAttributes()
        .forEach((key, value) -> {
          QName qname = QName.valueOf(key);

          if (isEmpty(qname.getNamespaceURI()) || NS_MULE_PARSER_METADATA.equals(qname.getNamespaceURI())) {
            metadataBuilder.putParserAttribute(qname.getLocalPart(), value);
          } else {
            metadataBuilder.putDocAttribute(qname.toString(), value.toString());
            if (NS_MULE_DOCUMENTATION.equals(qname.getNamespaceURI())) {
              // This is added for compatibility, since in previous versions the doc attributes were looked up without the
              // namespace.
              metadataBuilder.putDocAttribute(qname.getLocalPart(), value.toString());
            }
          }
        });

    componentAstBuilder.withIdentifier(builder()
        .namespace(namespace)
        .namespaceUri(namespaceUri)
        .name(configLine.getIdentifier())
        .build())
        .withMetadata(metadataBuilder.build());

    componentAstBuilder.withRawParameter(BODY_RAW_PARAM_NAME, configLine.getTextContent());

    for (SimpleConfigAttribute simpleConfigAttribute : configLine.getConfigAttributes().values()) {
      componentAstBuilder.withRawParameter(simpleConfigAttribute.getName(),
                                           simpleConfigAttribute.getValue());
    }

    configLine.getChildren().stream()
        .forEach(childConfigLine -> extractComponentDefinitionModel(childConfigLine, configFileName,
                                                                    componentAstBuilder.addChildComponent()));
  }

}
