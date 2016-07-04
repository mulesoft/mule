/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Record
{

    private final Set<Field> fields = new HashSet<Field>();

    public Record(Field... fields)
    {
        Collections.addAll(this.fields, fields);
    }

    public Record(Map<String, Object> fields)
    {
        for (String name : fields.keySet())
        {
            if ("DESCRIPTION".equalsIgnoreCase(name))
            {
                this.fields.add(new XmlField(name, fields.get(name)));
            }
            else
            {
                this.fields.add(new Field(name, fields.get(name)));
            }
        }
    }

    public Set<Field> getFields()
    {
        return fields;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        Record record = (Record) o;

        return fields.equals(record.fields);
    }

    @Override
    public int hashCode()
    {
        return fields.hashCode();
    }

    @Override
    public String toString()
    {
        return fields.toString();
    }
}
