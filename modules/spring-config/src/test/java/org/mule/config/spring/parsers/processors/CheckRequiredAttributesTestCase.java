/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;

public class CheckRequiredAttributesTestCase extends AbstractPreProcessorTestCase
{

    @Test
    public void testSingleSetSingleAttribute() throws ParserConfigurationException
    {
        String[][] groups = new String[][] {
            new String[]{ "a1" }
        };
        String text = "must have all attributes for one of the sets: [a1]";
        
        // no attributes
        assertBad(groups, "", text);
        
        // optional attribute
        assertBad(groups, "x", text);
        
        // required attribute
        assertOk(groups, "a1");
        assertOk(groups, "a1 x");
        assertOk(groups, "x a1");
    }

    @Test
    public void testSingleSetMultipleAttributes() throws ParserConfigurationException
    {
        String[][] groups = new String[][] {
            new String[] { "b1", "b2" }
        };
        String text = "must have all attributes for one of the sets: [b1, b2]";

        // no attributes
        assertBad(groups, "", text);

        // optional attribute
        assertBad(groups, "x", text);
        
        // one of the required attributes
        assertBad(groups, "b1", text);
        assertBad(groups, "b2", text);
        
        // one of the required attributes and an optional attribute
        assertBad(groups, "x b1", text);
        assertBad(groups, "x b2", text);

        // both required attributes
        assertOk(groups, "b1 b2");
        
        // both required attributes and an optional attribute
        assertOk(groups, "x b1 b2");
    }

    @Test
    public void testTwoSetsSingleAttribute() throws ParserConfigurationException
    {
        String[][] groups = new String[][] {
            new String[] { "a1" }, 
            new String[] { "b1" }
        };
        String text = "must have all attributes for one of the sets: [a1] [b1]";
        
        // empty set
        assertBad(groups, "", text);
        
        // only optional attributes
        assertBad(groups, "x", text);
        assertBad(groups, "x y", text);
        
        // one attribute from a required set
        assertOk(groups, "a1");
        assertOk(groups, "b1");
        
        // one attribute from a required set and optional attributes
        assertOk(groups, "a1 x");
        assertOk(groups, "x a1");
        assertOk(groups, "b1 x");
        assertOk(groups, "x b1");

        // attributes from both sets, assuming this is OK, too as both groups are fully satisfied
        assertOk(groups, "a1 b1");
    }
    
    @Test
    public void testTwoSetsEmptySecondSet() throws ParserConfigurationException
    {
        String[][] groups = new String[][] {
            new String[] { "a1" },
            new String[] {}
        };
        String text = "must have all attributes for one of the sets: [a1]";
        
        // no attributes
        assertBad(groups, "", text);
        
        // only optional attributes
        assertBad(groups, "x", text);
        assertBad(groups, "x b1", text);

        // required attribute
        assertOk(groups, "a1");
        assertOk(groups, "a1 x");
        assertOk(groups, "x a1");
    }
    
    @Test
    public void testTwoSetsMultipleAttributes() throws ParserConfigurationException
    {
        String[][] groups = new String[][] {
            new String[] { "a1", "a2" },
            new String[] { "b1", "b2" }
        };
        String text = "must have all attributes for one of the sets: [a1, a2] [b1, b2]";
        
        // no attributes
        assertBad(groups, "", text);
        
        // only optional attributes
        assertBad(groups, "x", text);

        // only one attribute from the required set
        assertBad(groups, "a1", text);
        assertBad(groups, "a2", text);
        assertBad(groups, "b1", text);
        assertBad(groups, "b2", text);
        assertBad(groups, "a1 x", text);
        assertBad(groups, "a2 x", text);
        assertBad(groups, "b1 x", text);
        assertBad(groups, "b2 x", text);
        assertBad(groups, "x a1", text);
        assertBad(groups, "x a2", text);
        assertBad(groups, "x b1", text);
        assertBad(groups, "x b2", text);
        assertBad(groups, "a1 b1", text);
        
        // attributes from one required set
        assertOk(groups, "a1 a2");
        assertOk(groups, "x a1 a2");
        assertOk(groups, "a1 x a2");
        assertOk(groups, "a1 a2 x");
        assertOk(groups, "b1 b2");
        assertOk(groups, "x b1 b2");
        assertOk(groups, "b1 x b2");
        assertOk(groups, "b1 b2 x");
        
        // attributes from both required sets
        assertOk(groups, "a1 a2 b1");
        assertOk(groups, "x a1 a2 b1");
        assertOk(groups, "a1 x a2 b1");
        assertOk(groups, "a1 a2 x b1");
        assertOk(groups, "a1 a2 b1 x");
        assertOk(groups, "a1 a2 b2");
        assertOk(groups, "x a1 a2 b2");
        assertOk(groups, "a1 x a2 b2");
        assertOk(groups, "a1 a2 x b2");
        assertOk(groups, "a1 a2 b2 x");
        assertOk(groups, "b1 b2 a1");
        assertOk(groups, "x b1 b2 a1");
        assertOk(groups, "b1 x b2 a1");
        assertOk(groups, "b1 b2 x a1");
        assertOk(groups, "b1 b2 a1 x");
        assertOk(groups, "b1 b2 a2");
        assertOk(groups, "x b1 b2 a2");
        assertOk(groups, "b1 x b2 a2");
        assertOk(groups, "b1 b2 x a2");
        assertOk(groups, "b1 b2 a2 x");
    }

    @Test
    public void testTwoSetsOverlappingAttributes() throws ParserConfigurationException
    {
        String[][] groups = new String[][] {
            new String[] { "a1", "a2" },
            new String[] { "a1", "b1" }
        };
        String text = "must have all attributes for one of the sets: [a1, a2] [a1, b1]";
        
        // no attributes
        assertBad(groups, "", text);
        
        // only optional attributes
        assertBad(groups, "x", text);

        // attributes from first group
        assertOk(groups, "a1 a2");
        assertOk(groups, "x a1 a2");
        assertOk(groups, "a1 x a2");
        assertOk(groups, "a1 a2 x");
        
        // attributes from second group
        assertOk(groups, "a1 b1");
        assertOk(groups, "x a1 b1");
        assertOk(groups, "a1 x b1");
        assertOk(groups, "a1 b1 x");
        
        // attributes from both groups
        assertOk(groups, "a1 a2 b1");
    }
    
    @Test
    public void testRealWorld() throws ParserConfigurationException
    {
        String[][] groups = new String[][] {
            new String[] { "address" },
            new String[] { "ref" },
            new String[] { "type", "from" },
            new String[] { "type", "recipient" }
        };
        
        assertOk(groups, "from id name type");
    }

    @Override
    protected PreProcessor createCheck(String[][] constraint, String elementName, String elementNamespaceUrl)
    {
        return new CheckRequiredAttributes(constraint);
    }
}
