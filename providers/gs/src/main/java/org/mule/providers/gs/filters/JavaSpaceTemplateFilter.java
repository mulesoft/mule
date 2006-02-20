/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.gs.filters;

import net.jini.core.entry.Entry;
import org.apache.commons.beanutils.BeanUtils;
import org.mule.config.i18n.Message;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.util.ClassHelper;
import org.mule.util.Utility;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Configures an entry template on a JavaSpace endpoint
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JavaSpaceTemplateFilter implements UMOFilter {

    public static final String NULL_VALUE = "null";

    private String expectedType = null;
    private Map fields = new HashMap();
    private Entry entry = null;

    public JavaSpaceTemplateFilter()
    {
    }

    public JavaSpaceTemplateFilter(String expectedType)
    {
        setExpectedType(expectedType);
    }

    /**
     * Check a given message against this filter.
     *
     * @param message a non null message to filter.
     * @return <code>true</code> if the message matches the filter
     */
    public boolean accept(UMOMessage message) {
        //This filter is Used to configure a template on a space endpoint
        return true;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(String expectedType) {
        if(NULL_VALUE.equalsIgnoreCase(expectedType) || Utility.EMPTY_STRING.equals(expectedType)) {
            expectedType=null;
        } else {
            this.expectedType = expectedType;
        }
    }

    public Map getFields() {
        return fields;
    }

    public void setFields(Map fields) {
        for (Iterator iterator = fields.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            setProperty(entry.getKey(), entry.getValue());
        }
    }

    public Entry getEntry() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException, ClassNotFoundException {

        if(entry==null) {
            if(expectedType==null) return null; //Match all template
            entry = (Entry)ClassHelper.instanciateClass(expectedType, ClassHelper.NO_ARGS);
            if(entry.getClass().isAssignableFrom(Entry.class)) {
                throw new IllegalArgumentException(new Message("gs", 1, expectedType).toString());
            }
            if(fields.size() > 0) {
                BeanUtils.populate(entry, fields);
            }
        }
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    //Useful when configuring the filter programmatically
    public Object getProperty(Object key) {
        return fields.get(key);
    }

    //Useful when configuring the filter programmatically
    public void setProperty(Object key, Object value) {
        if(NULL_VALUE.equalsIgnoreCase(value.toString()) ) {
            fields.put(key, null);
        } else {
            fields.put(key, value);
        }
    }

    public String toString() {
         return "JiniEntryFilter{" + "expectedType=" + expectedType + ", fields=" + fields + "}";
    }
}
