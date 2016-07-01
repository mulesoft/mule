/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.module.db.integration.matcher.FieldMatcher.containsField;
import static org.mule.module.db.integration.model.Planet.EARTH;
import static org.mule.module.db.integration.model.Planet.MARS;
import static org.mule.module.db.integration.model.Planet.VENUS;
import org.mule.api.MuleMessage;
import org.mule.module.db.integration.model.Alien;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.module.db.integration.model.XmlField;

import java.sql.SQLException;
import java.sql.Struct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestRecordUtil
{

    public static Record[] getAllPlanetRecords()
    {
        return new Record[] {getVenusRecord(), getEarthRecord(), getMarsRecord()};
    }

    public static Record[] getAllAlienRecords()
    {
        return new Record[] {getMonguitoRecord(), getEtRecord()};
    }

    public static void assertMessageContains(MuleMessage message, Record... records)
    {
        assertRecords(message.getPayload(), records);
    }

    public static void assertRecords(Object value, Record... records)
    {
        assertTrue("Expected a list but received: " + ((value == null) ? "null" : value.getClass().getName()), value instanceof List);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) value;
        assertThat(resultList.size(), equalTo(records.length));

        for (int i = 0, recordsLength = records.length; i < recordsLength; i++)
        {
            Record actualRecord = createRecord(resultList.get(i));
            assertRecord(records[i], actualRecord);
        }
    }

    private static Record createRecord(Map<String, Object> fields)
    {
        Map<String, Object> recordFields = new HashMap<>();
        for (String fieldName : fields.keySet())
        {
            Object fieldValue = fields.get(fieldName);
            if (fieldValue instanceof Object[] && ((Object[]) fieldValue).length > 0)
            {
                final Object[] arrayValue = (Object[]) fieldValue;
                if (arrayValue[0] instanceof Struct)
                {
                    fieldValue = convertStructToObjectArray((Struct) arrayValue[0]);
                }
            }
            else if (fieldValue instanceof Struct)
            {
                fieldValue = convertStructToObjectArray((Struct) fieldValue);
            }
            recordFields.put(fieldName, fieldValue);
        }

        return new Record(recordFields);
    }

    private static Object[] convertStructToObjectArray(Struct value)
    {
        try
        {
            final Object[] attributes = value.getAttributes();
            Object[] arrayFieldValue = new Object[attributes.length];
            for (int i = 0; i < attributes.length; i++)
            {
                arrayFieldValue[i] = attributes[i];
            }
            return arrayFieldValue;
        }
        catch (SQLException e)
        {
            throw new IllegalArgumentException("Cannot transform Struct to Object[]");
        }
    }

    public static void assertRecord(Record expected, Record actual)
    {
        Record expectedRecord = expected;

        for (Field field : expectedRecord.getFields())
        {
            assertThat(actual, containsField(field));
        }
    }

    public static Record getMarsRecord()
    {
        return new Record(new Field("NAME", MARS.getName()));
    }

    public static Record getVenusRecord()
    {
        return new Record(new Field("NAME", VENUS.getName()));
    }

    public static Record getEarthRecord()
    {
        return new Record(new Field("NAME", EARTH.getName()));
    }

    public static Record getMonguitoRecord()
    {
        return new Record(new Field("NAME", Alien.MONGUITO.getName()), new XmlField("DESCRIPTION", Alien.MONGUITO.getXml()));
    }

    public static Record getEtRecord()
    {
        return new Record(new Field("NAME", Alien.ET.getName()), new XmlField("DESCRIPTION", Alien.ET.getXml()));
    }
}
