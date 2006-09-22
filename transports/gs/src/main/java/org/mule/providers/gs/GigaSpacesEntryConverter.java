/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import com.gigaspaces.converter.pojo.Pojo2ExternalEntryConverter;
import com.j_spaces.core.client.ExternalEntry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.jini.core.entry.Entry;

import org.mule.umo.UMOMessage;

public class GigaSpacesEntryConverter
{
    private final Pojo2ExternalEntryConverter converter = new Pojo2ExternalEntryConverter();

    public ExternalEntry toEntry(Object pojo, UMOMessage msg)
    {
        ExternalEntry result = (ExternalEntry)converter.toEntry(pojo);

        int fieldCount = result.m_FieldsNames.length;
        int newFieldCount = fieldCount + 8;
        String[] fieldNames = new String[newFieldCount];
        String[] fieldTypes = new String[newFieldCount];
        Object[] fieldValues = new Object[newFieldCount];
        boolean[] indexIndicators = new boolean[newFieldCount];

        System.arraycopy(result.m_FieldsNames, 0, fieldNames, 0, fieldCount);
        fieldNames[fieldCount] = "__correlationId";
        fieldNames[fieldCount + 1] = "__correlationSequence";
        fieldNames[fieldCount + 2] = "__correlationGroupSize";
        fieldNames[fieldCount + 3] = "__replyTo";
        fieldNames[fieldCount + 4] = "__messageId";
        fieldNames[fieldCount + 5] = "__properties";
        fieldNames[fieldCount + 6] = "__encoding";
        fieldNames[fieldCount + 7] = "__exceptionPayload";

        System.arraycopy(result.m_FieldsTypes, 0, fieldTypes, 0, fieldCount);
        fieldTypes[fieldCount] = "java.lang.String";
        fieldTypes[fieldCount + 1] = "java.lang.Integer";
        fieldTypes[fieldCount + 2] = "java.lang.Integer";
        fieldTypes[fieldCount + 3] = "java.lang.Object";
        fieldTypes[fieldCount + 4] = "java.lang.String";
        fieldTypes[fieldCount + 5] = "java.util.Map";
        fieldTypes[fieldCount + 6] = "java.lang.String";
        fieldTypes[fieldCount + 7] = "org.mule.umo.UMOExceptionPayload";

        System.arraycopy(result.m_FieldsValues, 0, fieldValues, 0, fieldCount);

        if (msg != null) {
            fieldValues[fieldCount] = msg.getCorrelationId();
            fieldValues[fieldCount + 1] = new Integer(msg.getCorrelationSequence());
            fieldValues[fieldCount + 2] = new Integer(msg.getCorrelationGroupSize());
            fieldValues[fieldCount + 3] = msg.getReplyTo();
            fieldValues[fieldCount + 4] = msg.getUniqueId();

            Map props = new HashMap();
            for (Iterator propNames = msg.getPropertyNames().iterator(); propNames.hasNext();) {
                String propName = (String)propNames.next();
                props.put(propName, msg.getProperty(propName));
            }

            fieldValues[fieldCount + 5] = props;
            fieldValues[fieldCount + 6] = msg.getEncoding();
            fieldValues[fieldCount + 7] = msg.getExceptionPayload();
        }

        if (result.m_IndexIndicators != null) {
            System.arraycopy(result.m_IndexIndicators, 0, indexIndicators, 0, fieldCount);
        }

        result.m_FieldsNames = fieldNames;
        result.m_FieldsTypes = fieldTypes;
        result.m_FieldsValues = fieldValues;
        result.m_IndexIndicators = indexIndicators;

        return result;
    }

    public Object toPojo(Entry entry)
    {
        return converter.toPojo(entry);
    }

}
