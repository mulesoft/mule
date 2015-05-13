/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;
import org.mule.transport.NullPayload;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A configurable message transformer that allows users to add, overwrite, rename and delete
 * properties on the current message. Users can set a {@link List} of 'deleteProperties' regular 
 * expressions to remove the matching properties from the message and can also set a {@link Map} 
 * of 'addProperties' that will be added to the message and possibly overwrite existing properties
 * with the same name.
 * <p/> 
 * <p>
 * If {@link #overwrite} is set to <code>false</code>, and a property 
 * exists on the message (even if the value is <code>null</code>, it will be left intact. The 
 * transformer then acts as a more gentle 'enricher'. The default setting is <code>true</code>.
 * </p>
 */
public class MessagePropertiesTransformer extends AbstractMessageTransformer
{
    private List<String> deleteProperties = null;
    private Map<String, TypedValue> addProperties = new HashMap<>();
    /** the properties map containing rename mappings for message properties */
    private Map<String, String> renameProperties;
    private String getProperty;
    private boolean overwrite = true;
    // outbound is the default scope
    private PropertyScope scope = PropertyScope.OUTBOUND;

    public MessagePropertiesTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        MessagePropertiesTransformer clone = (MessagePropertiesTransformer) super.clone();

        if (deleteProperties != null)
        {
            clone.setDeleteProperties(new ArrayList<String>(deleteProperties));
        }

        if (addProperties != null)
        {
            clone.setAddTypedProperties(new HashMap<>(addProperties));
        }

        if (renameProperties != null)
        {
            clone.setRenameProperties(new HashMap<String, String>(renameProperties));
        }
        return clone;
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding)
    {
        if (deleteProperties != null && deleteProperties.size() > 0)
        {
            deleteProperties(message);
        }

        if (addProperties != null && addProperties.size() > 0)
        {
            addProperties(message);
        }

        /* perform renaming transformation */
        if (this.renameProperties != null && this.renameProperties.size() > 0)
        {
            renameProperties(message);
        }
        
        if (getProperty != null)
        {
            Object prop = message.getProperty(getProperty, scope);
            if (prop != null)
            {
                message = new DefaultMuleMessage(prop, muleContext);
            }
            else
            {
                message = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
            }
        }

        return message;
    }

    protected void deleteProperties(MuleMessage message)
    {
        final Set<String> propertyNames = new HashSet<String>(message.getPropertyNames(scope));
        
        for (String expression : deleteProperties)
        {
            for (String key : propertyNames)
            {
                if (key.matches(expression))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("Removing property: '%s' from scope: '%s'", key, scope.getScopeName()));
                    }
                    message.removeProperty(key, scope);
                }
                else
                {
                    // fallback to the plain wildcard for simplicity
                    WildcardFilter filter = new WildcardFilter(expression);
                    if (filter.accept(key))
                    {
                        message.removeProperty(key, scope);
                    }
                }
            }
        }
    }

    protected void addProperties(MuleMessage message)
    {
        for (Map.Entry<String, TypedValue> entry : addProperties.entrySet())
        {
            if (entry.getKey() == null)
            {
                logger.error("Setting Null property keys is not supported, this entry is being ignored");
            }
            else
            {
                final String key = entry.getKey();

                TypedValue typedValue = entry.getValue();
                Object value = typedValue.getValue();

                //Enable expression support for property values
                if (muleContext.getExpressionManager().isExpression(value.toString()))
                {
                    value = muleContext.getExpressionManager().evaluate(value.toString(), message);
                }

                if (value != null)
                {
                    DataType<?> propertyDataType = createPropertyDataType(value, typedValue.getDataType());
                    if (message.getProperty(key, scope) != null)
                    {
                        if (overwrite)
                        {
                            logger.debug("Overwriting message property " + key);
                            message.setProperty(key, value, scope, propertyDataType);
                        }
                        else if(logger.isDebugEnabled())
                        {
                            logger.debug(MessageFormat.format(
                                "Message already contains the property and overwrite is false, skipping: key={0}, value={1}, scope={2}",
                                key, value, scope));
                        }
                    }
                    //If value is null an exception will not be thrown if the key was marked as optional (with a '?'). If not
                    //optional the expression evaluator will throw an exception
                    else
                    {
                        message.setProperty(key, value, scope, propertyDataType);
                    }
                }
                else if (logger.isInfoEnabled())
                {
                    logger.info(MessageFormat.format(
                        "Property with key ''{0}'', not found on message using ''{1}''. Since the value was marked optional, nothing was set on the message for this property",
                        key, value));
                }
            }
        }
    }

    private DataType<?> createPropertyDataType(Object value, DataType<?> propertyDataType)
    {
        DataType<?> dataType = DataTypeFactory.create(value.getClass(), propertyDataType.getMimeType());
        dataType.setEncoding(propertyDataType.getEncoding());

        return dataType;
    }

    protected void renameProperties(MuleMessage message)
    {
        for (Map.Entry<String, String> entry : this.renameProperties.entrySet())
        {
            if (entry.getKey() == null)
            {
                logger.error("Setting Null property keys is not supported, this entry is being ignored");
            }
            else
            {
                final String key = entry.getKey();
                String value = entry.getValue();

                if (value == null)
                {
                    logger.error("Setting Null property values for renameProperties is not supported, this entry is being ignored");
                }
                else
                {
                    //Enable expression support for property values
                    if (muleContext.getExpressionManager().isValidExpression(value))
                    {
                        Object temp = muleContext.getExpressionManager().evaluate(value, message);
                        if (temp != null)
                        {
                            value = temp.toString();
                        }
                    }

                    /* log transformation */
                    if (logger.isDebugEnabled() && message.getProperty(key, scope) == null)
                    {
                        logger.debug(String.format("renaming message property '%s' to '%s'", key, value));
                    }

                    renameInScope(key, value, scope, message);
                }
            }
        }
    }

    protected void renameInScope(String oldKey, String newKey, PropertyScope propertyScope, MuleMessage message)
    {
        Object propValue = message.getProperty(oldKey, propertyScope);
        DataType<?> propertyDataType = message.getPropertyDataType(oldKey, propertyScope);
        message.removeProperty(oldKey, propertyScope);
        message.setProperty(newKey, propValue, propertyScope, propertyDataType);
    }

    public List<String> getDeleteProperties()
    {
        return deleteProperties;
    }

    /**
     * @see #setDeleteProperties(String...)
     */
    public void setDeleteProperties(List<String> deleteProperties)
    {
        this.deleteProperties = deleteProperties;
    }

    public void setDeleteProperties(String... deleteProperties)
    {
        this.deleteProperties = Arrays.asList(deleteProperties);
    }

    public Map<String, TypedValue> getAddProperties()
    {
        return addProperties;
    }

    public void setAddTypedProperties(Map<String, TypedValue> addProperties)
    {
        this.addProperties.putAll(addProperties);
    }

    public void setAddProperties(Map<String, Object> addProperties)
    {
        for (String key : addProperties.keySet())
        {
            Object value = addProperties.get(key);
            TypedValue typedValue = new TypedValue(value, value == null ? DataTypeFactory.OBJECT : DataTypeFactory.create(value.getClass()));
            this.addProperties.put(key, typedValue);
        }
    }

    /**
     * @return the renameProperties
     */
    public Map<String, String> getRenameProperties()
    {
        return this.renameProperties;
    }

    /**
     * @param renameProperties the renameProperties to set
     */
    public void setRenameProperties(Map<String, String> renameProperties)
    {
        this.renameProperties = renameProperties;
    }

    public String getGetProperty()
    {
        return getProperty;
    }

    public void setGetProperty(String getProperty)
    {
        this.getProperty = getProperty;
    }

    public boolean isOverwrite()
    {
        return overwrite;
    }

    public void setOverwrite(final boolean overwrite)
    {
        this.overwrite = overwrite;
    }

    public PropertyScope getScope()
    {
        return scope;
    }

    public void setScope(PropertyScope scope)
    {
        this.scope = scope;
    }
    
    /**
     * For XML-based config
     *
     * @return The string value name for a {@link org.mule.api.transport.PropertyScope}
     */
    public String getScopeName()
    {
        return scope != null ? scope.getScopeName() : null;
    }

    /**
     * For XML-based config
     * @param scopeName The string value name for a {@link org.mule.api.transport.PropertyScope}
     */
    public void setScopeName(String scopeName)
    {
        this.scope = PropertyScope.get(scopeName);
    }
}
