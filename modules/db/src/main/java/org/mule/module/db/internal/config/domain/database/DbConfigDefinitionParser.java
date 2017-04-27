/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import static org.mule.module.db.internal.domain.type.JdbcTypes.types;
import static org.springframework.util.xml.DomUtils.getChildElementByTagName;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.module.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.module.db.internal.domain.type.ResolvedDbType;
import org.mule.module.db.internal.domain.type.StructDbType;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public class DbConfigDefinitionParser extends MuleOrphanDefinitionParser
{
    public static final String CONNECTION_PROPERTIES_ELEMENT_NAME = "connection-properties";
    public static final String PROPERTY_ELEMENT_NAME = "property";
    public static final String DATA_TYPES_ELEMENT = "data-types";
    public static final String DATA_TYPE_ELEMENT = "data-type";
    public static final String TYPE_ID_ATTRIBUTE = "id";

    private static final Map<String, Integer> TRANSACTION_ISOLATION_MAPPING;

    private static final String KEY_ATTRIBUTE_NAME = "key";
    private static final String VALUE_ATTRIBUTE_NAME = "value";

    static
    {
        TRANSACTION_ISOLATION_MAPPING = new java.util.HashMap<String, Integer>();
        TRANSACTION_ISOLATION_MAPPING.put("", -1); // this is the default in xapool
        TRANSACTION_ISOLATION_MAPPING.put("NONE", Connection.TRANSACTION_NONE);
        TRANSACTION_ISOLATION_MAPPING.put("READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
        TRANSACTION_ISOLATION_MAPPING.put("READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
        TRANSACTION_ISOLATION_MAPPING.put("REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ);
        TRANSACTION_ISOLATION_MAPPING.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
    }

    public static final String TRANSACTION_ISOLATION_ATTRIBUTE = "transactionIsolation";
    public static final String URL_ATTRIBUTE = "url";
    public static final String USER_ATTRIBUTE = "user";
    public static final String PASSWORD_ATTRIBUTE = "password";
    public static final String DRIVER_ATTRIBUTE = "driver";
    public static final String HOST_ATTRIBUTE = "host";
    public static final String PORT_ATTRIBUTE = "port";
    public static final String DATABASE_ATTRIBUTE = "database";
    public static final String LOGIN_TIMEOUT_ATTRIBUTE = "connectionTimeout";
    public static final String DATA_SOURCE_REF_ATTRIBUTE = "dataSource-ref";
    public static final String USE_XA_TRANSACTIONS_ATTRIBUTE = "useXaTransactions";
    public static final String TYPE_NAME_ATTIRBUTE = "name";

    public DbConfigDefinitionParser(Class<? extends DbConfigResolverFactoryBean> dbConfigFactoryClass, CheckExclusiveAttributes exclusiveAttributes)
    {
        super(dbConfigFactoryClass, true);

        addMapping(TRANSACTION_ISOLATION_ATTRIBUTE, TRANSACTION_ISOLATION_MAPPING);
        addIgnored(CONNECTION_PROPERTIES_ELEMENT_NAME);
        addIgnored(DATA_TYPES_ELEMENT);
        registerPreProcessor(exclusiveAttributes);
    }

    @Override
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        super.doParse(element, context, builder);

        parseConnectionProperties(element, builder);
        parseCustomDataTypes(element, builder);
    }

    private void parseCustomDataTypes(Element element, BeanDefinitionBuilder builder)
    {
        List<DbType> customDbTypes = new ArrayList<DbType>();

        Element customTypes = getChildElementByTagName(element, DATA_TYPES_ELEMENT);

        if (customTypes != null)
        {
            for (Element dataType : DomUtils.getChildElementsByTagName(customTypes, DATA_TYPE_ELEMENT))
            {
                String name = dataType.getAttribute(TYPE_NAME_ATTIRBUTE);
                int id = Integer.valueOf(dataType.getAttribute(TYPE_ID_ATTRIBUTE));
                if (id == Types.ARRAY)
                {
                    customDbTypes.add(new ArrayResolvedDbType(id, name));
                }
                else if (id == Types.STRUCT)
                {
                    final String className = dataType.getAttribute("className");
                    if (!StringUtils.isEmpty(className))
                    {
                        Class<?> mappedClass;
                        try
                        {
                            mappedClass = Class.forName(className);
                        }
                        catch (ClassNotFoundException e)
                        {
                            throw new IllegalArgumentException("Cannot find mapped class: " + className);
                        }
                        customDbTypes.add(new MappedStructResolvedDbType(id, name, mappedClass));
                    }
                    else
                    {
                        customDbTypes.add(new StructDbType(id, name));
                    }
                }
                else if (types.contains(new ResolvedDbType(id, name)))
                {
                    customDbTypes.add(types.get(types.indexOf(new ResolvedDbType(id, name))));
                }
                else
                {
                    customDbTypes.add(new ResolvedDbType(id, name));
                }
            }
        }

        builder.addPropertyValue("customDataTypes", customDbTypes);
    }

    private void parseConnectionProperties(Element element, BeanDefinitionBuilder builder)
    {
        Map<String, String> propertiesMap = new LinkedHashMap<String, String>();

        Element properties = getChildElementByTagName(element, CONNECTION_PROPERTIES_ELEMENT_NAME);

        if (properties != null)
        {
            for (Element property : DomUtils.getChildElementsByTagName(properties, PROPERTY_ELEMENT_NAME))
            {
                propertiesMap.put(property.getAttribute(KEY_ATTRIBUTE_NAME), property.getAttribute(VALUE_ATTRIBUTE_NAME));
            }

        }

        builder.addPropertyValue("connectionProperties", propertiesMap);
    }

}
