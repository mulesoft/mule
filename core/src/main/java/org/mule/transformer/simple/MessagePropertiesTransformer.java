/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A configurable message transformer that allows users to add, overwrite and delete
 * properties on the current message. Users can set a {@link List} of
 * 'deleteProperties' regular expressions to remove the matching properties
 * from the message and can also set a {@link Map} of 'addProperties' that
 * will be added to the message and possibly overwrite existing properties
 * with the same name. <p/> If {@link #overwrite} is set to
 * <code>false</code>, and a property exists on the message (even if the value is
 * <code>null</code>, it will be left intact. The transformer then acts as a more
 * gentle 'enricher'. The default setting is <code>true</code>.
 */
public class MessagePropertiesTransformer extends AbstractMessageAwareTransformer implements MuleContextAware
{
    private List deleteProperties = null;
    private Map addProperties = null;
    /** the properties map containing rename mappings for message properties */
    private Map renameProperties;
    private String getProperty;
    private boolean overwrite = true;
    private PropertyScope scope;

    private MuleContext muleContext;

    public MessagePropertiesTransformer()
    {
        registerSourceType(Object.class);
        setReturnDataType(DataTypeFactory.create(Object.class));
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        MessagePropertiesTransformer clone = (MessagePropertiesTransformer)super.clone();

        if (deleteProperties != null)
        {
            clone.setDeleteProperties(new ArrayList(deleteProperties));
        }

        if (addProperties != null)
        {
            clone.setAddProperties(new HashMap(addProperties));
        }

        if (renameProperties != null)
        {
            clone.setRenameProperties(new HashMap(renameProperties));
        }
        return clone;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        if (hasPropertiesToRemove())
        {
            removePropertiesFromMessage(message);
        }

        if (hasPropertiesToAdd())
        {
            addPropertiesToMessage(message);
        }

        if (hasPropertiesToRename())
        {
            renamePropertiesInMessage(message);
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

    private boolean hasPropertiesToAdd()
    {
        return addProperties != null && addProperties.size() > 0;
    }

    private boolean hasPropertiesToRemove()
    {
        return deleteProperties != null && deleteProperties.size() > 0;
    }

    private boolean hasPropertiesToRename()
    {
        return this.renameProperties != null && this.renameProperties.size() > 0;
    }

    private void renamePropertiesInMessage(MuleMessage message)
    {
        for (Iterator iterator = this.renameProperties.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();

            if (entry.getKey() == null)
            {
                logger.error("Setting Null property keys is not supported, this entry is being ignored");
            }
            else
            {
                final String key = entry.getKey().toString();
                String value = (String) entry.getValue();

                if (value == null)
                {
                    logger.error("Setting Null property values for renameProperties is not supported, this entry is being ignored");
                }
                else
                {
                    //Enable expression support for property values
                    if (muleContext.getExpressionManager().isValidExpression(value))
                    {
                        Object temp = muleContext.getExpressionManager().evaluate(value.toString(), message);
                        if (temp != null)
                        {
                            value = temp.toString();
                        }
                    }

                    /* log transformation */
                    if (logger.isDebugEnabled() && message.getProperty(key, scope) == null)
                    {
                        logger.debug("renaming message property " + key + " to " + value);
                    }

                    /*
                     * store current value of the property. then remove key and
                     * store value under new key
                     */
                    Object propValue = message.getProperty(key, scope);
                    message.removeProperty(key, scope);
                    message.setProperty(value, propValue, scope);
                }
            }
        }
    }

    private void addPropertiesToMessage(MuleMessage message)
    {
        for (Iterator iterator = addProperties.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (entry.getKey() == null)
            {
                logger.error("Setting Null property keys is not supported, this entry is being ignored");
            }
            else
            {
                final String key = entry.getKey().toString();

                Object value = entry.getValue();

                //Enable expression support for property values
                if (muleContext.getExpressionManager().isValidExpression(value.toString()))
                {
                    value = muleContext.getExpressionManager().evaluate(value.toString(), message);
                }

                if (message.getProperty(key, scope) != null)
                {
                    if (overwrite)
                    {
                        logger.debug("Overwriting message property " + key);
                        message.setProperty(key, value, scope);
                    }
                    else
                    {
                        logger.debug(MessageFormat.format(
                                "Message already contains the property and overwrite is false, skipping: key={0}, value={1}, scope={2}",
                                key, value, scope));
                    }
                }
                else
                {
                    message.setProperty(key, value, scope);
                }
            }
        }
    }

    private void removePropertiesFromMessage(MuleMessage message)
    {
        Set<String> propNameSet = getPropertiesFromMessage(message);

        for (Iterator iterator = deleteProperties.iterator(); iterator.hasNext();)
        {
            String expression = iterator.next().toString();

            for (String propName : propNameSet)
            {
                if (Pattern.matches(expression, propName))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("Removing property: '%s' from scope: '%s'", propName, (scope == null) ? "all" : scope.getScopeName()));
                    }

                    message.removeProperty(propName, scope);
                }
            }
        }
    }

    private Set<String> getPropertiesFromMessage(MuleMessage message)
    {
        if (scope == null)
        {
            return message.getPropertyNames();
        }
        else
        {
            return message.getPropertyNames(scope);
        }
    }

    public List getDeleteProperties()
    {
        return deleteProperties;
    }

    public void setDeleteProperties(List deleteProperties)
    {
        this.deleteProperties = deleteProperties;
    }

    public Map getAddProperties()
    {
        return addProperties;
    }

    public void setAddProperties(Map addProperties)
    {
        this.addProperties = addProperties;
    }

    /**
     * @return the renameProperties
     */
    public Map getRenameProperties()
    {
        return this.renameProperties;
    }

    /**
     * @param renameProperties the renameProperties to set
     */
    public void setRenameProperties(Map renameProperties)
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
    
    /** For XML-based config */
    public String getScopeName()
    {
        return scope != null ? scope.getScopeName() : null;
    }

    /** For XML-based config */
    public void setScopeName(String scopeName)
    {
        this.scope = PropertyScope.get(scopeName);
    }
}
