<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="2.0"
                exclude-result-prefixes="xsl xsi">

    <xsl:output method="xml"/>

    <xsl:param name="SomeXml" as="node()"/>

    <xsl:template match="/">
        <result>
            <body>
                <xsl:sequence select="just"/>
            </body>
            <fromParam>
                <xsl:value-of select="$SomeXml/test/some/nested"/>
            </fromParam>
        </result>
    </xsl:template>
</xsl:stylesheet>
