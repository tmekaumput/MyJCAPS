/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.iomate.tools.jcaps.myjcaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Decoder;

/**
 *
 * @author kmekaumput
 */
public class JCD {
    
    private transient CMAP cmap;
    private transient XPath xPath;
    private transient Node jcdNode;
    private String status;
    private Date since;
    private Properties numberMessageProcessed;
    private long numberMessageInProcess;
    private transient String tempFolder;
    
    private List jcdInbounds = new ArrayList();
    private List jcdOutbounds = new ArrayList();
    
    public List getJcdOutbounds() {
        return jcdOutbounds;
    }
        
    public List getJcdInbounds() {
        return jcdInbounds;
    }


    public JCD(CMAP cmap, Node jcdNode) {
        this.cmap = cmap;
        this.jcdNode = jcdNode;
        this.xPath = XMLUtils.getXPath();
    }
    
    public CMAP getCMAP() {
        return this.cmap;
    }

    public CMAP getCmap() {
        return cmap;
    }

    public void setCmap(CMAP cmap) {
        this.cmap = cmap;
    }

    public Node getJcdNode() {
        return jcdNode;
    }

    public void setJcdNode(Node jcdNode) {
        this.jcdNode = jcdNode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getSince() {
        return since;
    }

    public void setSince(Date since) {
        this.since = since;
    }

    public Properties getNumberMessageProcessed() {
        return numberMessageProcessed;
    }

    public void setNumberMessageProcessed(Properties numberMessageProcessed) {
        this.numberMessageProcessed = numberMessageProcessed;
    }

    public long getNumberMessageInProcess() {
        return numberMessageInProcess;
    }

    public void setNumberMessageInProcess(long numberMessageInProcess) {
        this.numberMessageInProcess = numberMessageInProcess;
    }
    
    public void populateJCDInboundProperites(JCAPSMBeanServerWrapper jcapsWrapper) throws Exception {

        Node cmNode = getJcdNode();
        NodeList cmLinknodeList = getLinkNodes(cmNode);

        for (int linkNodeIndex = 0; linkNodeIndex < cmLinknodeList.getLength(); linkNodeIndex++) {
            
            if (cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getLocalName().equals("Source")) {
                Map jcdInbound = new HashMap();

                NamedNodeMap srcAttrs = cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getAttributes();
                //System.out.println("         Node Inbound:");
                jcdInbound.put("INBOUND_TYPE", srcAttrs.getNamedItem("type").getTextContent());
                jcdInbound.put("INBOUND_NAME", srcAttrs.getNamedItem("name").getTextContent());

                //System.out.println("                 - Inbound Type: " + srcAttrs.getNamedItem("type"));
                //System.out.println("                 - Inbound Name: " + srcAttrs.getNamedItem("name"));
                Node srcNode = getCMNodeByNameAndType(cmNode.getParentNode(), srcAttrs.getNamedItem("name").getTextContent(), srcAttrs.getNamedItem("type").getTextContent());
                for (int cNodeIndex = 0; cNodeIndex < srcNode.getChildNodes().getLength(); cNodeIndex++) {

                    Node cmLinkNode = srcNode.getChildNodes().item(cNodeIndex);

                    //System.out.println("                 - Object Name: " + cmLinkNode.getAttributes().getNamedItem("objectName"));
                    ObjectName linkNodeObjectName = null;
                    try {
                        linkNodeObjectName = new ObjectName(cmLinkNode.getAttributes().getNamedItem("objectName").getTextContent());
                    } catch (NullPointerException ex) {
                        System.out.println("Unable to find instance for LinkNode [" + cmLinknodeList.item(linkNodeIndex).getAttributes().getNamedItem("name").getTextContent() + "]");
                    }
                    if (linkNodeObjectName != null) {
                        ObjectInstance objInstance = jcapsWrapper.getObjectInstance(linkNodeObjectName);
                        //System.out.println("                 - Class Name: " + objInstance.getClassName());
                        Map srcProps = new TreeMap();
                        jcdInbound.put("INBOUND_PROPERTIES_" + cNodeIndex, srcProps);
                        srcProps.put("INBOUND_OBJECTNAME", cmLinkNode.getAttributes().getNamedItem("objectName").getTextContent());
                        srcProps.put("INBOUND_CLASSNAME", objInstance.getClassName());
                        //System.out.println("         - Class Name: " + objInstance.getClassName());
                        if (objInstance.getClassName().equals("com.stc.jmsjca.core.ActivationMBean")) {
                            Map subscriberMap = new HashMap();
                            srcProps.put("SUBSCRIBERS", subscriberMap);
                            Properties props = (Properties) jcapsWrapper.invokeMethod(linkNodeObjectName, "getProperties", null, null);

                            subscriberMap.put("SUBSCRIBER_NAME", props.getProperty("subscriber.name"));
                            subscriberMap.put("DESTINATION_TYPE", props.getProperty("destination.type"));
                            subscriberMap.put("DESTINATION_NAME", props.getProperty("destination.name"));
                            subscriberMap.put("RESOURCE_ADAPTER", jcapsWrapper.getAttributeString(linkNodeObjectName, "ResourceAdapter"));
                        }
                    }
                }
                this.jcdInbounds.add(jcdInbound);
            }
        }
    }

    public void populateJCDOutboundProperties(JCAPSMBeanServerWrapper jcapsWrapper) throws XPathExpressionException, MalformedObjectNameException, InstanceNotFoundException, IOException, MBeanException, MBeanException, MBeanException, IOException, MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, Exception {
               
        Node cmNode = getJcdNode();
        NamedNodeMap nodeAttrs = cmNode.getAttributes();
        NodeList cmLinknodeList = getLinkNodes(cmNode);

        String jcdName = nodeAttrs.getNamedItem("name").getTextContent();

        for (int linkNodeIndex = 0; linkNodeIndex < cmLinknodeList.getLength(); linkNodeIndex++) {
            if (cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getLocalName().equals("Destination")) {    
                Map jcdOutbound = new HashMap();
                jcdOutbounds.add(jcdOutbound);
                
                NamedNodeMap destAttrs = cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getAttributes();

                jcdOutbound.put("OUTBOUND_TYPE", destAttrs.getNamedItem("type").getTextContent());
                jcdOutbound.put("OUTBOUND_NAME", destAttrs.getNamedItem("name").getTextContent());

                NamedNodeMap linkNodeAttrs = cmLinknodeList.item(linkNodeIndex).getAttributes();

                ObjectName linkObjectName = null;
                try {
                    linkObjectName = new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent());
                } catch (NullPointerException ex) {
                    System.out.println("Unable to find instance for LinkNode [" + cmLinknodeList.item(linkNodeIndex).getAttributes().getNamedItem("name").getTextContent() + "]");
                }
                if (linkObjectName != null) {
                    ObjectInstance objInstance = jcapsWrapper.getObjectInstance(linkObjectName);
                    //System.out.println("         Class Name:");
                    //System.out.println("         - " + objInstance.getClassName());
                    jcdOutbound.put("OUTBOUND_CLASSNAME", objInstance.getClassName());

                    if ("SQLSERVERADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                        jcdOutbound.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_JDBC_CONNECTION_SETTING, jcapsWrapper));
                    } else if ("HL7ADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                        jcdOutbound.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_TCP_SETTING, jcapsWrapper));
                    } else if ("HTTPADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                        jcdOutbound.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_HTTP_SETTING, jcapsWrapper));
                    } else if ("BatchLocalFile.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                        jcdOutbound.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_TARGET_LOCATION, jcapsWrapper));
                    } else if ("TCPIPEXTADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                        jcdOutbound.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_TCP_SETTING, jcapsWrapper));
                    }else if ("messageService.Topic".equals(destAttrs.getNamedItem("type").getTextContent())) {
                        ObjectName linkedNodeObjectName = new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent());
                        //System.out.println("         Configuration:");
                        //System.out.println("         - " + (String)connection.getAttribute(linkedNodeObjectName, "RAInfo"));
                        Map outboundProps = new TreeMap();
                        try{
                            outboundProps.put("RAInfo", jcapsWrapper.getAttributeString(linkedNodeObjectName, "RAInfo"));
                            jcdOutbound.put("OUTBOUND_PROPERTIES", outboundProps);
                        }catch(AttributeNotFoundException ex) {
                            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            System.out.println("Unable to find attributes for outbound [" + destAttrs.getNamedItem("name").getTextContent() + "]");
                            //MBeanInfo info = jcapsWrapper.getMBeanInfo(linkObjectName);
                            //MBeanAttributeInfo[] attrInfos = info.getAttributes();
                            //for(MBeanAttributeInfo attrInfo : attrInfos) {
                            //    System.out.println("-------------------");
                            //   System.out.println(attrInfo.getType());
                            //    System.out.println(attrInfo.getName());
                            //}
                            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        }


                    } else if ("messageService.Queue".equals(destAttrs.getNamedItem("type").getTextContent())) {
                        ObjectName linkedNodeObjectName = new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent());
                        //System.out.println("         Configuration:");
                        //System.out.println("         - " + (String)connection.getAttribute(linkedNodeObjectName, "RAInfo"));
                        Map outboundProps = new TreeMap();
                        try{
                            outboundProps.put("RAInfo", jcapsWrapper.getAttributeString(linkedNodeObjectName, "RAInfo"));
                            jcdOutbound.put("OUTBOUND_PROPERTIES", outboundProps);
                        }catch(AttributeNotFoundException ex) {
                            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            System.out.println("Unable to find attributes for outbound [" + destAttrs.getNamedItem("name").getTextContent() + "]");
                            //MBeanInfo info = jcapsWrapper.getMBeanInfo(linkObjectName);
                            //MBeanAttributeInfo[] attrInfos = info.getAttributes();
                            //for(MBeanAttributeInfo attrInfo : attrInfos) {
                            //    System.out.println("-------------------");
                            //    System.out.println(attrInfo.getType());
                            //    System.out.println(attrInfo.getName());
                            //}
                            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        }

                    } else {
                        ObjectName linkedNodeObjectName = new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent());

                        System.out.println("         Configuration:");
                        System.out.println("         - " + jcapsWrapper.getAttributeString(linkedNodeObjectName, "Configuration"));
                        BASE64Decoder decoder = new BASE64Decoder();
                        String configCDATA = new String(decoder.decodeBuffer((jcapsWrapper.getAttributeString(linkedNodeObjectName, "Configuration")).substring(6)));
                        String configXML = configCDATA.substring("<![CDATA[".length(), configCDATA.length() - 3);
                        configXML = configXML.replace("&lt;", "<").replace("&gt;", ">");
                        FileWriter fw = new FileWriter(tempFolder + jcdName + "." + destAttrs.getNamedItem("name").getTextContent() + "." + destAttrs.getNamedItem("type").getTextContent() + ".outbound");

                        fw.write(configXML);
                        fw.flush();
                        fw.close();
                    }
                }
            }
        }
    }

    private Map getAdapterProperties(String objectName, String searchExpression, JCAPSMBeanServerWrapper jcapsWrapper) throws MalformedObjectNameException, IOException, MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, Exception {
        Map adapterProps = new HashMap();

        ObjectName linkedNodeObjectName = new ObjectName(objectName);
        //System.out.println("         Configuration:");
        //System.out.println("         - " + (String) connection.getAttribute(linkedNodeObjectName, "Configuration"));
        BASE64Decoder decoder = new BASE64Decoder();
        String configCDATA = new String(decoder.decodeBuffer((jcapsWrapper.getAttributeString(linkedNodeObjectName, "Configuration")).substring(6)));
        String configXML = configCDATA.substring("<![CDATA[".length(), configCDATA.length() - 3);
        configXML = configXML.replace("&lt;", "<").replace("&gt;", ">");

        Document configDoc = XMLUtils.getDocument(configXML);

        NodeList propertiesNodeList = (NodeList) xPath.compile(searchExpression).evaluate(configDoc, XPathConstants.NODESET);

        if (propertiesNodeList.getLength() > 0) {

            Node jdbcSettingsNode = propertiesNodeList.item(0);
            NodeList paramNodes = (NodeList) xPath.compile(XMLUtils.XPATH_EXPRESSION_SEARCH_PARAMETER).evaluate(jdbcSettingsNode, XPathConstants.NODESET);

            for (int paramIndex = 0; paramIndex < paramNodes.getLength(); paramIndex++) {
                for (int valueIndex = 0; valueIndex < paramNodes.item(paramIndex).getChildNodes().getLength(); valueIndex++) {
                    String paramName = paramNodes.item(paramIndex).getAttributes().getNamedItem("name").getTextContent();
                    String paramValue = null;
                    String tmpNodeName = paramNodes.item(paramIndex).getChildNodes().item(valueIndex).getLocalName();
                    if (tmpNodeName.equals("value")) {
                        paramValue = paramNodes.item(paramIndex).getChildNodes().item(valueIndex).getTextContent();
                    }
                    adapterProps.put(paramName, paramValue);
                }
            }

        } else {
            System.out.println("############ERROR############");
            throw new Exception("Unable to find properties for the adapter [" + objectName + "]");
        }
        return adapterProps;
    }
    
    private NodeList getLinkNodes(Node cmNode) throws XPathExpressionException {
        String XPATH_EXPRESSION_SEARCH_JCD_LINK_NODES = "//cm:CMNode[@type='jce.JavaCollaborationDefinition']/cm:CMLink";
        NodeList jcdLinkNodes = (NodeList) xPath.compile(XPATH_EXPRESSION_SEARCH_JCD_LINK_NODES).evaluate(cmNode, XPathConstants.NODESET);

        return jcdLinkNodes;
    }

    private Node getCMNodeByNameAndType(Node cmapNode, String name, String type) throws XPathExpressionException {
        Node cmNode = (Node)xPath.compile("//cm:CMNode[@name='" + name + "' and @type='" + type + "']").evaluate(cmapNode, XPathConstants.NODE);
        return cmNode;
    }
    
}
