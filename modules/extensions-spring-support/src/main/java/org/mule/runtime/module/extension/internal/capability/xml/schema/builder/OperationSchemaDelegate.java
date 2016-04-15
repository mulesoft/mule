/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.mule.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.TARGET_ATTRIBUTE_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.TYPE_SUFFIX;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.runtime.core.util.ValueHolder;

import javax.xml.namespace.QName;

/**
 * Builder delegation class to generate a XSD schema that describes a
 * {@link OperationModel}
 *
 * @since 4.0.0
 */
class OperationSchemaDelegate
{

    private final SchemaBuilder builder;
    private Schema schema;

    public OperationSchemaDelegate(SchemaBuilder builder)
    {
        this.builder = builder;
    }

    public void registerOperation(Schema schema, OperationModel operationModel)
    {
        this.schema = schema;

        String typeName = capitalize(operationModel.getName()) + TYPE_SUFFIX;
        registerProcessorElement(operationModel, typeName);
        registerOperationType(typeName, operationModel);
    }

    private void registerProcessorElement(OperationModel operationModel, String typeName)
    {
        Element element = new TopLevelElement();
        element.setName(hyphenize(operationModel.getName()));
        element.setType(new QName(schema.getTargetNamespace(), typeName));
        element.setAnnotation(builder.createDocAnnotation(operationModel.getDescription()));
        element.setSubstitutionGroup(getOperationSubstitutionGroup(operationModel));
        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerOperationType(String name, OperationModel operationModel)
    {
        ExtensionType operationType = builder.registerExecutableType(name, operationModel, MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE);
        addTargetParameter(operationType, operationModel);
    }

    private QName getOperationSubstitutionGroup(OperationModel operationModel)
    {
        ValueHolder<QName> substitutionGroup = new ValueHolder<>(MULE_ABSTRACT_MESSAGE_PROCESSOR);
        operationModel.getModelProperty(ExtendingOperationModelProperty.class)
                .ifPresent(property -> substitutionGroup.set(builder.getSubstitutionGroup(property.getType())));

        return substitutionGroup.get();
    }

    private void addTargetParameter(ExtensionType operationType, OperationModel operationModel)
    {
        if (!isVoid(operationModel))
        {
            Attribute attribute = builder.createAttribute(TARGET_ATTRIBUTE, builder.load(String.class), false, NOT_SUPPORTED);
            attribute.setAnnotation(builder.createDocAnnotation(TARGET_ATTRIBUTE_DESCRIPTION));
            operationType.getAttributeOrAttributeGroup().add(attribute);
        }
    }
}
