/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.iomate.tools.jcaps.myjcaps;

/**
 *
 * @author kmekaumput
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author kmekaumput
 */
public class XMLUtils {

    //private static XPath xPath;
    public static final String XPATH_EXPRESSION_SVG_SEARCH = "//*[local-name()='SVG']";
    public static final String XPATH_EXPRESSION_SEARCH_PROJECT = "//*[local-name()='Project'][count(@*)>2]";
    public static final String XPATH_EXPRESSION_SEARCH_JDBC_CONNECTION_SETTING = "/configuration/instance/cfg/configuration/section[@name=\"JDBCConnectorSettings\"]";
    public static final String XPATH_EXPRESSION_SEARCH_PARAMETER = "parameter";
    public static final String XPATH_EXPRESSION_SEARCH_TCP_SETTING = "/configuration/instance/cfg/configuration/section[@name=\"TCPIPBaseSettings\"]";
    public static final String XPATH_EXPRESSION_SEARCH_HTTP_SETTING = "/configuration/instance/cfg/configuration/section[@name=\"HTTPSettings\"]";
    public static final String XPATH_EXPRESSION_SEARCH_TARGET_LOCATION = "/configuration/instance/cfg/configuration/section[@name=\"Target Location\"]";
    public static final String XPATH_EXPRESSION_SEARCH_TCP_SERVER_SETTING = "/configuration/instance/cfg/configuration/section[@name=\"TCPIPServerBaseSettings\"]";
    public static final String XPATH_EXPRESSION_SEARCH_HL7_MSH_SEGMENT = "/configuration/instance/cfg/configuration/section[@name=\"HL7MSHSegment\"]";

    public static XPath getXPath() {

        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new NullPointerException("Null prefix");
                } else if ("cm".equals(prefix)) {
                    return "http://www.seebeyond.com/ican/codegen/cm";
                } else if ("xml".equals(prefix)) {
                    return XMLConstants.XML_NS_URI;
                }
                return XMLConstants.NULL_NS_URI;
            }

            // This method isn't necessary for XPath processing.
            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }

            // This method isn't necessary for XPath processing either.
            public Iterator getPrefixes(String uri) {
                throw new UnsupportedOperationException();
            }
        });
        return xPath;
    }

    public static Document getDocument(String content) throws Exception {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            InputStream stream = new ByteArrayInputStream(content.getBytes());

            Document doc = docBuilder.parse(stream);
            return doc;

        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
            throw ex;
        } catch (SAXException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public static NodeList extractProjectNodes(Document doc) throws XPathExpressionException {

        NodeList projectNodeList = (NodeList) getXPath().compile(XPATH_EXPRESSION_SEARCH_PROJECT).evaluate(doc, XPathConstants.NODESET);
        //NodeList projectNodeList = doc.getElementsByTagNameNS("http://www.seebeyond.com/ican/codegen/cm", "Project");

        return projectNodeList;
    }
}
