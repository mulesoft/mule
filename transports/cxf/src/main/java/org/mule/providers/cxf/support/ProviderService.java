
package org.mule.providers.cxf.support;

import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider
public class ProviderService implements Provider<Source>
{

    public Source invoke(Source arg0)
    {
        return null;
    }

}
