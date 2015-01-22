/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.extensions.internal.capability.xml.schema.model;

import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_BOOLEAN;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_DATE_TIME;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_DECIMAL;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_DOUBLE;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_INTEGER;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_LIST;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_LONG;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_MAP;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_OBJECT;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.EXPRESSION_STRING;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.STRING;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_BOOLEAN;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_DATE_TIME;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_DECIMAL;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_INT;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_LONG;
import static org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_NAME;
import org.mule.extensions.introspection.DataType;
import org.mule.module.extensions.internal.introspection.BaseDataQualifierVisitor;
import org.mule.util.ValueHolder;

import javax.xml.namespace.QName;

public final class SchemaTypeConversion
{

    public static QName convertType(final DataType type, final boolean dynamic) {
        final ValueHolder<QName> qName = new ValueHolder<>();
        type.getQualifier().accept(new BaseDataQualifierVisitor()
        {
            @Override
            public void onBoolean()
            {
                qName.set(dynamic ? EXPRESSION_BOOLEAN : SUBSTITUTABLE_BOOLEAN);
            }

            @Override
            public void onInteger()
            {
                qName.set(dynamic ? EXPRESSION_INTEGER : SUBSTITUTABLE_INT);
            }

            @Override
            public void onDouble()
            {
                qName.set(dynamic ? EXPRESSION_DOUBLE : SUBSTITUTABLE_DECIMAL);
            }

            @Override
            public void onDecimal()
            {
                qName.set(dynamic ? EXPRESSION_DECIMAL : SUBSTITUTABLE_DECIMAL);
            }

            @Override
            public void onString()
            {
                qName.set(dynamic ? EXPRESSION_STRING : STRING);
            }

            @Override
            public void onLong()
            {
                qName.set(dynamic ? EXPRESSION_LONG : SUBSTITUTABLE_LONG);
            }

            @Override
            public void onDateTime()
            {
                qName.set(dynamic ? EXPRESSION_DATE_TIME : SUBSTITUTABLE_DATE_TIME);
            }

            @Override
            public void onList()
            {
                qName.set(dynamic ? EXPRESSION_LIST : SUBSTITUTABLE_NAME);
            }

            @Override
            public void onMap()
            {
                qName.set(dynamic ? EXPRESSION_MAP : SUBSTITUTABLE_NAME);
            }

            @Override
            protected void defaultOperation()
            {
                qName.set(dynamic ? EXPRESSION_OBJECT : STRING);
            }
        });

        return qName.get();
    }
}
