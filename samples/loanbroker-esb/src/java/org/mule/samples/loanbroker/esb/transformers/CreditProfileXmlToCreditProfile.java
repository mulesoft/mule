package org.mule.samples.loanbroker.esb.transformers;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.mule.samples.loanbroker.esb.message.CreditProfile;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * Extracts the customer credit profile info from Xml returned from the CreditAgency EJB App.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CreditProfileXmlToCreditProfile extends AbstractTransformer {
    public CreditProfileXmlToCreditProfile() {
        registerSourceType(String.class);
        registerSourceType(Document.class);
        setReturnClass(CreditProfile.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException {
        Document doc = null;
        if(src instanceof Document) {
            doc = (Document)src;
        } else {
            try {
                doc = DocumentHelper.parseText(src.toString());
            } catch (DocumentException e) {
                throw new TransformerException(this, e);
            }
        }
        String history = doc.valueOf("/credit-profile/customer-history");
        String score = doc.valueOf("/credit-profile/credit-score");
        CreditProfile cp = new CreditProfile();
        cp.setCreditHistory(Integer.valueOf(history).intValue());
        cp.setCreditScore(Integer.valueOf(score).intValue());
        return cp;
    }
}
