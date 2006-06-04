/**
 * <code>DomXmlTransformersTestCase</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 */
package org.mule.test.transformers;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.xml.DomDocumentToXml;
import org.mule.transformers.xml.XmlToDomDocument;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.FileUtils;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 611 $
 */
public class DomXmlTransformersTestCase extends AbstractTransformerTestCase
{

    private String srcData;
    private Document resultData;

    protected void doSetUp() throws Exception
    {
        srcData = FileUtils.loadResourceAsString("cdcatalog.xml", getClass());
        org.dom4j.Document dom4jDoc = DocumentHelper.parseText(srcData);
        resultData = new DOMWriter().write(dom4jDoc);
    }

    public UMOTransformer getTransformer() throws Exception
    {
        return new XmlToDomDocument();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new DomDocumentToXml();
    }

    public Object getTestData()
    {
        return srcData;
    }

    public Object getResultData()
    {
        return resultData;
    }

    public boolean compareResults(Object src, Object result)
    {
        if (src instanceof Document) {
            String srcXml = new DOMReader().read((Document) src).asXML();
            String resultXml = new DOMReader().read((Document) result).asXML();
            return srcXml.equals(resultXml);
        } else if (src != null) {
            src = ((String) src).replaceAll("\r", "");
            src = ((String) src).replaceAll("\t", "");
            src = ((String) src).replaceAll("\n", "");
            result = ((String) result).replaceAll("\r", "");
            result = ((String) result).replaceAll("\t", "");
            result = ((String) result).replaceAll("\n", "");
        }
        return super.compareResults(src, result);
    }
}
