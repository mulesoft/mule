/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension.schema;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MODULE_OPERATION_CONFIG_REF;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.TimeType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.dsl.model.extension.ModuleExtension;
import org.mule.runtime.config.spring.dsl.model.extension.OperationExtension;
import org.mule.runtime.config.spring.dsl.model.extension.ParameterExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.utils.NamespaceMap;

/**
 * A class that given a {@link ModuleExtension} returns a schema as an stream when executing {@link #getSchema(ModuleExtension)}.
 * //TODO MULE-10866 : once we rely the <module>s in {@link org.mule.runtime.api.meta.model.ExtensionModel}, the generation will be not only consistent, but it will also imply this class must be deleted.
 */
public class ModuleSchemaGenerator {

  private static final String MULE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  private static final String MULE_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
  private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
  private static final String MULE_PREFIX = "mule";

  private static final String ABSTRACT_MESSAGE_PROCESSOR_ELEMENT = "abstract-message-processor";
  private static final String ABSTRACT_MESSAGE_PROCESSOR_TYPE = "abstractMessageProcessorType";
  private static final String MULE_SUBSTITUTABLE_NAME_TYPE = "substitutableName";
  private static final String TYPE_SUFFIX = "-type";
  private static final String ABSTRACT_EXTENSION_ELEMENT = "abstract-extension";
  private static final String ABSTRACT_EXTENSION_TYPE = "abstractExtensionType";
  private static final String NAME_ATTRIBUTE = "name";

  private static final QName STRING = new QName(XSD_NAMESPACE, "string", "xs");
  private static final QName EXPRESSION_STRING = new QName(MULE_NAMESPACE, "expressionString", MULE_PREFIX);
  private static final QName EXPRESSION_BOOLEAN = new QName(MULE_NAMESPACE, "expressionBoolean", MULE_PREFIX);
  private static final QName EXPRESSION_INTEGER = new QName(MULE_NAMESPACE, "expressionInt", MULE_PREFIX);
  private static final QName EXPRESSION_OBJECT = new QName(MULE_NAMESPACE, "expressionObject", MULE_PREFIX);
  private static final QName EXPRESSION_DATE_TIME = new QName(MULE_NAMESPACE, "expressionDateTime", MULE_PREFIX);

  public InputStream getSchema(ModuleExtension moduleExtension) {
    InputStream result;
    XmlSchema xmlSchema = parseModule(moduleExtension);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      xmlSchema.write(out);
      result = new ByteArrayInputStream(out.toByteArray());
    } catch (IOException e) {
      result = null;
    }
    return result;
  }

  private XmlSchema parseModule(ModuleExtension moduleExtension) {
    XmlSchema schema =
        new XmlSchema(XMLConstants.W3C_XML_SCHEMA_NS_URI, moduleExtension.getNamespace(), new XmlSchemaCollection());
    schema.setTargetNamespace(moduleExtension.getNamespace());
    schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);

    //adding mule namespace and import
    NamespaceMap namespaceContext = new NamespaceMap();
    namespaceContext.add(MULE_PREFIX, MULE_NAMESPACE);
    schema.setNamespaceContext(namespaceContext);

    XmlSchemaImport muleSchemaImport = new XmlSchemaImport(schema);
    muleSchemaImport.setNamespace(MULE_NAMESPACE);
    muleSchemaImport.setSchemaLocation(MULE_SCHEMA_LOCATION);

    //add config element if necessary
    if (moduleExtension.hasConfig()) {
      generateConfig(moduleExtension, schema);
    }
    //add operations elements
    moduleExtension.getOperations().forEach((operationName, operationExtension) -> {
      generateOperation(schema, moduleExtension, operationExtension);
    });

    return schema;
  }

  private void generateConfig(ModuleExtension moduleExtension, XmlSchema schema) {
    final XmlSchemaElement schemaElement = new XmlSchemaElement(schema, true);
    schemaElement.setName(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);

    XmlSchemaComplexType configSchemaType =
        generateConfigSchemaType(schema, moduleExtension.getProperties(), MODULE_CONFIG_GLOBAL_ELEMENT_NAME);

    schemaElement.setSchemaTypeName(configSchemaType.getQName());

    QName messageProcessorQName = new QName(MULE_NAMESPACE, ABSTRACT_EXTENSION_ELEMENT, MULE_PREFIX);
    schemaElement.setSubstitutionGroup(messageProcessorQName);
  }

  private void generateOperation(XmlSchema schema, ModuleExtension moduleExtension, OperationExtension operationExtension) {
    final XmlSchemaElement schemaElement = new XmlSchemaElement(schema, true);
    schemaElement.setName(operationExtension.getName());

    XmlSchemaComplexType operationSchemaType = generateOperationSchemaType(schema, moduleExtension, operationExtension);
    schemaElement.setSchemaTypeName(operationSchemaType.getQName());

    QName messageProcessorQName = new QName(MULE_NAMESPACE, ABSTRACT_MESSAGE_PROCESSOR_ELEMENT, MULE_PREFIX);
    schemaElement.setSubstitutionGroup(messageProcessorQName);
  }

  private XmlSchemaComplexType generateOperationSchemaType(XmlSchema schema, ModuleExtension moduleExtension,
                                                           OperationExtension operationExtension) {
    ArrayList<XmlSchemaAttributeOrGroupRef> attributes = generateAttributes(schema, operationExtension.getParameters());
    if (moduleExtension.hasConfig()) {
      XmlSchemaAttribute configRefAttribute = new XmlSchemaAttribute(schema, false);
      configRefAttribute.setName(MODULE_OPERATION_CONFIG_REF);
      QName attributeTypeQName = new QName(MULE_NAMESPACE, MULE_SUBSTITUTABLE_NAME_TYPE, MULE_PREFIX);
      configRefAttribute.setSchemaTypeName(attributeTypeQName);
      configRefAttribute.setUse(XmlSchemaUse.REQUIRED);
      attributes.add(configRefAttribute);
    }

    return generateSchemaType(schema, operationExtension.getName(), ABSTRACT_MESSAGE_PROCESSOR_TYPE, attributes);
  }

  private XmlSchemaComplexType generateConfigSchemaType(XmlSchema schema, List<ParameterExtension> parameters,
                                                        String operationXmlName) {
    ArrayList<XmlSchemaAttributeOrGroupRef> attributes = generateAttributes(schema, parameters);

    XmlSchemaAttribute configRefAttribute = new XmlSchemaAttribute(schema, false);
    configRefAttribute.setName(NAME_ATTRIBUTE);
    configRefAttribute.setSchemaTypeName(STRING);
    configRefAttribute.setUse(XmlSchemaUse.REQUIRED);
    attributes.add(configRefAttribute);

    return generateSchemaType(schema, operationXmlName, ABSTRACT_EXTENSION_TYPE, attributes);
  }

  private XmlSchemaComplexType generateSchemaType(XmlSchema schema, String xmlElementName, String localPart,
                                                  List<XmlSchemaAttributeOrGroupRef> attributes) {
    XmlSchemaComplexType operationSchemaType = new XmlSchemaComplexType(schema, true);
    operationSchemaType.setName(xmlElementName.concat(TYPE_SUFFIX));

    XmlSchemaComplexContent complexContent = new XmlSchemaComplexContent();
    XmlSchemaComplexContentExtension complexContentExtension = new XmlSchemaComplexContentExtension();

    QName baseQName = new QName(MULE_NAMESPACE, localPart, MULE_PREFIX);
    complexContentExtension.setBaseTypeName(baseQName);
    complexContentExtension.getAttributes().addAll(attributes);

    complexContent.setContent(complexContentExtension);
    operationSchemaType.setContentModel(complexContent);

    return operationSchemaType;
  }

  private ArrayList<XmlSchemaAttributeOrGroupRef> generateAttributes(XmlSchema schema, List<ParameterExtension> parameters) {
    ArrayList<XmlSchemaAttributeOrGroupRef> attributes = new ArrayList<>();
    for (ParameterExtension parameterExtension : parameters) {
      XmlSchemaAttribute attribute = new XmlSchemaAttribute(schema, false);
      attribute.setName(parameterExtension.getName());

      parameterExtension.getType().accept(new MetadataTypeVisitor() {

        @Override
        protected void defaultVisit(MetadataType metadataType) {
          attribute.setSchemaTypeName(EXPRESSION_OBJECT);
        }

        @Override
        public void visitBoolean(BooleanType booleanType) {
          attribute.setSchemaTypeName(EXPRESSION_BOOLEAN);
        }

        @Override
        public void visitNumber(NumberType numberType) {
          attribute.setSchemaTypeName(EXPRESSION_INTEGER);
        }

        @Override
        public void visitString(StringType stringType) {
          attribute.setSchemaTypeName(EXPRESSION_STRING);
        }

        @Override
        public void visitTime(TimeType timeType) {
          attribute.setSchemaTypeName(EXPRESSION_DATE_TIME);
        }

        @Override
        public void visitDateTime(DateTimeType dateTimeType) {
          attribute.setSchemaTypeName(EXPRESSION_DATE_TIME);
        }

        @Override
        public void visitDate(DateType dateType) {
          attribute.setSchemaTypeName(EXPRESSION_DATE_TIME);
        }
      });

      if (parameterExtension.getDefaultValue().isPresent()) {
        attribute.setUse(XmlSchemaUse.OPTIONAL);
        attribute.setDefaultValue(parameterExtension.getDefaultValue().get());
      } else {
        attribute.setUse(XmlSchemaUse.REQUIRED);
      }
      attributes.add(attribute);
    }
    return attributes;
  }
}
