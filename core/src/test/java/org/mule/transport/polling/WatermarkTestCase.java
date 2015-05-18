/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mule.transport.polling.watermark.selector.MaxValueWatermarkSelector;
import org.mule.transport.polling.watermark.selector.MinValueWatermarkSelector;

public class WatermarkTestCase {
	
	@Test
	public void testMaxValueSelectedInWatermark() {
		MaxValueWatermarkSelector selector = new MaxValueWatermarkSelector();
		
		String last = "";
		for (int i = 0; i < 10; i++) {
			selector.acceptValue(Integer.toString(i));
			last = Integer.toString(i);
		}
		assertEquals(last, selector.getSelectedValue());
	}

	@Test
	public void testMinValueSelectedInWatermark() {
		MinValueWatermarkSelector selector = new MinValueWatermarkSelector();
		List<String> strings = new ArrayList<String>();
		
		for (int i = 0; i < 10; i++) {
			strings.add(Integer.toString(i));
			selector.acceptValue(Integer.toString(i));
		}
		assertEquals(strings.get(0), selector.getSelectedValue());
	}

}
