package org.mule.providers.dq.transformers;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mule.providers.dq.DQMessage;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

/**
 * <code> XmlToDQMessage</code> Will convert an xml string to a DQMessage.
 * 
 * @author m999svm
 */
public class XmlToDQMessage extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6677408209819658768L;

    /**
     * The contructor
     * 
     */
    public XmlToDQMessage()
    {
        registerSourceType(String.class);
        setReturnClass(DQMessage.class);
    }

    /**
     * @see org.mule.transformers.AbstractTransformer#doTransform(Object, String)
     */
    public final Object doTransform(final Object src, String encoding) throws TransformerException
    {
        String xml = (String) src;
        DQMessage msg;

        try {

            Document document = DocumentHelper.parseText(xml);
            msg = new DQMessage();

            Element root = document.getRootElement();
            String name;
            String value;
            Element element;

            for (Iterator i = root.elementIterator(); i.hasNext();) {
                element = (Element) i.next();
                name = element.attributeValue(DQMessage.XML_NAME);
                value = element.getTextTrim();
                msg.addEntry(name, value);
            }

            return msg;

        } catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }

}
