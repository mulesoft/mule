/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.xml.sax.InputSource;

/**
 * <code>IsXmlFilter</code> accepts a String or byte[] if its contents are valid XML.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class IsXmlFilter implements UMOFilter {

    public IsXmlFilter() {
        super();
    }

    public boolean accept(UMOMessage obj) {
        return accept(obj.getPayload());
    }
    
    private boolean accept(Object obj) {
        try {
            if (obj instanceof String) {
                DocumentHelper.parseText((String) obj);
            } else if (obj instanceof byte[]) {
                new SAXReader().read(new InputSource(new ByteArrayInputStream((byte[]) obj)));
            } else {
                throw new DocumentException("Object must be a string or byte array");
            }
            log.debug("Filter result = true (message is valid XML)");
            return true;
        } catch (DocumentException e) {
            log.debug("Filter result = false (message is not valid XML): " + e.getMessage());
            return false;
        }
    }

    private static Log log = LogFactory.getLog(IsXmlFilter.class);
}
