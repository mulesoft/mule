<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:java="http://xml.apache.org/xslt/java"
        xmlns:helper="org.mule.config.XslHelper" exclude-result-prefixes="helper java"
        version='1.0'>

    <xsl:output method="xml" indent="yes" encoding="ISO-8859-1" standalone="yes"
                doctype-public="-//SPRING//DTD BEAN//EN"
                doctype-system="http://www.springframework.org/dtd/spring-beans.dtd"/>

    <xsl:template match="beans">
        <xsl:attribute name="default-init-method">initialise</xsl:attribute>
        <xsl:attribute name="default-destroy-method">dispose</xsl:attribute>
    </xsl:template>

    <xsl:template match="bean[@class='org.mule.impl.endpoint.MuleEndpoint']">
        <xsl:if test="property[@name='connector']">
            <xsl:attribute name="dependsOn">
                <xsl:value-of select="property[@name='connector']"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="scope">prototype</xsl:attribute>
     </xsl:template>

     <xsl:template match="bean[@class='org.mule.impl.MuleDescriptor']">
        <xsl:if test="property[@name='modelName']">
            <xsl:attribute name="dependsOn">
                <xsl:value-of select="property[@name='modelName']"/>
            </xsl:attribute>
        </xsl:if>
     </xsl:template>
</xsl:stylesheet>
