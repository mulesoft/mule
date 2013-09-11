/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * A wrapper for the {@link org.codehaus.jackson.JsonNode} object that
 * allows for nested object keys i.e. user/name will return the name property on
 * the user object.
 * <p/>
 * There is no 'xpath' for JSON yet (though I expect Jackson to do implement this at some point).  
 * This class provides a simple way to navigate a Json data structure. To select a child entry use -
 * <code>
 * person/name
 * </code>
 * <p/>
 * to access array data, use square braces with an index value i.e.
 * <code>
 * person/addresses[0]/postcode
 * <p/>
 * or
 * <p/>
 * [0]/arrayElement
 * </code>
 * <p/>
 * Also, multi-dimensional arrays can be accessed using:
 * <code>
 * filters[1]/init[1][0]
 * </code>
 * <p/>
 * if a Json property name contains a '/' the name needs to be quoted with single quotes i.e.
 * <p/>
 * <code>
 * results/'http://foo.com'/value
 * </code>
 */
public class JsonData implements Serializable
{
    private JsonNode node;

    public JsonData(JsonNode node)
    {
        this.node = node;
    }

    public JsonData(URL node) throws IOException
    {
        this(node.openStream());
    }

    public JsonData(InputStream node) throws IOException
    {
        this.node = new ObjectMapper().readTree(node);
    }

    public JsonData(Reader node) throws IOException
    {
        this.node = new ObjectMapper().readTree(node);
    }

    public JsonData(String node) throws IOException
    {
        this(new StringReader(node));
    }

    public JsonNode get(int index)
    {
        return node.get(index);
    }

    public boolean isArray()
    {
        return node.isArray();
    }
    
    public JsonNode[] toArray() 
    {
        List<JsonNode> children = new ArrayList<JsonNode>();
        for (Iterator<JsonNode> itr = node.getElements(); itr.hasNext();) 
        {
            children.add(itr.next());
        }
        return children.toArray(new JsonNode[children.size()]);
    }

    public JsonNode get(String expression)
    {
        List<String> tokens = parseTokens(expression);

        JsonNode o = node;
        for (String token : tokens)
        {
            if (token.startsWith("["))
            {
                if (o.isArray())
                {
                    int index = Integer.valueOf(token.substring(1, token.length() - 1));
                    o = o.path(index);
                }
                else
                {
                    throw new IllegalArgumentException("Current node is not an array, but expression is expecting one");
                }
            }
            else
            {
                o = o.path(token);
            }

            if (o.isMissingNode())
            {
                throw new IllegalArgumentException("Not a valid element: " + token);
            }
        }

        return o;
    }
    
    public String getAsString(String expression)
    {
        JsonNode node = get(expression);
        if (node.isValueNode())
        {
            return node.asText();
        }
        else
        {
            return node.toString();
        }
    }
    
    public boolean hasNode(String key)
    {
        JsonNode result = node.path(key);
        return result.isMissingNode() == false;
    }

    protected List<String> parseTokens(String expresion)
    {
        List<String> tokens = new ArrayList<String>();
        while (expresion.length() > 0)
        {
            int slash = expresion.indexOf("/");
            int brace = expresion.indexOf("[");
            //Handled quoted strings
            if (expresion.charAt(0) == '\'')
            {
                if (expresion.endsWith("'"))
                {
                    tokens.add(expresion.substring(1, expresion.length() - 1));
                    expresion = "";
                }
                else
                {
                    int x = expresion.indexOf("'/");
                    tokens.add(expresion.substring(1, x));
                    expresion = expresion.substring(x + 2);
                }
            }
            else if (slash == -1 && brace == -1)
            {
                tokens.add(expresion);
                expresion = "";
            }
            else if ((slash > -1 && slash < brace) || brace == -1)
            {
                if (slash == 0)
                {
                    if (expresion.charAt(1) == '\'')
                    {
                        int x = expresion.indexOf("'", 2);
                        tokens.add(expresion.substring(2, x));
                        expresion = expresion.substring(x + 1);
                    }
                    else if (expresion.charAt(1) == '[')
                    {
                        int i = expresion.indexOf("]", 1);
                        tokens.add(expresion.substring(0, i));
                        expresion = expresion.substring(i + 1);
                    }
                    else
                    {
                        expresion = expresion.substring(slash + 1);
                    }
                }
                else
                {
                    tokens.add(expresion.substring(0, slash));
                    expresion = expresion.substring(slash + 1);
                }
            }
            else if (brace > 0)
            {
                tokens.add(expresion.substring(0, brace));
                expresion = expresion.substring(brace);

            }
            else
            {
                int i = expresion.indexOf("]", brace);
                tokens.add(expresion.substring(0, i + 1));
                expresion = expresion.substring(i + 1);
            }
        }
        return tokens;
    }

    @Override
    public boolean equals(Object obj)
    {
        return node.equals(obj);
    }
    
    @Override
    public int hashCode()
    {
        return node.hashCode();
    }

    @Override
    public String toString()
    {
        return node.toString();
    }
}
