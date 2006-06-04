/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.tck.model;

import org.mule.impl.RequestContext;
import org.mule.model.NoSatisfiableMethodsException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.util.ClassUtils;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractEntryPointDiscoveryTestCase extends AbstractMuleTestCase
{

    /**
     * Tests entrypoint discovery when there is no discoverable method
     */
    public void testFailEntryPointDiscovery() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getTestDescriptor("badSatsuma", InvalidSatsuma.class.getName());

        UMOEntryPoint ep = null;
        try {
            ep = epd.resolveEntryPoint(descriptor);
        } catch (NoSatisfiableMethodsException e) {
            // expected
            return;
        }
        assertTrue(ep != null);
        try {

            RequestContext.setEvent(getTestEvent("Hello"));
            ep.invoke(new InvalidSatsuma(), RequestContext.getEventContext());
            fail("Should have failed to find entrypoint on Satsuma");

        } catch (Exception e) {
            // expected
        } finally {
            RequestContext.setEvent(null);
        }

    }

    /**
     * Tests entrypoint discovery on object that has it's own event handler
     * without implementing any of the Mule event interfaces
     */
    public void testEntryPointDiscovery() throws Exception
    {
        ComponentMethodMapping[] mappings = getComponentMappings();

        for (int i = 0; i < mappings.length; i++) {
            if (mappings[i].isShouldFail()) {
                doExpectedFail(mappings[i]);
            } else {
                doExpectedPass(mappings[i]);
            }
        }
    }

    private void doExpectedPass(ComponentMethodMapping mapping) throws Exception
    {
        UMOEntryPointResolver epr = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(mapping.getComponentClass().getName());
        UMOEntryPoint ep = epr.resolveEntryPoint(descriptor);
        assertNotNull(ep);
        // TODO
        // if(!(ep instanceof DynamicEntryPoint)) {
        // assertEquals(ep.getName(), mapping.getMethodName());
        // assertEquals(ep.getParameterType(), mapping.getMethodArgumentType());
        // }
    }

    private void doExpectedFail(ComponentMethodMapping mapping) throws Exception
    {
        UMOEntryPointResolver epr = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(mapping.getComponentClass().getName());

        try {
            UMOEntryPoint ep = epr.resolveEntryPoint(descriptor);
            ep.invoke(ClassUtils.instanciateClass(mapping.getComponentClass(), ClassUtils.NO_ARGS), getTestEventContext("blah"));
            fail("Resolving should have failed for: " + mapping.toString());
        } catch (Exception e) {
            // expected
        }

    }

    public UMODescriptor getDescriptorToResolve(String className) throws Exception
    {
        return getTestDescriptor("myComponent", className);
    }

    public abstract UMOEntryPointResolver getEntryPointResolver();

    /**
     * @return an array of the the different components that can be resolved by
     *         the resolver and the method name to be resolved on each component
     */
    public abstract ComponentMethodMapping[] getComponentMappings();

    /**
     * <p>
     * <code>ComponentMethodMapping</code> is used to supply a component class
     * and the correct method to be resovled on the component.
     * 
     * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
     * @version $Revision$
     */
    public class ComponentMethodMapping
    {
        private Class componentClass;
        private Class methodArgumentType;
        private String methodName;
        private boolean shouldFail;

        public ComponentMethodMapping(Class componentClass, String methodName, Class methodArgumentType)
        {
            this(componentClass, methodName, methodArgumentType, false);
        }

        public ComponentMethodMapping(Class componentClass,
                                      String methodName,
                                      Class methodArgumentType,
                                      boolean shouldFail)
        {
            this.componentClass = componentClass;
            this.methodName = methodName;
            this.methodArgumentType = methodArgumentType;
            this.shouldFail = shouldFail;
        }

        /**
         * @return Returns the componentClass.
         */
        public Class getComponentClass()
        {
            return componentClass;
        }

        /**
         * @return Returns the methodName.
         */
        public String getMethodName()
        {
            return methodName;
        }

        /**
         * @return Returns the methodName.
         */
        public Class getMethodArgumentType()
        {
            return methodArgumentType;
        }

        /**
         * @return Returns the shouldFail.
         */
        public boolean isShouldFail()
        {
            return shouldFail;
        }

        public String toString()
        {
            return componentClass.getName() + "." + methodName + "(" + methodArgumentType.getName()
                    + "), Expected to fail= " + shouldFail;
        }

    }
}
