/*
 * $Id: FunctionExpressionEvaluator.java 18005 2010-07-09 16:47:06Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.WildcardFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mule.expression.ExpressionConstants.ALL_ARGUMENT;
import static org.mule.expression.ExpressionConstants.DELIM;
import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;

/**
 *
 */
public final class ExpressionUtils
{

    private ExpressionUtils()
    {
        // don't instantiate
    }

    public static Object getPropertyWithScope(String expression, MuleMessage msg)
    {
        return getPropertyWithScope(expression, msg, Object.class);
    }

    /**
     * Handlers scope-aware expressions like "#[header:INBOUND:foo]
     */
    public static <T> T getPropertyWithScope(String expression, MuleMessage msg, Class<T> type)
    {
        PropertyScope defaultScope = getScope(expression);
        if (defaultScope != null)
        {
            // cut-off leading scope and separator
            expression = expression.substring(defaultScope.getScopeName().length() + 1);
        }
        else
        {
            // default
            defaultScope = PropertyScope.OUTBOUND;
        }

        if (expression.contains(ALL_ARGUMENT))
        {
            WildcardFilter filter = new WildcardFilter(expression);
            if (Map.class.isAssignableFrom(type))
            {
                Map<String, Object> props = new HashMap<String, Object>();
                for (String name : msg.getPropertyNames(defaultScope))
                {
                    if (filter.accept(name))
                    {
                        props.put(name, msg.getProperty(name, defaultScope));
                    }
                }
                return (T) returnMap(props, defaultScope);
            }
            else if (List.class.isAssignableFrom(type))
            {
                List<Object> values = new ArrayList<Object>();
                for (String name : msg.getPropertyNames(defaultScope))
                {
                    if (filter.accept(name))
                    {
                        values.add(msg.getProperty(name, defaultScope));
                    }
                }
                return (T) returnList(values, defaultScope);
            }
            else
            {
                //TODO i18n
                throw new IllegalArgumentException("Type specified is not a collection type but '" + ALL_ARGUMENT + "' was specified for all properties. Type is: " + type);
            }
        }
        else if (Map.class.isAssignableFrom(type))
        {
            String[] names = expression.split(DELIM);
            Map<String, Object> props = new HashMap<String, Object>();
            for (String name : names)
            {
                boolean required = true;
                name = name.trim();
                PropertyScope scope = getScope(name);
                if (scope != null)
                {
                    // cut-off leading scope and separator
                    name = name.substring(scope.getScopeName().length() + 1);
                }
                else
                {
                    scope = defaultScope;
                }
                if (name.endsWith(OPTIONAL_ARGUMENT))
                {
                    name = name.substring(0, name.length() - OPTIONAL_ARGUMENT.length());
                    required = false;
                }
                Object value = msg.getProperty(name, scope);
                if (value == null && required)
                {
                    throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull("headers", scope.getScopeName() + ":" + name));
                }
                else if (value != null)
                {
                    props.put(name, value);
                }
            }
            return (T) returnMap(props, defaultScope);
        }
        else if (List.class.isAssignableFrom(type))
        {
            String[] names = expression.split(DELIM);
            List<Object> values = new ArrayList<Object>();
            for (String name : names)
            {
                boolean required = true;
                name = name.trim();
                PropertyScope scope = getScope(name);
                if (scope != null)
                {
                    // cut-off leading scope and separator
                    name = name.substring(scope.getScopeName().length() + 1);
                }
                else
                {
                    scope = defaultScope;
                }
                if (name.endsWith(OPTIONAL_ARGUMENT))
                {
                    name = name.substring(0, name.length() - OPTIONAL_ARGUMENT.length());
                    required = false;
                }
                name = name.trim();
                Object value = msg.getProperty(name, scope);
                if (value == null && required)
                {
                    throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull("headers-list", scope.getScopeName() + ":" + name));
                }
                else if (value != null)
                {
                    values.add(value);
                }
            }
            return (T) returnList(values, defaultScope);
        }
        else
        {
            boolean required = true;
            if (expression.endsWith(OPTIONAL_ARGUMENT))
            {
                expression = expression.substring(0, expression.length() - OPTIONAL_ARGUMENT.length());
                required = false;
            }
            Object result = msg.getProperty(expression.trim(), defaultScope);
            if (result == null && required)
            {
                throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull("header", defaultScope.getScopeName() + ":" + expression));

            }
            return (T) result;
        }
    }

    private static Map<String, Object> returnMap(Map<String, Object> props, PropertyScope scope)
    {
        Map<String, Object> p = (props.size() == 0 ? Collections.<String, Object>emptyMap() : props);
        if (scope.equals(PropertyScope.INBOUND))
        {
            p = Collections.unmodifiableMap(p);
        }
        return p;
    }

    private static List<Object> returnList(List<Object> values, PropertyScope scope)
    {
        List<Object> l = (values.size() == 0 ? Collections.<Object>emptyList() : values);
        if (scope.equals(PropertyScope.INBOUND))
        {
            l = Collections.unmodifiableList(l);
        }
        return l;
    }

    private static PropertyScope getScope(String expression)
    {
        // see if scope has been specified explicitly
        final String[] tokens = expression.split(":", 2); // note we split only once, not on every separator
        PropertyScope scope = null;
        if (tokens.length == 2)
        {
            final String candidate = tokens[0];
            scope = PropertyScope.get(candidate.toLowerCase());
            if (scope == null)
            {
                throw new IllegalArgumentException(String.format("'%s' is not a valid property scope.", candidate));
            }

            return scope;
        }
        return null;
    }
}
