/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.*;

import org.mule.common.Capability;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DefaultMuleArtifactTestCase extends AbstractMuleTestCase
{
    
    private interface MyCapability extends Capability{}
    
    private class BaseClassWithCapability implements MyCapability {}
    
    private class SubClassInheritingCapability extends BaseClassWithCapability {}
    
    private class BaseClassWithoutCapability {}
    
    private class SubClassImplementingCapability extends BaseClassWithoutCapability implements MyCapability {}

    @Test
    public void testCapabilityHierarchies()
    {
        DefaultMuleArtifact artifact = new DefaultMuleArtifact(new BaseClassWithCapability());
        assertTrue(artifact.hasCapability(MyCapability.class));
        assertNotNull(artifact.getCapability(MyCapability.class));
        
        artifact = new DefaultMuleArtifact(new SubClassInheritingCapability());
        assertTrue(artifact.hasCapability(MyCapability.class));
        assertNotNull(artifact.getCapability(MyCapability.class));
        
        artifact = new DefaultMuleArtifact(new BaseClassWithoutCapability());
        assertFalse(artifact.hasCapability(MyCapability.class));
        assertNull(artifact.getCapability(MyCapability.class));

        artifact = new DefaultMuleArtifact(new SubClassImplementingCapability());
        assertTrue(artifact.hasCapability(MyCapability.class));
        assertNotNull(artifact.getCapability(MyCapability.class));
    }

}


