/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.json.util.JsonUtils;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.NullPayload;

import java.io.UnsupportedEncodingException;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a JSON encoded object representation in to an XML representation. Each property and enclosing
 * element are wrapped in XML elements
 * these generated element names can be set on the transformer. Names can be configured for Object
 * elements, array elements and value elements.
 * <p/>
 * The returnClass for this transformer is always java.lang.String, there is no need to set this.
 */
public class JsonToXml extends AbstractTransformer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToXml.class);

    public static final String ELEMENT_NAME_OBJECT = "o";

    public static final String ELEMENT_NAME_ARRAY = "a";

    public static final String ELEMENT_NAME_VALUE = "e";

    public static final String ELEMENT_NAME_ARRAY_ELEMENT = ELEMENT_NAME_VALUE;

    protected static final String DEFAULT_ENCODING = "UTF-8";

    protected static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"${encoding}\"?>";


    protected String objectElementName = ELEMENT_NAME_OBJECT;
    protected String arrayElementName = ELEMENT_NAME_ARRAY;
    protected String valueElementName = ELEMENT_NAME_VALUE;

    public JsonToXml()
    {
        this.registerSourceType(JSON.class);
        this.registerSourceType(String.class);
        this.registerSourceType(byte[].class);
        this.registerSourceType(Object.class);
        this.setReturnClass(String.class);
    }

    public String getObjectElementName()
    {
        return objectElementName;
    }

    public void setObjectElementName(String objectElementName)
    {
        this.objectElementName = objectElementName;
    }

    public String getArrayElementName()
    {
        return arrayElementName;
    }

    public void setArrayElementName(String arrayElementName)
    {
        this.arrayElementName = arrayElementName;
    }

    public String getValueElementName()
    {
        return valueElementName;
    }

    public void setValueElementName(String valueElementName)
    {
        this.valueElementName = valueElementName;
    }

    public boolean isAcceptMuleMessage()
    {
        return this.sourceTypes.contains(MuleMessage.class);
    }

    public void setAcceptMuleMessage(boolean value)
    {
        if (value)
        {
            this.registerSourceType(MuleMessage.class);
        }
        else
        {
            this.unregisterSourceType(MuleMessage.class);
        }
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {

        String returnValue = null;
        JSON json = null;

        if (src == null || src instanceof NullPayload)
        {
            return this.getStringOfEncoding(this.getXmlDeclaration(encoding) + "\n" + this.getXmlOfNull(),
                    encoding);
        }

        // Not String, not JSON, can't be actually
        if (!(src instanceof JSON) && !(src instanceof String))
        {
            src = src.toString();
        }

        // String
        if (src instanceof String)
        {
            String jsonString = (String) src;

            if (JsonUtils.isBoolean(jsonString) || JsonUtils.isString(jsonString)
                    || JsonUtils.isNumber(jsonString))
            {
                return this.getStringOfEncoding(this.getXmlDeclaration(encoding) + "\n"
                        + this.getXmlOfCommonValue(jsonString), encoding);
            }
            else if (JsonUtils.isArray(jsonString))
            {
                json = JSONArray.fromObject(jsonString);
            }
            else if (JsonUtils.isObject(jsonString))
            {
                json = JSONObject.fromObject(jsonString);
            }
            else
            {
                throw new IllegalArgumentException("Could not recognize type of '" + jsonString + "'");
            }
        }

        // JSON
        if (json != null || src instanceof JSON)
        {
            if (json == null)
            {
                json = (JSON) src;
            }
            returnValue = this.getXmlOfJson(json, encoding);
        }

        return returnValue;
    }

    protected String getStringOfEncoding(String string, String encoding)
    {

        if (encoding == null)
        {
            encoding = DEFAULT_ENCODING;
        }

        String returnValue;

        try
        {
            returnValue = new String(string.getBytes(encoding));
        }
        catch (UnsupportedEncodingException uee)
        {
            LOGGER.warn("Unsupported encoding specified '" + encoding
                    + "', could not handle and thus returning original string");
            returnValue = string;
        }

        return returnValue;
    }

    protected String getXmlDeclaration(String encoding)
    {
        if (encoding == null)
        {
            encoding = DEFAULT_ENCODING;
        }
        String returnValue = XML_DECLARATION;
        returnValue = StringUtils.replace(returnValue, "${encoding}", encoding);

        return returnValue;
    }

    protected String getXmlOfNull()
    {
        return "<" + getValueElementName() + "/>";
    }

    protected String getXmlOfCommonValue(String value)
    {
        StringBuffer sb = new StringBuffer();

        sb.append("<" + getValueElementName() + " type=\"");
        if (JsonUtils.isString(value))
        {
            sb.append("string");
        }
        else if (JsonUtils.isBoolean(value))
        {
            sb.append("boolean");
        }
        else if (JsonUtils.isNumber(value))
        {
            sb.append("number");
        }
        else
        {
            sb.append("unknown");
        }
        sb.append("\">");
        sb.append(value);
        sb.append("</" + getValueElementName() + ">");

        return sb.toString();
    }

    protected String getXmlOfJson(JSON json, String encoding)
    {
        XMLSerializer xmlSerializer = this.newXmlSerializer();
        return xmlSerializer.write(json, encoding);
    }

    protected XMLSerializer newXmlSerializer()
    {

        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setObjectName(getObjectElementName());
        xmlSerializer.setArrayName(getArrayElementName());
        xmlSerializer.setElementName(getValueElementName());

        return xmlSerializer;
    }

}
