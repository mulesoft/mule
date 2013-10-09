/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.codec;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

public class XMLEntityTransformersTestCase extends AbstractTransformerTestCase
{

    public Object getResultData()
    {
        return "&lt;?xml version=&quot;1.0&quot; encoding=&quot;utf-8&quot;?&gt;&lt;string xmlns=&quot;http://www.webserviceX.NET/&quot;&gt;&lt;StockQuotes&gt;&lt;Stock&gt;&lt;Symbol&gt;IBM&lt;/Symbol&gt;&lt;Last&gt;91.52&lt;/Last&gt;&lt;Date&gt;11/3/2006&lt;/Date&gt;&lt;Time&gt;11:10am&lt;/Time&gt;&lt;Change&gt;-0.16&lt;/Change&gt;&lt;Open&gt;91.76&lt;/Open&gt;&lt;High&gt;92.34&lt;/High&gt;&lt;Low&gt;91.43&lt;/Low&gt;&lt;Volume&gt;1856600&lt;/Volume&gt;&lt;MktCap&gt;139.3B&lt;/MktCap&gt;&lt;PreviousClose&gt;91.68&lt;/PreviousClose&gt;&lt;PercentageChange&gt;-0.17%&lt;/PercentageChange&gt;&lt;AnnRange&gt;72.73 - 92.68&lt;/AnnRange&gt;&lt;Earns&gt;5.815&lt;/Earns&gt;&lt;P-E&gt;15.77&lt;/P-E&gt;&lt;Name&gt;INTL BUSINESS MAC&lt;/Name&gt;&lt;/Stock&gt;&lt;/StockQuotes&gt;&lt;/string&gt;";
    }

    public Object getTestData()
    {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><string xmlns=\"http://www.webserviceX.NET/\"><StockQuotes><Stock><Symbol>IBM</Symbol><Last>91.52</Last><Date>11/3/2006</Date><Time>11:10am</Time><Change>-0.16</Change><Open>91.76</Open><High>92.34</High><Low>91.43</Low><Volume>1856600</Volume><MktCap>139.3B</MktCap><PreviousClose>91.68</PreviousClose><PercentageChange>-0.17%</PercentageChange><AnnRange>72.73 - 92.68</AnnRange><Earns>5.815</Earns><P-E>15.77</P-E><Name>INTL BUSINESS MAC</Name></Stock></StockQuotes></string>";
    }

    public Transformer getTransformer()
    {
        return new XmlEntityEncoder();
    }

    public Transformer getRoundTripTransformer()
    {
        return new XmlEntityDecoder();
    }

}
