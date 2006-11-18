/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers.codec;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.codec.XmlEntityDecoder;
import org.mule.transformers.codec.XmlEntityEncoder;
import org.mule.umo.transformer.UMOTransformer;

public class XMLEntityTransformersTestCase extends AbstractTransformerTestCase
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getResultData()
     */
    public Object getResultData()
    {
        return "&lt;?xml version=&quot;1.0&quot; encoding=&quot;utf-8&quot;?&gt;&lt;string xmlns=&quot;http://www.webserviceX.NET/&quot;&gt;&lt;StockQuotes&gt;&lt;Stock&gt;&lt;Symbol&gt;IBM&lt;/Symbol&gt;&lt;Last&gt;91.52&lt;/Last&gt;&lt;Date&gt;11/3/2006&lt;/Date&gt;&lt;Time&gt;11:10am&lt;/Time&gt;&lt;Change&gt;-0.16&lt;/Change&gt;&lt;Open&gt;91.76&lt;/Open&gt;&lt;High&gt;92.34&lt;/High&gt;&lt;Low&gt;91.43&lt;/Low&gt;&lt;Volume&gt;1856600&lt;/Volume&gt;&lt;MktCap&gt;139.3B&lt;/MktCap&gt;&lt;PreviousClose&gt;91.68&lt;/PreviousClose&gt;&lt;PercentageChange&gt;-0.17%&lt;/PercentageChange&gt;&lt;AnnRange&gt;72.73 - 92.68&lt;/AnnRange&gt;&lt;Earns&gt;5.815&lt;/Earns&gt;&lt;P-E&gt;15.77&lt;/P-E&gt;&lt;Name&gt;INTL BUSINESS MAC&lt;/Name&gt;&lt;/Stock&gt;&lt;/StockQuotes&gt;&lt;/string&gt;";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTestData()
     */
    public Object getTestData()
    {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><string xmlns=\"http://www.webserviceX.NET/\"><StockQuotes><Stock><Symbol>IBM</Symbol><Last>91.52</Last><Date>11/3/2006</Date><Time>11:10am</Time><Change>-0.16</Change><Open>91.76</Open><High>92.34</High><Low>91.43</Low><Volume>1856600</Volume><MktCap>139.3B</MktCap><PreviousClose>91.68</PreviousClose><PercentageChange>-0.17%</PercentageChange><AnnRange>72.73 - 92.68</AnnRange><Earns>5.815</Earns><P-E>15.77</P-E><Name>INTL BUSINESS MAC</Name></Stock></StockQuotes></string>";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTransformers()
     */
    public UMOTransformer getTransformer()
    {
        return new XmlEntityEncoder();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getRoundTripTransformer()
     */
    public UMOTransformer getRoundTripTransformer()
    {
        return new XmlEntityDecoder();
    }

}
