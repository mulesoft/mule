/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml.schema.builder;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_MESSAGE_SOURCE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_MESSAGE_SOURCE_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.TYPE_SUFFIX;
import static org.mule.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.extension.api.introspection.SourceModel;
import org.mule.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import javax.xml.namespace.QName;

/**
 * Builder delegation class to generate a XSD schema that describes a
 * {@link SourceModel}
 *
 * @since 4.0.0
 */
class SourceSchemaDelegate
{

    private final SchemaBuilder builder;
    private Schema schema;

    public SourceSchemaDelegate(SchemaBuilder builder)
    {
        this.builder = builder;
    }

    public void registerMessageSource(Schema schema, SourceModel sourceModel)
    {
        this.schema = schema;

        String typeName = capitalize(sourceModel.getName()) + TYPE_SUFFIX;
        registerSourceElement(sourceModel, typeName);
        registerSourceType(typeName, sourceModel);
    }

    private void registerSourceElement(SourceModel sourceModel, String typeName)
    {
        Element element = new TopLevelElement();
        element.setName(hyphenize(sourceModel.getName()));
        element.setType(new QName(schema.getTargetNamespace(), typeName));
        element.setAnnotation(builder.createDocAnnotation(sourceModel.getDescription()));
        element.setSubstitutionGroup(MULE_ABSTRACT_MESSAGE_SOURCE);
        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerSourceType(String name, SourceModel sourceModel)
    {
        builder.registerExecutableType(name, sourceModel, MULE_ABSTRACT_MESSAGE_SOURCE_TYPE);
    }
}
