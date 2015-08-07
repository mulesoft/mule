/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.mule.expression.ExpressionConstants.ALL_ARGUMENT;
import static org.mule.expression.ExpressionConstants.DELIM;
import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used by the different header expression evaluators to read message properties, honuouring scope and return type
 */
public final class ExpressionUtils
{
    private ExpressionUtils()
    {
        // don't instantiate
    }

    /**
     * Gets a property or map/list of properties specific by an expression supporting multiple return types as well as all and optional modifiers
     *
     * Handles scope-aware expressions like "#[header:INBOUND:foo]
     * @param expression the header name to evaluate.  this can be prefixed with a message scope such as INBOUND, OUTBOUND
     * or INVOCATION scope. If no scope is defined the default scope is OUTBOUND
     *
     * @param msg the message to evaluate on
     */
    public static Object getPropertyWithScope(String expression, MuleMessage msg)
    {
        return getPropertyWithScope(expression, msg, Object.class);
    }

    /**
     * Gets a property or map/list of properties specific by an expression supporting multiple return types as well as all and optional modifiers
     *
     * Handles scope-aware expressions like "#[header:INBOUND:foo]
     * @param expression the header name to evaluate.  this can be prefixed with a message scope such as INBOUND, OUTBOUND
     * or INVOCATION scope. If no scope is defined the default scope is OUTBOUND
     *
     * @param msg the message to evaluate on
     * @param type the expected return type for this evaluation
     * @return  an object of type 'type' corresponding to the message header requested or null if the header was not on
     * the message in the specified scope
     */
    public static <T> T getPropertyWithScope(String expression, MuleMessage msg, Class<T> type)
    {
        return getPropertyInternal(expression, PropertyScope.OUTBOUND, true, msg, type);
    }

    /**
     * Gets a property or map/list of properties specified by an expression supporting
     * multiple return types as well as all and optional modifiers.
     */
    public static Object getProperty(String expression, PropertyScope scope, MuleMessage msg)
    {
        return getProperty(expression, scope, msg, Object.class);
    }

    /**
     * Gets a property or map/list of properties specific by an expression supporting multiple return types as well as all and optional modifiers
     *
     * @param msg the message to evaluate on
     * @param type the expected return type for this evaluation
     * @return  an object of type 'type' corresponding to the message header requested or null if the header was not on
     * the message in the specified scope
     */
    public static <T> T getProperty(String expression, PropertyScope scope,  MuleMessage msg, Class<T> type)
    {
        return getPropertyInternal(expression, scope, false, msg, type);
    }

    /**
     * Obtains a property or map/list of properties from a message using an expression that specifies which property or properties to evaluate.
     * This method can be used  default scope
     * @param expression the expression used to evaluator the message
     * @param scope the scope to be used when obtaining a property.  This is the default if parseScopes is true.
     * @param parseScope should scope we parsed from expression string.  When true the scope acts as a default.
     * @param msg the message to be evaluated
     * @param type return type expected
     * @return property or list/map of evaluated property values
     */
    @SuppressWarnings("unchecked")
    protected static <T> T getPropertyInternal(String expression, PropertyScope scope, boolean parseScope, MuleMessage msg, Class<T> type)
    {
        if (parseScope)
        {
            PropertyScope tempScope = getScope(expression);
            if (tempScope != null)
            {
                // cut-off leading scope and separator
                expression = expression.substring(tempScope.getScopeName().length() + 1);
                scope = tempScope;
            }
        }

        if (expression.contains(ALL_ARGUMENT))
        {
            WildcardFilter filter = new WildcardFilter(expression);
            if (Map.class.isAssignableFrom(type))
            {
                Map<String, Object> props = new HashMap<String, Object>();
                for (String name : msg.getPropertyNames(scope))
                {
                    if (filter.accept(name))
                    {
                        props.put(name, msg.getProperty(name, scope));
                    }
                }
                return (T) returnMap(props, scope);
            }
            else if (List.class.isAssignableFrom(type))
            {
                List<Object> values = new ArrayList<Object>();
                for (String name : msg.getPropertyNames(scope))
                {
                    if (filter.accept(name))
                    {
                        values.add(msg.getProperty(name, scope));
                    }
                }
                return (T) returnList(values, scope);
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
                PropertyScope entryScope = scope;
                if (parseScope)
                {
                    entryScope = getScope(name);
                    if (entryScope != null)
                    {
                        // cut-off leading scope and separator
                        name = name.substring(entryScope.getScopeName().length() + 1);
                    }
                    else
                    {
                        entryScope = scope;
                    }
                }
                if (name.endsWith(OPTIONAL_ARGUMENT))
                {
                    name = name.substring(0, name.length() - OPTIONAL_ARGUMENT.length());
                    required = false;
                }
                Object value = msg.getProperty(name, entryScope);
                if (value == null && required)
                {
                    throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull("headers", entryScope.getScopeName() + ":" + name));
                }
                else if (value != null)
                {
                    props.put(name, value);
                }
            }
            return (T) returnMap(props, scope);
        }
        else if (List.class.isAssignableFrom(type))
        {
            String[] names = expression.split(DELIM);
            List<Object> values = new ArrayList<Object>();
            for (String name : names)
            {
                boolean required = true;
                name = name.trim();
                PropertyScope itemScope = scope;
                if (parseScope)
                {
                    itemScope = getScope(name);
                    if (itemScope != null)
                    {
                        // cut-off leading scope and separator
                        name = name.substring(itemScope.getScopeName().length() + 1);
                    }
                    else
                    {
                        itemScope = scope;
                    }
                }
                if (name.endsWith(OPTIONAL_ARGUMENT))
                {
                    name = name.substring(0, name.length() - OPTIONAL_ARGUMENT.length());
                    required = false;
                }
                name = name.trim();
                Object value = msg.getProperty(name, itemScope);
                if (value == null && required)
                {
                    throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull("headers-list", itemScope.getScopeName() + ":" + name));
                }
                else if (value != null)
                {
                    values.add(value);
                }
            }
            return (T) returnList(values, scope);
        }
        else
        {
            boolean required = true;
            if (expression.endsWith(OPTIONAL_ARGUMENT))
            {
                expression = expression.substring(0, expression.length() - OPTIONAL_ARGUMENT.length());
                required = false;
            }
            Object result = msg.getProperty(expression.trim(), scope);
            if (result == null && required)
            {
                throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull("header", scope.getScopeName() + ":" + expression));

            }
            return (T) result;
        }
    }

    public static TypedValue getTypedProperty(String expression, MuleMessage msg)
    {
        PropertyScope scope = PropertyScope.OUTBOUND;

        PropertyScope tempScope = getScope(expression);
        if (tempScope != null)
        {
            // cut-off leading scope and separator
            expression = expression.substring(tempScope.getScopeName().length() + 1);
            scope = tempScope;
        }

        return getTypedProperty(expression, msg, scope);
    }

    public static TypedValue getTypedProperty(String expression, MuleMessage msg, PropertyScope scope)
    {
        boolean required = true;
        if (expression.endsWith(OPTIONAL_ARGUMENT))
        {
            expression = expression.substring(0, expression.length() - OPTIONAL_ARGUMENT.length());
            required = false;
        }
        final String propertyName = expression.trim();
        Object result = msg.getProperty(propertyName, scope);
        if (result == null && required)
        {
            throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull("header", scope.getScopeName() + ":" + expression));

        }

        DataType<?> propertyDataType = msg.getPropertyDataType(propertyName, scope);
        if (propertyDataType == null)
        {
            propertyDataType = DataTypeFactory.create(Object.class, null);
        }

        return new TypedValue(result, propertyDataType);
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
        List<Object> l = (values.size() == 0 ? Collections.emptyList() : values);
        if (scope.equals(PropertyScope.INBOUND))
        {
            l = Collections.unmodifiableList(l);
        }
        return l;
    }

    protected static PropertyScope getScope(String expression)
    {
        // see if scope has been specified explicitly
        final String[] tokens = expression.split(":", 2); // note we split only once, not on every separator
        PropertyScope scope;
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
