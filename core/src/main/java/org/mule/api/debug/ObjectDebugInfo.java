/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import static org.mule.util.Preconditions.checkArgument;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides debug information for an object
 *
 * @since 3.8.0
 */
public class ObjectDebugInfo
{

    private final List<FieldDebugInfo> fields;
    private final Class type;

    /**
     * Creates debug information for an object
     *
     * @param type class of the field. Can be null.
     * @param fields a {@link List} containing debug information for the
     *               object's fields. Non empty and must not contain multiple
     *               {@link FieldDebugInfo} with the same name
     */
    public ObjectDebugInfo(Class type, List<FieldDebugInfo> fields)
    {
        checkArgument(fields != null && !fields.isEmpty(), "Fields cannot be null or empty");
        final Set<String> duplicatedFieldNames = findDuplicatedFieldNames(fields);
        checkArgument(duplicatedFieldNames.isEmpty(), "Duplicated field names: " + duplicatedFieldNames);

        this.type = type;
        this.fields = Collections.unmodifiableList(fields);
    }

    private Set<String> findDuplicatedFieldNames(List<FieldDebugInfo> fields)
    {
        Set<String> fieldNames = new HashSet<>();
        Set<String> duplicatedFieldNames = new HashSet<>();

        for (FieldDebugInfo field : fields)
        {
            if (fieldNames.contains(field.getName()))
            {
                duplicatedFieldNames.add(field.getName());
            }
            else
            {
                fieldNames.add(field.getName());
            }
        }

        return duplicatedFieldNames;
    }

    public Class getType()
    {
        return type;
    }

    public List<FieldDebugInfo> getFields()
    {
        return fields;
    }
}
