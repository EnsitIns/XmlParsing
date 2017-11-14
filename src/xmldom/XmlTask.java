package xmldom;

/*
 * TableXml.java
 *
 * Created on 15 Октябрь 2007 г., 11:30
 *
 * To change this template, choose Tools | Template Manager and open the
 * template in the editor.
 */
//import com.sun.java_cup.internal.parser;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.*;

/*
 * Класс создания и управления Xml-таблицами @author Сычев С.А.
 */
public class XmlTask {

    //Текущий логер
    public static org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger("LogServer");
//  Надо удалиь
    private static HashMap<String, File> hmFile = new HashMap();
    public static String DIR_PROGRAMM;
    public static File curDir; // текущщая директория
    /**
     * Статическая Коллекция всех загруженных таблиц
     */
    private static ArrayList<XmlTask> TableList = new ArrayList();

    public static HashMap<String, File> getHmFile() {
        return hmFile;
    }

    public static void setFile(final String name, final File file) {
        hmFile.put(name, file);

    }

    /**
     *
     * @return Рабочий каталог программы
     */
    public static String getApplicationPath() {

        String result = "";

        String sPath = System.getProperty("java.class.path");

        File file = new File(sPath);

        if (file != null && file.isFile()) {
            result = file.getParent();
        } else {

            result = System.getProperty("user.dir");

        }




        return result;
    }

    /**
     * ненен
     *
     * @param name имя файла
     * @return Файл
     */
    public static File getFile(String name) {
        return hmFile.get(name);

    }

    public static ArrayList<HashMap> getListbyXML(final Object fobj, final String nameNode,
            final HashMap<String, String> hmAttr) throws Exception {

        final ArrayList alValue = new ArrayList();

       
            final DefaultHandler handlerSax = new DefaultHandler() {
                @Override
                public void startElement(final String uri, final String localName, final String qName,
                        final Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);


                    boolean b = false;

                    if (hmAttr != null) {

                        Iterator<String> it = hmAttr.keySet().iterator();

                        while (it.hasNext()) {
                            String key = it.next();
                            String val = attributes.getValue(key);
                            String valset = hmAttr.get(key);
                            b = (valset.equals(val));
                            if (!b) {
                                break;
                            }

                        }
                    } else {
                        b = true;
                    }

                    if (qName.equals(nameNode) && b) {

                        HashMap hmObj = new HashMap();
                        alValue.add(hmObj);

                        for (int j = 0; j < attributes.getLength(); j++) {
                            String name = attributes.getQName(j);
                            String value = attributes.getValue(j);
                            hmObj.put(name, value);
                        }
               }

                }
            };


            SAXParserFactory factorySax = SAXParserFactory.newInstance();
            SAXParser parser = factorySax.newSAXParser();

            if (fobj instanceof File) {

                File f = (File) fobj;
                parser.parse(f, handlerSax);
            } else

            if (fobj instanceof InputStream) {

                InputStream is = (InputStream) fobj;

                parser.parse(is, handlerSax);
            } else  if (fobj instanceof String) {

                String dok=  (String) fobj;

                parser.parse(new InputSource(new StringReader(dok)), handlerSax);
            }

        return alValue;

    }

    /**
     * Сохраняет карту в файле xml в виде name=value
     *
     * @param hm карта
     * @param file путь к файлу Если file =null или "" то появляется диал. окно
     * выбора файла
     */
    public static void saveMapInXml(final Map<String, String> hm, final String file) throws Exception {

        Document dcm = getNewDocument();
        Element root = dcm.createElement("root");
        dcm.appendChild(root);

        for (String s : hm.keySet()) {
            String val = hm.get(s);
            Element elm = dcm.createElement("row");
            elm.setAttribute("name", s);
            elm.setAttribute("value", val);
            root.appendChild(elm);
        }

        saveXmlDocument(dcm, file);


    }

    /**
     * Загружает xml файл в карту
     *
     * @param hm карта
     * @param file путь к файлу Если file =null или "" то появляется диал. окно
     * выбора файла
     *
     */
    public static void loadXmlInMap(final Map<String, String> hm, final File file) throws Exception {

        Document dcm = getDocument(file);

        String sql = "child::row";

        Element root = dcm.getDocumentElement();




        NodeList nl = XmlTask.getNodeListByXpath(root, sql);

        for (int i = 0; i < nl.getLength(); i++) {
            Node elm = nl.item(i);
            String name = ((Element) elm).getAttribute("name");
            String value = ((Element) elm).getAttribute("value");
            hm.put(name, value);

        }


    }

    /**
     * Ищет узел по названию и значению атрибута.
     *
     * @param xNodeParent Узел где ищется.
     * @param attrName Имя атрибута.
     * @param attrValue Значение атрибута.
     * @param nameResultNode Имя когого уэла ищется если "" или NULL то во всех.
     * @return Найденный узел.
     */
    public static Node getNodeByAttribute(final Node xNodeParent, final String attrName,
            final String attrValue, final String nameResultNode) throws XPathExpressionException {

        String nn;
        if (nameResultNode == null || nameResultNode.equals("")) {
            nn = "*";
        } else {
            nn = nameResultNode;
        }
        String sql;

        sql = "descendant::" + nn + "[attribute::" + attrName + "=" + "'" + attrValue + "'" + "]";

        return getNodeByXpath(xNodeParent, sql);


    }

    /**
     * Парсит XML по XSL
     *
     * @param docXml XML документ
     * @param docXsl Файл XSL
     * @return HTML код
     */
    public static String parseXsl(final Document docXml, final File docXsl) throws Exception {

        StreamResult result = null;
        TransformerFactory tFactory = TransformerFactory.newInstance();
        DocumentBuilderFactory dbfpars = DocumentBuilderFactory.newInstance();
        dbfpars.setNamespaceAware(true);

        Document d = null;
        d = dbfpars.newDocumentBuilder().parse(docXsl);
        //StreamSource source = new StreamSource(new File("xmltest.xml"));

        DOMSource source = new DOMSource(docXml);

        DOMSource xslds = new DOMSource(d);
        Transformer transformer = null;
        transformer = tFactory.newTransformer(xslds);


        result = new StreamResult(new StringWriter());
        transformer.transform(source, result);



        return result.getWriter().toString();


    }

    /**
     * Парсит XML по XSL
     *
     * @param docXml XML документ
     * @param is Файл XSL
     * @return HTML код
     */
    public static String parseXsl(final Document docXml, final InputStream is) throws Exception {

        StreamResult result = null;
        TransformerFactory tFactory = TransformerFactory.newInstance();
        DocumentBuilderFactory dbfpars = DocumentBuilderFactory.newInstance();
        dbfpars.setNamespaceAware(true);

        Document d = null;
        d = dbfpars.newDocumentBuilder().parse(is);

        DOMSource source = new DOMSource(docXml);

        DOMSource xslds = new DOMSource(d);
        Transformer transformer = null;
        transformer = tFactory.newTransformer(xslds);

        result = new StreamResult(new StringWriter());

        transformer.transform(source, result);


        return result.getWriter().toString();


    }

    /**
     * ищет узелы по названию и значению атрибута.
     *
     * @param xNodeParent Узел где ищется.
     * @param alKeys Листинг Имен и значений атрибутов например id=234
     * @param sPref префикс
     * @param nameResultNode Имя когого уэла ищется если "" или NULL то во всех.
     * @return NodeList Коллекция найденых узлов
     */
    public static NodeList getNodeListByAttribute(final Node xNodeParent, final ArrayList alKeys,
            final String nameResultNode, final String sPref) throws Exception {

        String nn;
        if (nameResultNode == null || nameResultNode.equals("")) {
            nn = "*";
        } else {
            nn = nameResultNode;
        }

        String sql = "", attrName = "", attrValue = "";

        String sqlKey = "";

        if (alKeys != null) {


            for (Object obj : alKeys) {
                String s = obj.toString();
                String sa[] = s.split("=");
                attrName = "@" + sa[0];
                attrValue = sa[1];

                sqlKey = sqlKey + ((sqlKey.equals("")) ? "" : " " + sPref + " ")
                        + attrName + "=" + "'" + attrValue + "'";


            }


            sqlKey = "[" + sqlKey + "]";


        }

        sql = "descendant::" + nn + sqlKey;

        NodeList xn;
        xn = null;

        if (xNodeParent != null) {
            xn = getNodeListByXpath(xNodeParent, sql);
        }

        return xn;


    }

    /**
     * Парсинг по умолчанию
     */
    public static class DeffParse extends DefaultHandler {

        private String name = null;
        private String avalue = null;

        public DeffParse(final String name) {

            this.name = name;
        }

        public String getAvalue() {
            return avalue;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);

            String nameatt = attributes.getValue("name");

            if (nameatt != null && nameatt.equals(name)) {
                avalue = attributes.getValue("value");
            }
        }
    }

    /**
     * Возвращает значение параметра
     *
     * @param sDoc строка где ищется.
     * @param name листинг, куда пишутся атрибуты
     * @param date на дату
     * @return Значение параметра.
     */
    public static String getValueByName(final String sDoc, final String name, final java.util.Date date) throws Exception {


        DeffParse handler = new DeffParse(name);



        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = null;
            parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(sDoc)), handler);
     
        return handler.getAvalue();

    }

    /**
     * Создает карту из ВСЕХ антрибутов узла nameNode.
     *
     * @param sxml строка где ищется.
     * @param al листинг, куда пишутся атрибуты
     * @param
     * @param nameNode Название узла антрибутов.
     *
     */
    public static void getAttributesNodeBySax(final String sxml, final ArrayList<HashMap<String, String>> al, final String nameNode) throws Exception {
      
            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(final String uri, final String localName, final String qName,
                        final Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);



                    if (qName.equals(nameNode)) {

                        HashMap<String, String> hm = new HashMap();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String name = attributes.getQName(i);
                            String value = attributes.getValue(i);
                            hm.put(name, value);

                        }

                        al.add(hm);
                    }

                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(sxml)), handler);

        }

    /**
     * Создает карту из ВСЕХ антрибутов узла nameNode.
     *
     * @param f строка где ищется.
     * @param al листинг, куда пишутся атрибуты
     * @param nameNode Название узла антрибутов.
     *
     */
    public static void getValuesNodeBySax(final File f, final ArrayList<HashMap<String, String>> al, final String nameNode) throws Exception {
     
            DefaultHandler handler = new DefaultHandler() {
                String name;
                HashMap<String, String> hm;

                @Override
                public void characters(final char[] ch, final int start, final int length) throws SAXException {

                    if (hm != null && name != null) {

                        String value = String.valueOf(ch, start, length);

                        if (hm.containsKey(name)) {

                            value = hm.get(name) + value;

                            hm.put(name, value);

                        } else {
                            hm.put(name, value);
                        }
                    }


                }

                @Override
                public void endElement(final String uri, final String localName,
                        final String qName) throws SAXException {

                    if (nameNode.equals(qName)) {

                        al.add(hm);
                        al.add(hm);

                        name = null;


                    } else {

                        name = null;
                    }
                }

                @Override
                public void startElement(final String uri, final String localName,
                        final String qName, final Attributes attributes) throws SAXException {

                    if (nameNode.equals(qName)) {

                        hm = new HashMap();

                    } else {

                        if (hm != null) {

                            name = qName;
                        }

                    }

                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(f, handler);

    
    }

    /**
     * Создает карту из ВСЕХ антрибутов узла nameNode.
     *
     * @param f файл где ищется.
     * @param al листинг, куда пишутся атрибуты
     * @param
     * @param nameNode Название узла антрибутов.
     *
     */
    public static void getAttributesNodeBySax(final File f, 
            final ArrayList<HashMap<String, String>> al, final String nameNode)
            throws Exception {
    
            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(final String uri, final String localName,
                        final String qName, final Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);

                    if (qName.equals(nameNode)) {

                        HashMap<String, String> hm = new HashMap();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String name = attributes.getQName(i);
                            String value = attributes.getValue(i);
                            hm.put(name, value);

                        }

                        al.add(hm);
                    }

                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(f, handler);

     
    }

    /**
     * Создает карту из антрибутов узла nameNode.
     *
     * @param file Файл где ищется.
     * @param al листинг, куда пишутся атрибуты
     * @param nameNode Название узла антрибутов.
     */
    public static void getNamedNodeMapBySax(final File file, final ArrayList<Attributes> al,
            final String nameNode) throws Exception {
      
            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);




                    if (qName.equals(nameNode)) {
                        al.add(attributes);
                    }

                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(file, handler);

      
    }

    /**
     * Создает карту из антрибутов узла nameNode.
     *
     * @param xmlDoc XML документ в виде строки
     * @param attrName имя атрибута для выбора значения
     * @param alAttr Листинг куда пишутся значения атрибута attrName
     * @param nameNode Название узла где ищется.
     */
    public static void getArrayValueByAttr(final String xmlDoc, final String attrName,
            final ArrayList alAttr, final String nameNode) throws Exception {


            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(final String uri, final String localName, final String qName,
                        final Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);

                    if (qName.equals(nameNode)) {
                        String value = attributes.getValue(attrName);
                        alAttr.add(value);
                    }

                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(xmlDoc)), handler);

    
    }

    /**
     * Создает карту из пары антрибутов узла nameNode.
     *
     * @param xmlDoc XML документ в виде строки
     * @param attrName атрибут Key
     * @param attrValue атрибут Value
     * @param nameNode Название узла антрибутов.
     * @return Карту в виде: attrName=attrValue.
     */
    public static HashMap<String, String> getMapAttrByXML(String xmlDoc, final String attrName,
            final String attrValue, final String nameNode)throws Exception  {

        final HashMap<String, String> hmAttr = new HashMap();


            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);



                    if (qName.equals(nameNode)) {
                        String name = attributes.getValue(attrName);
                        String value = attributes.getValue(attrValue);
                        hmAttr.put(name, value);
                    }

                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(xmlDoc)), handler);

     
        return hmAttr;

    }

    /**
     * Возвращает карту параметров из XML файла
     *
     * @param file -XML файл
     * @param attName -атрибут имя
     * @param attValue -атрибут значение
     * @param nodeName -название узлов откуда берем
     * @return карту параметров "имя-значение"
     */
    public static HashMap<String, String> getParametersFromFile(File file, String attName,
            String attValue, String nodeName) throws Exception {

        HashMap<String, String> hmParam = new HashMap<String, String>();


        InputStream is = null;

        try {
            is = new FileInputStream(file);
            hmParam = getMapAttrByXML(is, attName, attValue, nodeName);

        } finally {

            is.close();
        }
        return hmParam;

    }

    /**
     * Создает карту из пары антрибутов узла NameNode.
     *
     * @param sourse Поток файл или строка (поток закрывается)
     * @param attrName атрибут Key
     * @param attrValue атрибут Value
     * @param NameNode Название узла антрибутов.
     * @return Карту в виде: attrName=attrValue.
     */
    public static HashMap<String, Object> getMapValuesByXML(Object sourse, final String attrName, final String attrValue, final String NameNode) throws Exception {

        final HashMap<String, Object> hmAttr = new HashMap();


        DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);

                Object value = null;

                if (qName.equals(NameNode)) {
                    String type = attributes.getValue("type").toUpperCase();
                    String name = attributes.getValue(attrName);
                    String svalue = attributes.getValue(attrValue);

                    if ("INTEGER".equals(type)) {

                        try {
                            value = Integer.parseInt(svalue);
                        } catch (NumberFormatException ex) {

                            value = -1;
                        }

                    } else if ("BYTE".equals(type)) {

                        try {
                            value = Byte.parseByte(svalue);
                        } catch (NumberFormatException ex) {

                            value = -1;
                        }
                    } else if ("BOOLEAN".equals(type)) {

                        value = ((svalue.trim().equals("1")|| svalue.trim().equals("true")) ? Boolean.TRUE : Boolean.FALSE);

                    } else {
                        value = svalue;
                    }

                    hmAttr.put(name, value);
                }

            }
        };

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        if (sourse instanceof String) {

            String xmlDoc = (String) sourse;
            parser.parse(new InputSource(new StringReader(xmlDoc)), handler);
        } else if (sourse instanceof File) {
            File file = (File) sourse;
            parser.parse(file, handler);
        } else if (sourse instanceof InputStream) {

            InputStream is = (InputStream) sourse;
            parser.parse(is, handler);
            is.close();

        }
        return hmAttr;

    }

    /**
     * Создает карту из пары антрибутов узла NameNode.
     *
     * @param is Входящий поток
     * @param attrName атрибут Key
     * @param attrValue атрибут Value
     * @param NameNode Название узла антрибутов.
     * @return Карту в виде: attrName=attrValue.
     */
    public static HashMap<String, String> getMapAttrByXML(InputStream is,
            final String attrName, final String attrValue, final String NameNode) throws Exception {

        final HashMap<String, String> hmAttr = new HashMap();


            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);



                    if (qName.equals(NameNode)) {
                        String name = attributes.getValue(attrName);
                        String value = attributes.getValue(attrValue);
                        hmAttr.put(name, value);
                    }

                }
            };

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(is, handler);
            is.close();


    
        return hmAttr;

    }

    /**
     * Создает карту из антрибутов узлов NameNode.
     *
     * @param aNodeParent Узел где ищется.
     * @param al листинг, куда пишутся атрибуты
     * @param NameNode имя Узела антрибутов.
     * @return
     */
    public static void getNamedNodeMap(Node aNodeParent, ArrayList<NamedNodeMap> al, String NameNode) throws Exception {


        NodeList nl = getNodeListByXpath(aNodeParent, "descendant::" + NameNode);


        for (int i = 0; i < nl.getLength(); i++) {
            Node nd = nl.item(i);
            al.add(nd.getAttributes());
        }

    }


    /**
     * ??????? ??????????? ????? ?? ?????????? ???? NameNode.
     *
     * @param aNodeParent ???? ??? ??????.
     * @param aAttName ??? ????????.
     * @param aAttValue ??? ??????? ????????.
     * @param NameNode ???? ??????????.
     * @return ????? ? ????: attrName=attrValue.
     * @throws java.lang.Exception
     */
    public static LinkedHashMap getLinkedMapAttrubuteByName(Node aNodeParent, String aAttName, String aAttValue, String NameNode) throws Exception {


        LinkedHashMap<String,String> result = new LinkedHashMap<String, String>();


        NodeList nl = getNodeListByXpath(aNodeParent, "descendant::" + NameNode);

        for (int i = 0; i < nl.getLength(); i++) {
            Node nd = nl.item(i);

            String sName;
            String sValue;


            NamedNodeMap nnm = nd.getAttributes();


            Node AttrNam = nnm.getNamedItem(aAttName);
            Node AttrVal = nnm.getNamedItem(aAttValue);


            if (AttrNam != null & AttrVal != null) {
                sName = AttrNam.getNodeValue();
                sValue = AttrVal.getNodeValue();

                result.put(sName, sValue);
            }
        }


        return result;
    }



    /**
     * Создает карту из антрибутов узла NameNode.
     *
     * @param aNodeParent Узел где ищется.
     * @param aAttName Имя атрибута.
     * @param aAttValue Имя второго атрибута.
     * @param NameNode Узел антрибутов.
     * @return Карту в виде: attrName=attrValue.
     */
    public static HashMap getMapAttrubuteByName(Node aNodeParent, String aAttName, String aAttValue, String NameNode) throws Exception {
        HashMap hm = new HashMap();

        NodeList nl = getNodeListByXpath(aNodeParent, "descendant::" + NameNode);

        for (int i = 0; i < nl.getLength(); i++) {
            Node nd = nl.item(i);

            String sName;
            String sValue;


            NamedNodeMap nnm = nd.getAttributes();


            Node AttrNam = nnm.getNamedItem(aAttName);
            Node AttrVal = nnm.getNamedItem(aAttValue);




            if (AttrNam != null & AttrVal != null) {
                sName = AttrNam.getNodeValue();
                sValue = AttrVal.getNodeValue();

                hm.put(sName, sValue);
            }
        }


        return hm;
    }

    /**
     * Открывает файл
     *
     * @param ext Задаваемое расширение (например *xml)
     * @param extMsg
     *
     */
    public static File openFile(final String ext, final String extMsg, Component parent) {


        final JFileChooser chooser = new JFileChooser();

        if (curDir != null) {

            chooser.setCurrentDirectory(curDir);
            chooser.setDialogTitle("" + curDir.getAbsolutePath());
        } else {

            curDir = chooser.getCurrentDirectory();
            chooser.setDialogTitle("" + curDir.getAbsolutePath());

        }

        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(ext);
            }

            @Override
            public String getDescription() {
                return extMsg;
            }
        });





        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        } else {

            curDir = chooser.getCurrentDirectory();
            return chooser.getSelectedFile();


        }


    }

    /**
     * Открывает файл
     *
     * @param ext Задаваемое расширение (например *xml)
     * @param extMsg
     * @param shoosemode тип выбора файл или директория или и то и другое
     */
    public static File openFile(final String ext, final String extMsg, int shoosemode, Component parent) {


        JFrame jf = new JFrame();
        final JFileChooser chooser = new JFileChooser();


        File curDir = chooser.getCurrentDirectory();

        chooser.setFileSelectionMode(shoosemode);
        chooser.setDialogTitle("" + curDir.getAbsolutePath());
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(ext);
            }

            @Override
            public String getDescription() {
                return extMsg;
            }
        });





        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        } else {
            return chooser.getSelectedFile();
        }


    }

    /**
     * Ищет узел по XPath выражению Пример: "descendant::*[attribute::idx='" +
     * fidx + "'][attribute::NameTable='" + Nametable + "']";
     *
     * @param nodeParent Узел где ищется
     * @param sql XPath выражение
     * @return найденый узел
     */
    public static Node getNodeByXpath(Node nodeParent, String sql) throws XPathExpressionException {
       
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            return (Node) xpath.evaluate(sql, nodeParent, XPathConstants.NODE);
       
    }

    /**
     * Строковое выражение преобразует в XML документ
     *
     * @param docstring Строковое выражение
     * @return XML документ
     */
    public static Document stringToXmlDoc(String docstring) throws Exception {

        Document document = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder bulder = null;
            bulder = factory.newDocumentBuilder();
     
            StringReader reader = new StringReader(docstring);

            InputSource source = new InputSource(reader);

            document = bulder.parse(source);
            reader.close();


     
        return document;
    }

    /**
     * Сохраняет документ под текущем именем
     *
     * @param doc XML документ
     *
     */
    public static void saveXmlDocument(Document doc) throws Exception {

            URI uri = new URI(doc.getDocumentURI());

            File file = new File(uri);

            if (file.exists()) {
                XmlTask.saveXmlDocument(doc, file);
            }

    }

    /**
     * XML документ преобразует в Строковое выражение
     *
     * @param docxml XML документ
     * @return Строковое выражение
     */
    public static String xmlDocToString(Document docxml) throws Exception {

        Transformer transformer = null;
            transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        //initialize StreamResult with File object to save to file
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(docxml);
            transformer.transform(source, result);
        return result.getWriter().toString();

    }

    /**
     * Возвращает листинг узлов по XPath выражению
     *
     * @param nodeParent Узел где ищется.
     * @param sql XPath выражение
     * @return NodeList
     */
    public static NodeList getNodeListByXpath(Node nodeParent, String sql)throws Exception {

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new PersonalNamespaceContext());

        XPathExpression expr = null;
            expr = xpath.compile(sql);
    
        Object result = null;
            result = expr.evaluate(nodeParent, XPathConstants.NODESET);
        return (NodeList) result;

    }

    public static class PersonalNamespaceContext implements NamespaceContext {

        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new NullPointerException("Null prefix");
            } else if ("svg".equals(prefix)) {
                return "http://www.w3.org/2000/svg";
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
    }

    public static String getStringByXpath(Node nodeParent, String sql) throws XPathExpressionException {
      
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            return (String) xpath.evaluate(sql, nodeParent, XPathConstants.STRING);
      
    }

    /**
     * Возвращает новый XMLDocument
     */
    public static Document getNewDocument() throws ParserConfigurationException {
            Document doc = null;


            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            //factory.setXIncludeAware(false);

            DocumentBuilder bulder;
            bulder = factory.newDocumentBuilder();
            doc = bulder.newDocument();

            return doc;
       
    }

    public static Document getDocument(InputStream is) throws Exception {

        Document doc = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        //  factory.setXIncludeAware(false);

        DocumentBuilder bulder = null;
            bulder = factory.newDocumentBuilder();
            doc = bulder.parse(is);

        return doc;
    }

    public static Document getDocument(File aFile) throws Exception {

        Document doc = null;

     
            boolean b = false;


            b = (aFile != null && aFile.isFile() && aFile.exists());


            if (!b) {
                aFile = openFile(".xml", "Фойлы XML", null);

                if (aFile == null) {
                    return null;
                }

            } else if (aFile == null && !aFile.exists()) {
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            //  factory.setXIncludeAware(false);

            DocumentBuilder bulder;
            bulder = factory.newDocumentBuilder();


            // FileInputStream in = new FileInputStream(aFile);

            doc = bulder.parse(aFile);

            // doc= bulder.parse(in);
            BaseFile = aFile;
            return doc;

    }

    /**
     * Записываем XML-Документ
     *
     * @param doc Существующий XML-Документ
     * @param aFile Файл для записи
     *
     */
    public static void saveXmlDocument(Document doc, File aFile) throws Exception {

        FileOutputStream fos;
        fos = null;
            fos = new FileOutputStream(aFile);


        Transformer t;
        try{

        t = TransformerFactory.newInstance().newTransformer();
                t.transform(new DOMSource(doc), new StreamResult(fos));

                t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        }finally{
                fos.close();
        }
    }

    public static void writeDocument(Document doc, String aPathDoc) throws FileNotFoundException {


        String NameFile = aPathDoc;



        if (aPathDoc == null || aPathDoc.equals("")) {
            final JFileChooser chooser = new JFileChooser();
            JFrame jf = new JFrame();

            File curDir = chooser.getCurrentDirectory();
            chooser.setDialogTitle("" + curDir.getAbsolutePath());

            if (chooser.showOpenDialog(jf) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                NameFile = f.getPath();
            }
        }

        File fil = new File(NameFile);





        DOMImplementation impl = doc.getImplementation();
        if (impl.hasFeature("LS", "3.0")) {
            DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
            LSSerializer serializer = implLS.createLSSerializer();
            LSOutput output = implLS.createLSOutput();
            output.setEncoding("UTF-8");

            FileOutputStream fout;
                fout = new FileOutputStream(fil);
                output.setByteStream(fout);
                serializer.write(doc, output);


         

        } else {
            // Нет поддержки LS ver.3.0
        }


    }

    /**
     * Записываем XML-Документ
     *
     * @param doc Существующий XML-Документ
     * @param aPathDoc Путь к файлу если пусто или NULL то выходит окно диалога
     * записи
     */
    public static File saveXmlDocument(Document doc, String aPathDoc) throws Exception {

 File fil=null;

            String NameFile = aPathDoc;

            if (aPathDoc == null || aPathDoc.equals("")) {
                final JFileChooser chooser = new JFileChooser();
                JFrame jf = new JFrame();

                 if(curDir==null){
                 curDir = chooser.getCurrentDirectory();
                }

                 chooser.setDialogTitle("" + curDir.getAbsolutePath());

                if (chooser.showOpenDialog(jf) == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    NameFile = f.getPath();
                }
            }

             fil = new File(NameFile);

            FileOutputStream fos;
            fos = null;
                fos = new FileOutputStream(fil);

                try{

            Transformer t = TransformerFactory.newInstance().newTransformer();


            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            t.transform(new DOMSource(doc), new StreamResult(fos));
        }finally{
            fos.close();
        }
       return fil;
        
    }
    // Файл базы данных.
    public static File BaseFile;

    /**
     * Устанавливает атрибут узла
     *
     * @param ThisNode Узел
     * @param NameAttribute Название Атрибута
     * @param ValueAttribute Значение атрибута
     *
     */
    public static void setAttribute(Node ThisNode, String NameAttribute, String ValueAttribute) {

        NamedNodeMap Nodemap = ThisNode.getAttributes();

        Node attr = Nodemap.getNamedItem(NameAttribute);

        if (attr == null) {


            Attr a = ThisNode.getOwnerDocument().createAttribute(NameAttribute);
            //Attr a = XMLDoc.createAttribute(NameAttribute);
            a.setValue(ValueAttribute);
            Nodemap.setNamedItem(a);
        } else {
            attr.setNodeValue(ValueAttribute);
        }

    }

    /**
     * Возвращает значение антрибуа узла
     *
     * @param thisNode Узел где ищется.
     * @param nameAttribute Имя атрибута.
     *
     *
     * @return Значение атрибута.
     */
    public static String getAttribute(final Node thisNode, final String nameAttribute) {

        NamedNodeMap nodemap = thisNode.getAttributes();

        Node attr = nodemap.getNamedItem(nameAttribute);

        if (attr != null) {
            return attr.getNodeValue();
        } else {
            return null;
        }

    }

    /**
     * Возвращает отсортированный список по названию атрибута
     *
     * @param nlParent Листинг где ищется.
     * @param nameAttr Имя атрибута.
     *
     *
     * @return Отсортированный список TreeSet.
     */
    public static TreeSet getListByNameAttribyte(final NodeList nlParent,
            final String nameAttr) {
        TreeSet ts = new TreeSet();
        Node node;
        Node attrNode;
        String valueAttr;


        for (int i = 0; i < nlParent.getLength(); i++) {
            node = nlParent.item(i);
            NamedNodeMap nnm = node.getAttributes();
            attrNode = nnm.getNamedItem(nameAttr);
            valueAttr = attrNode.getNodeValue();
            ts.add(valueAttr.trim());
        }



        return ts;
    }
}
