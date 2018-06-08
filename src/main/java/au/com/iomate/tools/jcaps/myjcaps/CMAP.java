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

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Properties;
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
public class CMAP {
    private transient Project project;
    private transient Node cmapNode;
    private String name;
    private transient XPath xPath;
    private static String tempFolder = "F:\\apps\\JavaCAP63\\";
    
    private transient Map cmapInbound = new HashMap();
    private transient Map cmapOutbound = new HashMap();
    private Map cmapData = new HashMap();

    public Map getCMAPData() {
        return this.cmapData;
    }
    
    public Map getCMAPInbound() {
        return this.cmapInbound;
    }
    
    public Map getCMAPOutbound() {
        return this.cmapOutbound;
    }

    public CMAP(Project project, Node cmapNode) {
        this.project = project;
        this.cmapNode = cmapNode;

        cmapData.put("CMAP_INBOUNDS", cmapInbound);
        cmapData.put("CMAP_OUTBOUNDS", cmapOutbound);

        populateAttributes(this.cmapNode);
        this.xPath = XMLUtils.getXPath();
    }

    private void populateAttributes(Node cmapNode) {
        NamedNodeMap attrMap = cmapNode.getAttributes();
        this.setName(attrMap.getNamedItem("name").getTextContent());
    }


    private NodeList getCMNodes() throws XPathExpressionException {
        String XPATH_EXPRESSION_SEARCH_CMNODE = "//*[local-name()='CMNode']";
        NodeList cmNodeList = (NodeList) xPath.compile(XPATH_EXPRESSION_SEARCH_CMNODE).evaluate(getProject().getProjectNode(), XPathConstants.NODESET);
        return cmNodeList;
    }

    private Map getCMAPSources(Node cmapNode) throws XPathExpressionException {
        Map sourceMap = new HashMap();
        String XPATH_EXPRESSION_SEARCH_JCD_SOURCE_NODES = "//cm:CMNode[@type='jce.JavaCollaborationDefinition']/cm:CMLink/cm:Source";
        NodeList jcdSourceList = (NodeList) xPath.compile(XPATH_EXPRESSION_SEARCH_JCD_SOURCE_NODES).evaluate(cmapNode, XPathConstants.NODESET);

        for (int srcIndex = 0; srcIndex < jcdSourceList.getLength(); srcIndex++) {
            NamedNodeMap srcAttrs = jcdSourceList.item(srcIndex).getAttributes();
            sourceMap.put(srcAttrs.getNamedItem("name").getTextContent(), srcAttrs.getNamedItem("type").getTextContent());
        }
        return sourceMap;
    }

 
    private Map getCMAPDestinations(Node cmapNode) throws XPathExpressionException {
        Map destMap = new HashMap();
        String XPATH_EXPRESSION_SEARCH_DESTINATION_NODES = "//cm:CMNode/cm:CMLink/cm:Destination";
        NodeList jcdDestList = (NodeList) xPath.compile(XPATH_EXPRESSION_SEARCH_DESTINATION_NODES).evaluate(cmapNode, XPathConstants.NODESET);

        for (int destIndex = 0; destIndex < jcdDestList.getLength(); destIndex++) {
            NamedNodeMap destAttrs = jcdDestList.item(destIndex).getAttributes();
            destMap.put(destAttrs.getNamedItem("name").getTextContent(), destAttrs.getNamedItem("type").getTextContent());
        }
        return destMap;

    }

    private Node getCMNodeByNameAndType(Node cmapNode, String name, String type) throws XPathExpressionException {
        Node cmNode = (Node)xPath.compile("//cm:CMNode[@name='" + name + "' and @type='" + type + "']").evaluate(cmapNode, XPathConstants.NODE);
        return cmNode;
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

    public Map process(JCAPSMBeanServerWrapper jcapsWrapper) throws IOException, MalformedObjectNameException, InstanceNotFoundException, MBeanException, AttributeNotFoundException, ReflectionException, Exception {

        NodeList cmNodeList = this.getCMNodes();

        Map<String, String> sourceMap = getCMAPSources(getCmapNode());
        Map<String, String> destinationMap = getCMAPDestinations(getCmapNode());

        for (int nodeIndex = 0; nodeIndex < cmNodeList.getLength(); nodeIndex++) {

            NamedNodeMap nodeAttrs = cmNodeList.item(nodeIndex).getAttributes();

            if (nodeAttrs.getNamedItem("type").getTextContent().equals("jce.JavaCollaborationDefinition")) {
                Map jcdMap = (Map) cmapData.get("JCD_MAP");
                if (jcdMap == null) {
                    jcdMap = new TreeMap();
                    cmapData.put("JCD_MAP", jcdMap);
                }

                ObjectName nodeObjectName = new ObjectName(nodeAttrs.getNamedItem("objectName").getTextContent());
                JCD jcd = new JCD(this, cmNodeList.item(nodeIndex));
                jcd.setStatus(jcapsWrapper.getAttributeString(nodeObjectName, "Status"));
                jcd.setSince((java.util.Date)jcapsWrapper.getAttributeObject(nodeObjectName, "Since"));
                jcd.setNumberMessageProcessed((java.util.Properties)jcapsWrapper.getAttributeObject(nodeObjectName, "NumberMsgProcessed"));
                jcd.setNumberMessageInProcess((Long)jcapsWrapper.getAttributeObject(nodeObjectName, "NumberMsgInProcess"));
                
                //Map jcdNodeMap = new HashMap();
                String jcdName = nodeAttrs.getNamedItem("name").getTextContent();
                //jcdMap.put(jcdName, jcdNodeMap);
                jcdMap.put(jcdName, jcd);

                //ObjectName nodeObjectName = new ObjectName(nodeAttrs.getNamedItem("objectName").getTextContent());
                //jcdNodeMap.put("Status", jcapsWrapper.getAttributeString(nodeObjectName, "Status"));
                //jcdNodeMap.put("Since", (java.util.Date)jcapsWrapper.getAttributeObject(nodeObjectName, "Since"));
                //jcdNodeMap.put("NumberMsgProcessed", (java.util.Properties)jcapsWrapper.getAttributeObject(nodeObjectName, "NumberMsgProcessed"));
                //jcdNodeMap.put("NumberMsgInProcess", (Long)jcapsWrapper.getAttributeObject(nodeObjectName, "NumberMsgInProcess"));
                jcd.populateJCDInboundProperites(jcapsWrapper);
                jcd.populateJCDOutboundProperties(jcapsWrapper);
                //for (int linkNodeIndex = 0; linkNodeIndex < cmLinknodeList.getLength(); linkNodeIndex++) {
                    //if (cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getLocalName().equals("Source")) {
                        //Map jcdInbound = getJCDInboundProperites(cmNodeList.item(nodeIndex), jcapsWrapper);
                        //jcd.getJCDInbounds().add(jcdInbound);
                        //jcdNodeMap.put("JCD_INBOUND", jcdInbound);
                    //} else if (cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getLocalName().equals("Destination")) {
                        //Map jcdOutbound = getJCDOutboundProperties(cmNodeList.item(nodeIndex), jcapsWrapper);
                        //jcd.getJCDOutbounds().add(jcdOutbound);
                        //jcdNodeMap.put("JCD_OUTBOUND", jcdOutbound);
                    //}
                //}
            }
            
            NodeList cmLinknodeList = cmNodeList.item(nodeIndex).getChildNodes();

            for (int linkNodeIndex = 0; linkNodeIndex < cmLinknodeList.getLength(); linkNodeIndex++) {
                NamedNodeMap linkNodeAttrs = cmLinknodeList.item(linkNodeIndex).getAttributes();
                if (nodeAttrs.getNamedItem("type").getNodeValue().equals("jce.JavaCollaborationDefinition")) {
                    if (!linkNodeAttrs.getNamedItem("type").getNodeValue().equals("jce.JavaCollaborationDefinition.LINK")) {

                        if (cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getLocalName().equals("Destination")) {

                            NamedNodeMap destAttrs = cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getAttributes();
                            String type = sourceMap.get(destAttrs.getNamedItem("name").getTextContent());

                            if (type == null) {
                                String destName = destAttrs.getNamedItem("name").getTextContent();
                                String destType = destAttrs.getNamedItem("type").getTextContent();

                                //Map destProps = new TreeMap();
                                Map outboundMap = new HashMap();
                                cmapOutbound.put(destName + "-" + destType, outboundMap);

                                outboundMap.put("OUTBOUND_TYPE", destType);
                                outboundMap.put("OUTBOUND_NAME", destName);
                                ObjectInstance objInstance = jcapsWrapper.getObjectInstance(new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent()));
                                outboundMap.put("OUTBOUND_CLASSNAME", objInstance.getClassName());

                                if ("SQLSERVERADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                                    outboundMap.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_JDBC_CONNECTION_SETTING, jcapsWrapper));
                                } else if ("HL7ADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                                    outboundMap.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_TCP_SETTING, jcapsWrapper));
                                } else if ("HTTPADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                                    outboundMap.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_HTTP_SETTING, jcapsWrapper));
                                } else if ("BatchLocalFile.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                                    outboundMap.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_TARGET_LOCATION, jcapsWrapper));
                                } else if ("TCPIPEXTADAPTER.ExternalApplication".equals(destAttrs.getNamedItem("type").getTextContent())) {
                                    outboundMap.put("OUTBOUND_PROPERTIES", getAdapterProperties(linkNodeAttrs.getNamedItem("objectName").getTextContent(), XMLUtils.XPATH_EXPRESSION_SEARCH_TCP_SETTING, jcapsWrapper));
                                } else if ("messageService.Topic".equals(destAttrs.getNamedItem("type").getTextContent())) {
                                    ObjectName linkedNodeObjectName = new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent());
                                    Map destProps = new TreeMap();
                                    destProps.put("RAInfo", jcapsWrapper.getAttributeString(linkedNodeObjectName, "RAInfo"));
                                    outboundMap.put("OUTBOUND_PROPERTIES", destProps);
                                } else if ("messageService.Queue".equals(destAttrs.getNamedItem("type").getTextContent())) {
                                    ObjectName linkedNodeObjectName = new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent());
                                    Map destProps = new TreeMap();
                                    destProps.put("RAInfo", jcapsWrapper.getAttributeString(linkedNodeObjectName, "RAInfo"));
                                    outboundMap.put("OUTBOUND_PROPERTIES", destProps);
                                } else {
                                    ObjectName linkedNodeObjectName = new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent());

                                    System.out.println("         Configuration:");
                                    System.out.println("         - " + jcapsWrapper.getAttributeString(linkedNodeObjectName, "Configuration"));
                                    BASE64Decoder decoder = new BASE64Decoder();
                                    String configCDATA = new String(decoder.decodeBuffer((jcapsWrapper.getAttributeString(linkedNodeObjectName, "Configuration")).substring(6)));
                                    String configXML = configCDATA.substring("<![CDATA[".length(), configCDATA.length() - 3);
                                    configXML = configXML.replace("&lt;", "<").replace("&gt;", ">");
                                    System.out.println("         - " + configXML);
                                    FileWriter fw = new FileWriter(tempFolder + destAttrs.getNamedItem("type").getTextContent() + ".outbound");
                                    fw.write(configXML);
                                    fw.flush();
                                    fw.close();
                                }
                            }
                        }
                    }

                    if (cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getLocalName().equals("Source")) {

                        NamedNodeMap srcAttrs = cmLinknodeList.item(linkNodeIndex).getChildNodes().item(0).getAttributes();
                        String type = destinationMap.get(srcAttrs.getNamedItem("name").getTextContent());

                        if (type == null) {
                            String srcName = srcAttrs.getNamedItem("name").getTextContent();
                            String srcType = srcAttrs.getNamedItem("type").getTextContent();

                            Map inboundMap = new HashMap();
                            cmapInbound.put(srcName + "-" + srcType, inboundMap);

                            inboundMap.put("INBOUND_TYPE", srcType);
                            inboundMap.put("INBOUND_NAME", srcName);

                            Node destNode = getCMNodeByNameAndType( getCmapNode(), srcAttrs.getNamedItem("name").getTextContent(), srcAttrs.getNamedItem("type").getTextContent());
                            for (int cLinkNodeIndex = 0; cLinkNodeIndex < destNode.getChildNodes().getLength(); cLinkNodeIndex++) {
                                Node cmLinkNode = destNode.getChildNodes().item(cLinkNodeIndex);
                                //System.out.println("         - Object Name: " + cmLinkNode.getAttributes().getNamedItem("objectName"));
                                ObjectName linkNodeObjectName = new ObjectName(cmLinkNode.getAttributes().getNamedItem("objectName").getTextContent());
                                //ObjectInstance objInstance = connection.getObjectInstance(linkNodeObjectName);
                                ObjectInstance objInstance = jcapsWrapper.getObjectInstance(new ObjectName(linkNodeAttrs.getNamedItem("objectName").getTextContent()));

                                Map srcProps = new TreeMap();
                                inboundMap.put("INBOUND_PROPERTIES_" + cLinkNodeIndex, srcProps);
                                srcProps.put("INBOUND_OBJECTNAME", cmLinkNode.getAttributes().getNamedItem("objectName").getTextContent());
                                srcProps.put("INBOUND_CLASSNAME", objInstance.getClassName());
                                //System.out.println("         - Class Name: " + objInstance.getClassName());
                                if (objInstance.getClassName().equals("com.stc.jmsjca.core.ActivationMBean")) {
                                    Map subscriberMap = new HashMap();
                                    srcProps.put("SUBSCRIBERS", subscriberMap);
                                    Properties props = (Properties) jcapsWrapper.invokeMethod(linkNodeObjectName, "getProperties", null, null);
                                    //System.out.println("         - Subscriber Name: " + props.getProperty("subscriber.name"));
                                    //System.out.println("         - Destination Type: " + props.getProperty("destination.type"));
                                    //System.out.println("         - Destination Name: " + props.getProperty("destination.name"));
                                    //System.out.println("         - Resource Adapter: " + (String) connection.getAttribute(linkNodeObjectName, "ResourceAdapter"));

                                    subscriberMap.put("SUBSCRIBER_NAME", props.getProperty("subscriber.name"));
                                    subscriberMap.put("DESTINATION_TYPE", props.getProperty("destination.type"));
                                    subscriberMap.put("DESTINATION_NAME", props.getProperty("destination.name"));
                                    subscriberMap.put("RESOURCE_ADAPTER", jcapsWrapper.getAttributeString(linkNodeObjectName, "ResourceAdapter"));

                                } else if (objInstance.getClassName().equals("com.stc.connector.tcpip.hl7.mbeans.HL7ActivationSpecMonitor")) {
                                    Map tcpSettingsMap = new HashMap();
                                    Map hl7SegmentMap = new HashMap();
                                    srcProps.put("TCP_SETTINGS", tcpSettingsMap);
                                    srcProps.put("HL7_SEGMENT", hl7SegmentMap);

                                    //System.out.println("#######################");
                                    //System.out.println("         Configuration:");
                                    //System.out.println("         - " + (String) connection.getAttribute(linkNodeObjectName, "Configuration"));
                                    BASE64Decoder decoder = new BASE64Decoder();
                                    String configCDATA = new String(decoder.decodeBuffer((jcapsWrapper.getAttributeString(linkNodeObjectName, "Configuration")).substring(6)));
                                    String configXML = configCDATA.substring("<![CDATA[".length(), configCDATA.length() - 3);
                                    configXML = configXML.replace("&lt;", "<").replace("&gt;", ">");
                                    //System.out.println("         - " + configXML);

                                    //InputStream configStream = new ByteArrayInputStream(configXML.getBytes());
                                    //Document configDoc = docBuilder.parse(configStream);
                                    Document configDoc = XMLUtils.getDocument(configXML);

                                    NodeList tcpSettigsNodeList = (NodeList) xPath.compile(XMLUtils.XPATH_EXPRESSION_SEARCH_TCP_SERVER_SETTING).evaluate(configDoc, XPathConstants.NODESET);

                                    if (tcpSettigsNodeList.getLength() > 0) {

                                        Node tcpSettingsNode = tcpSettigsNodeList.item(0);
                                        String paramExpression = "parameter";
                                        NodeList paramNodes = (NodeList) xPath.compile(paramExpression).evaluate(tcpSettingsNode, XPathConstants.NODESET);
                                        for (int paramIndex = 0; paramIndex < paramNodes.getLength(); paramIndex++) {
                                            for (int valueIndex = 0; valueIndex < paramNodes.item(paramIndex).getChildNodes().getLength(); valueIndex++) {
                                                String paramName = paramNodes.item(paramIndex).getAttributes().getNamedItem("name").getTextContent();
                                                String paramValue = null;
                                                String tmpNodeName = paramNodes.item(paramIndex).getChildNodes().item(valueIndex).getLocalName();
                                                if (tmpNodeName.equals("value")) {
                                                    paramValue = paramNodes.item(paramIndex).getChildNodes().item(valueIndex).getTextContent();
                                                }
                                                tcpSettingsMap.put(paramName, paramValue);
                                            }
                                        }

                                    } else {
                                        System.out.println("############ERROR############");
                                    }


                                    NodeList hl7SegmentNodeList = (NodeList) xPath.compile(XMLUtils.XPATH_EXPRESSION_SEARCH_HL7_MSH_SEGMENT).evaluate(configDoc, XPathConstants.NODESET);

                                    if (hl7SegmentNodeList.getLength() > 0) {

                                        Node hl7SegmentNode = hl7SegmentNodeList.item(0);
                                        NodeList paramNodes = (NodeList) xPath.compile(XMLUtils.XPATH_EXPRESSION_SEARCH_PARAMETER).evaluate(hl7SegmentNode, XPathConstants.NODESET);
                                        for (int paramIndex = 0; paramIndex < paramNodes.getLength(); paramIndex++) {
                                            for (int valueIndex = 0; valueIndex < paramNodes.item(paramIndex).getChildNodes().getLength(); valueIndex++) {
                                                String paramName = paramNodes.item(paramIndex).getAttributes().getNamedItem("name").getTextContent();
                                                String paramValue = null;
                                                String tmpNodeName = paramNodes.item(paramIndex).getChildNodes().item(valueIndex).getLocalName();
                                                if (tmpNodeName.equals("value")) {
                                                    paramValue = paramNodes.item(paramIndex).getChildNodes().item(valueIndex).getTextContent();
                                                }
                                                hl7SegmentMap.put(paramName, paramValue);
                                            }
                                        }

                                    } else {
                                        System.out.println("############ERROR############");
                                    }

                                    /*
                                    FileWriter fw = new FileWriter(tempFolder + "HL7ConfigXML.inbound");
                                    fw.write(configXML);
                                    fw.flush();
                                    fw.close();
                                     *
                                     */
                                }
                            }

                        }
                    }
                }
            }
            //}
        }
        return cmapData;
    }

    /**
     * @return the project
     */
    public Project getProject() {
        return project;
    }

    /**
     * @param project the project to set
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * @return the cmapNode
     */
    public Node getCmapNode() {
        return cmapNode;
    }

    /**
     * @param cmapNode the cmapNode to set
     */
    public void setCmapNode(Node cmapNode) {
        this.cmapNode = cmapNode;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

}
