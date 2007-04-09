/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.csv;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.csv.CSVToMaps;
import org.mule.transformers.csv.MapsToCSV;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.IOUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CSVTransformersTestCase extends AbstractTransformerTestCase
{
    private String srcData;
    private String resultData;
    private List labels;

    public CSVTransformersTestCase()
    {
        super();
        labels = new ArrayList();
        labels.add(0, "Name");
        labels.add(1, "Street");
        labels.add(2, "City");
        labels.add(3, "Country");
        labels.add(4, "Email");
    }

    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.getResourceAsString("addresses-sloppy.csv", getClass());
        resultData = IOUtils.getResourceAsString("addresses.csv", getClass());
    }

    public void testTransform() throws Exception
    {
        Object result = this.getTransformer().transform(getTestData());
        assertNotNull(result);
        assertTrue(result instanceof List);

        List list = (List)result;
        Iterator iter = list.iterator();

        // Make sure we read 4 rows
        assertEquals(list.size(), 4);

        // Make sure all Maps have 5 columns
        while (iter.hasNext())
        {
            Map map = (Map)iter.next();
            assertEquals(map.size(), 5);
        }
    }

    /**
     * Note: testing with input data that has quote won't work because OpenCSV will
     * escape them with another quote. You can't turn this off (at least in this
     * version), so the last assert will always fail.
     */
    public void testRoundtripTransform() throws Exception
    {
        Object outbound = this.getTransformer().transform(getTestData());
        assertNotNull(outbound);
        Object inbound = this.getRoundTripTransformer().transform(outbound);
        assertNotNull(inbound);
        assert (compareResults(inbound, getResultData()));
    }

    public UMOTransformer getTransformer() throws Exception
    {
        CSVToMaps t = new CSVToMaps();
        t.setFieldNames(labels);
        return t;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        MapsToCSV t = new MapsToCSV();
        t.setQuoteCharacter('\u0000');
        t.setFieldNames(labels);
        return t;
    }

    public Object getTestData()
    {
        return srcData;
    }

    public Object getResultData()
    {
        return resultData;
    }

}
