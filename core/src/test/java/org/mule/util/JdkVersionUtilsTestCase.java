/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.JdkVersionUtils.JdkVersion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JdkVersionUtilsTestCase extends AbstractMuleTestCase
{
	
	private String originalJavaVersion;
	
	@Before
	public void before()
	{
		originalJavaVersion = System.getProperty("java.version");
	}
	
	@After
	public void after() {
		setJdkVersion(originalJavaVersion);
	}
	
	private static void setJdkVersion(String version)
	{
		System.setProperty("java.version", version);
	}

	@Test
	public void testIsSupportedJdkVersion()
	{
		// supported
		assertTrue(JdkVersionUtils.isSupportedJdkVersion());
		setJdkVersion("1.6");
		assertTrue(JdkVersionUtils.isSupportedJdkVersion());
		setJdkVersion("1.7");
		assertTrue(JdkVersionUtils.isSupportedJdkVersion());

		//not supported
		setJdkVersion("1.8");
		assertFalse(JdkVersionUtils.isSupportedJdkVersion());
		setJdkVersion("1.4.2");
		assertFalse(JdkVersionUtils.isSupportedJdkVersion());
		setJdkVersion("1.4.2_12");
		assertFalse(JdkVersionUtils.isSupportedJdkVersion());
	}
	
	@Test
	public void testSupportedJdkVendor()
	{
		assertTrue(JdkVersionUtils.isSupportedJdkVendor());
	}
	
	@Test
	public void testRecommendedJdkVersion()
	{
		assertTrue(JdkVersionUtils.isRecommendedJdkVersion());
		// recommended
		assertTrue(JdkVersionUtils.isRecommendedJdkVersion());
		setJdkVersion("1.6");
		assertTrue(JdkVersionUtils.isRecommendedJdkVersion());
		setJdkVersion("1.7");
		assertTrue(JdkVersionUtils.isRecommendedJdkVersion());

		//not recommended
		setJdkVersion("1.8");
		assertFalse(JdkVersionUtils.isRecommendedJdkVersion());
		setJdkVersion("1.4.2");
		assertFalse(JdkVersionUtils.isRecommendedJdkVersion());
		setJdkVersion("1.6.0_5");
		assertFalse(JdkVersionUtils.isRecommendedJdkVersion());
	}
	
	@Override
	public int getTestTimeoutSecs() {
		return 999999;
	}
	
	@Test
	public void testJdkVersion()
	{
		JdkVersion jdkVersion = new JdkVersion("1.7");
		assertEquals(new Integer(1), jdkVersion.getMajor());
		assertEquals(new Integer(7), jdkVersion.getMinor());
		assertNull(jdkVersion.getMicro());
		assertNull(jdkVersion.getUpdate());
		assertNull(jdkVersion.getMilestone());
		
		jdkVersion = new JdkVersion("1.7.0-ea");
		assertEquals(new Integer(1), jdkVersion.getMajor());
		assertEquals(new Integer(7), jdkVersion.getMinor());
		assertEquals(new Integer(0), jdkVersion.getMicro());
		assertNull(jdkVersion.getUpdate());
		assertEquals("ea", jdkVersion.getMilestone());

		jdkVersion = new JdkVersion("1.6.0_29-b05");
		assertEquals(new Integer(1), jdkVersion.getMajor());
		assertEquals(new Integer(6), jdkVersion.getMinor());
		assertEquals(new Integer(0), jdkVersion.getMicro());
		assertEquals(new Integer(29), jdkVersion.getUpdate());
		assertEquals("b05", jdkVersion.getMilestone());
	}
	
	@Test
	public void testJdkVersionComparison()
	{
		JdkVersion jdk1_3 = new JdkVersion("1.3");
		JdkVersion jdk1_6_0_5 = new JdkVersion("1.6.0_5");
		JdkVersion jdk1_7 = new JdkVersion("1.7");
		JdkVersion jdk1_6_0_29_b04 = new JdkVersion("1.6.0_29-b04");
		JdkVersion jdk1_6_0_29_b05 = new JdkVersion("1.6.0_29-b05");
		
		assertTrue(jdk1_3.compareTo(jdk1_7) < 0);
		assertTrue(jdk1_7.compareTo(jdk1_3) > 0);
		assertTrue(jdk1_3.compareTo(jdk1_3) == 0);
		assertTrue(jdk1_6_0_29_b05.compareTo(jdk1_6_0_29_b05) == 0);
		
		assertTrue(jdk1_6_0_5.compareTo(jdk1_6_0_29_b04) < 0);
		assertTrue(jdk1_6_0_29_b04.compareTo(jdk1_6_0_5) > 0);
		
		assertTrue(jdk1_6_0_29_b04.compareTo(jdk1_6_0_29_b05) < 0);
		assertTrue(jdk1_6_0_29_b05.compareTo(jdk1_6_0_29_b04) > 0);
		
		assertTrue(jdk1_6_0_29_b04.compareTo(jdk1_7) < 0);
		assertTrue(jdk1_7.compareTo(jdk1_6_0_29_b04) > 0);
	}
}
