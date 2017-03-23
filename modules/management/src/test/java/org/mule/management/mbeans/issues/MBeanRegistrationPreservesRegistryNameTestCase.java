/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.mbeans.issues;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.construct.Flow;
import org.mule.module.management.agent.JmxApplicationAgent;
import org.mule.module.management.support.JmxSupport;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import javax.management.MBeanServer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;


/**
 * Verify that the MBeans registration preserves original registry values
 */
public class MBeanRegistrationPreservesRegistryNameTestCase extends AbstractServiceAndFlowTestCase
{

    private static final String FLOW_PREFIX = "SimpleBridge";

    private static final String FLOW_SUFFIX_NAME = "flowSuffix";

    private static final String FLOW_SUFFIX_VALUE = "flow1";

    private static final String FLOW_ORIGINAL_REGISTRY_KEY = FLOW_PREFIX + "-${" + FLOW_SUFFIX_NAME + "}"; 

    private static final String FLOW_NAME = FLOW_PREFIX + "-" + FLOW_SUFFIX_VALUE; 

    private MBeanServer mBeanServer;
    private String domainName;
    private JmxSupport jmxSupport;

    @Rule
    public SystemProperty flowSuffix = new SystemProperty(FLOW_SUFFIX_NAME, FLOW_SUFFIX_VALUE);

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{ConfigVariant.FLOW, "issues/mbeans-registration-preserves-registry-name.xml"},});
    }


    public MBeanRegistrationPreservesRegistryNameTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        JmxApplicationAgent jmxAgent = (JmxApplicationAgent) muleContext.getRegistry().lookupAgent("jmx-agent");
        jmxSupport = jmxAgent.getJmxSupportFactory().getJmxSupport();
        domainName = jmxSupport.getDomainName(muleContext);
        mBeanServer = jmxAgent.getMBeanServer();
    }


    @Test
    public void whenAnMBeanIsRegisteredThenNameIsOriginalRegistryName() throws Exception
    {
        Flow flow = muleContext.getRegistry().get(FLOW_ORIGINAL_REGISTRY_KEY);
        assertThat(flow.getName(), equalTo(FLOW_NAME));
    }

}
