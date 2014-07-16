/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;

import org.junit.Test;

public class CheckExclusiveAttributesTestCase extends AbstractPreProcessorTestCase
{
    @Test
    public void testDisjointSingleAttributeGroups() throws Exception
    {
        String[][] groups = new String[][] {
            new String[] { "a" }, 
            new String[] { "b" }
        };
     
        assertOk(groups, "a");
        assertOk(groups, "b");
        assertOk(groups, "x");
    }
    
    @Test
    public void testDisjointMultipleAttributes() throws Exception
    {
        String[][] groups = new String[][] {
            new String[] { "a1" }, 
            new String[] { "b1", "b2" }
        };
        String text = "do not match the exclusive groups";
        
        assertOk(groups, "");
        // optional attribute
        assertOk(groups, "x");
        // all attributes from first group
        assertOk(groups, "a1");
        // attribute from second group        
        assertOk(groups, "b1");
        assertOk(groups, "b2");
        // attribute from first group and optional attribute
        assertOk(groups, "a1 x");
        // attribute from second group and optional attribute
        assertOk(groups, "x b1");
        assertOk(groups, "x b2");
        // all attributes from second group
        assertOk(groups, "b1 b2");
        
        assertBad(groups, "a1 b1", text);
        assertBad(groups, "b1 a1", text);
        assertBad(groups, "a1 b2", text);
        assertBad(groups, "b2 a1", text);
        assertBad(groups, "a1 b1 b2", text);
        assertBad(groups, "a1 b2 x", text);
    }

    @Test
    public void testSecondGroupEmpty() throws Exception
    {
        String[][] groups = new String[][]{
            new String[] { "a1" },
            new String[] {}
        };
        
        assertOk(groups, "");
        // optional attribute
        assertOk(groups, "x");
        // only attribute from first group
        assertOk(groups, "a1");
        // attribute from first group plus optional attribute
        assertOk(groups, "a1 x");
    }
    
    @Test
    public void testGroupsWithOverlappingAttributes() throws Exception
    {
        String[][] groups = new String[][] {
            new String[] { "a1", "b1" },
            new String[] { "a1", "b2" }
        };
        
        // attribute from first group (can be in either group)
        assertBad(groups, "a1", "do not satisfy");
        // attribute from first group
        assertOk(groups, "b1");
        // attribute from second group
        assertOk(groups, "b2");
        // optional attribute
        assertOk(groups, "a1 b1 x");
        assertOk(groups, "a1 b2 x");
        // complete first group
        assertOk(groups, "a1 b1");
        // complete second group
        assertOk(groups, "a1 b2");
    }
    
    @Test
    public void testRealWorld() throws Exception
    {
        String[][] groups = new String[][] {
            new String[] { "type", "recipient" },
            new String[] { "type", "from" }
        };
        
        assertOk(groups, "id name recipient subject type");
    }
    
    @Override
    protected PreProcessor createCheck(String[][] constraint)
    {
        return new CheckExclusiveAttributes(constraint);
    }
}
