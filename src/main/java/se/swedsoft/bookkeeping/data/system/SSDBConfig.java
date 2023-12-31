package se.swedsoft.bookkeeping.data.system;


import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributeListImpl;


/**
 *
 * $Id: SSDBConfig.java 237 2021-11-01 17:53:13Z ellefj $
 */
public class SSDBConfig {

    private static final File CONFIG_FILE = new File(Path.get(Path.APP_BASE),
            "database.config");

    private static String iClientKey;

    private static String iServerAddress;

    private static Integer iCompanyId;

    private static Integer iYearId;

    private SSDBConfig() {}

    public static String getServerAddress() {
        return iServerAddress == null ? "" : iServerAddress;
    }

    public static Integer getCompanyId() {
        return iCompanyId;
    }

    public static void setCompanyId(Integer iId) {
        iCompanyId = iId;
        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));
            iParser.getDocument().getDocumentElement().setAttribute("company",
                    iCompanyId == null ? "" : iCompanyId.toString());

            // Write back the database path to the config file.
            OutputFormat iFormat = new OutputFormat(iParser.getDocument());
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                    iFormat);

            serializer.serialize(iParser.getDocument());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Integer getYearId() {
        return iYearId;
    }

    public static void setYearId(Integer pCompanyId, Integer iId) {
        iYearId = iId;
        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));
            iParser.getDocument().getDocumentElement().setAttribute("year",
                    iYearId == null ? "" : iYearId.toString());

            boolean iExists = false;
            NodeList iCompanyElements = iParser.getDocument().getDocumentElement().getElementsByTagName(
                    "company");

            for (int i = 0; i < iCompanyElements.getLength(); i++) {
                Node iCompanyNode = iCompanyElements.item(i);

                if (iCompanyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element iCompanyElement = (Element) iCompanyNode;
                    Integer iCompanyElementId = Integer.parseInt(
                            iCompanyElement.getAttribute("id"));

                    if (iCompanyElementId.equals(pCompanyId)) {
                        iCompanyElement.setAttribute("yearid",
                                iId == null ? "" : iId.toString());
                        iExists = true;
                    }
                }
            }
            if (!iExists) {
                Element iCompanyElement = iParser.getDocument().createElement("company");

                iCompanyElement.setAttribute("id", pCompanyId.toString());
                iCompanyElement.setAttribute("yearid", iId == null ? "" : iId.toString());
                iParser.getDocument().getDocumentElement().appendChild(iCompanyElement);
            }

            // Write back the database path to the config file.
            OutputFormat iFormat = new OutputFormat(iParser.getDocument());
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                    iFormat);

            serializer.serialize(iParser.getDocument());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static SSNewAccountingYear loadCompanySetting(Integer pCompanyId) {
        if (pCompanyId == null) {
            return null;
        }
        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));
            NodeList iCompanyElements = iParser.getDocument().getDocumentElement().getElementsByTagName(
                    "company");

            for (int i = 0; i < iCompanyElements.getLength(); i++) {
                Node iCompanyNode = iCompanyElements.item(i);

                if (iCompanyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element iCompanyElement = (Element) iCompanyNode;
                    Integer iCompanyElementId = Integer.parseInt(
                            iCompanyElement.getAttribute("id"));

                    if (iCompanyElementId.equals(pCompanyId)) {
                        String iResult = iCompanyElement.getAttribute("yearid");

                        if (iResult == null || iResult.length() == 0) {
                            return null;
                        }
                        SSNewAccountingYear iYear = new SSNewAccountingYear();

                        iYear.setId(Integer.parseInt(iResult));
                        return SSDB.getInstance().getAccountingYear(iYear);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void setServerAddress(String iAddress) {
        iServerAddress = iAddress;

        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));

            iParser.getDocument().getDocumentElement().setAttribute("server",
                    iServerAddress == null ? "" : iServerAddress);
            iParser.getDocument().getDocumentElement().setAttribute("company", "");
            iParser.getDocument().getDocumentElement().setAttribute("year", "");

            NodeList iCompanyElements = iParser.getDocument().getDocumentElement().getElementsByTagName(
                    "company");

            for (int i = 0; i < iCompanyElements.getLength(); i++) {
                Node iCompanyNode = iCompanyElements.item(i);

                iParser.getDocument().getDocumentElement().removeChild(iCompanyNode);
            }

            // Write back the database path to the config file.
            OutputFormat iFormat = new OutputFormat(iParser.getDocument());
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                    iFormat);

            serializer.serialize(iParser.getDocument());

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void setClientKey(String iKey) {
        iClientKey = iKey;
        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));
            iParser.getDocument().getDocumentElement().setAttribute("clientkey",
                    iClientKey == null ? "" : iClientKey);

            // Write back the database path to the config file.
            OutputFormat iFormat = new OutputFormat(iParser.getDocument());
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                    iFormat);

            serializer.serialize(iParser.getDocument());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getClientkey() {
        return iClientKey == null ? "" : iClientKey;
    }
    static {
        load();
    }
    
    /*
     * Create a config file if not found
    */
    private static void createIfNotExists() throws IOException {
        if (CONFIG_FILE.createNewFile()) {
            System.out.println("Creating database config file.");
            
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                new OutputFormat("XML", "UTF-8", true));

            try {
                serializer.startDocument();
                serializer.startElement("database", new AttributeListImpl());
                serializer.endElement("database");
                serializer.endDocument();
//                serializer.serialize(new );
            } catch (SAXException ex) {
            }
        }
    }

    /**
     *
     */
    public static void load() {

        DOMParser iParser = new DOMParser();

        try {
            createIfNotExists();
            
            // parser.set(false)
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));

            String iServer = null;

            if (iParser.getDocument().getDocumentElement().hasAttribute("server")) {
                iServer = iParser.getDocument().getDocumentElement().getAttribute("server");
            }

            String iCompany = null;

            if (iParser.getDocument().getDocumentElement().hasAttribute("company")) {
                iCompany = iParser.getDocument().getDocumentElement().getAttribute(
                        "company");
            }
            if (iCompany != null && iCompany.length() != 0) {
                iCompanyId = Integer.parseInt(iCompany);
            }

            String iYear = null;

            if (iParser.getDocument().getDocumentElement().hasAttribute("year")) {
                iYear = iParser.getDocument().getDocumentElement().getAttribute("year");
            }
            if (iYear != null && iYear.length() != 0) {
                iYearId = Integer.parseInt(iYear);
            }

            String iKey = null;

            if (iParser.getDocument().getDocumentElement().hasAttribute("clientkey")) {
                iKey = iParser.getDocument().getDocumentElement().getAttribute("clientkey");
            } else {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm z");

                setClientKey(dateFormat.format(new Date()));
            }

            iClientKey = iKey;

            iServerAddress = iServer;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
