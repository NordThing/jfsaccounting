package org.fribok.bookkeeping;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.FontPolicy;
import com.jgoodies.looks.FontPolicies;
import com.jgoodies.looks.FontSet;
import com.jgoodies.looks.FontSets;

import org.fribok.bookkeeping.app.Path;
import org.fribok.bookkeeping.app.Version;
import se.swedsoft.bookkeeping.data.system.SSCompanyLock;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSDBConfig;
import se.swedsoft.bookkeeping.data.system.SSYearLock;
import se.swedsoft.bookkeeping.data.util.SSConfig;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.company.SSCompanyFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.frame.SSFrameManager;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;

import javax.swing.*;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 *
 * @version $Id: Bookkeeping.java 237 2021-11-01 17:53:13Z ellefj $
 */
public class Bookkeeping {

    public static boolean iRunning;

    private Bookkeeping() {}

    /**
     *
     */
    private static void startupDatabase() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR: failed to load HSQLDB JDBC driver.");
            e.printStackTrace();
            return;
        }

        try {
            String iServerAddress = SSDBConfig.getServerAddress();

            if (iServerAddress.length() == 0) {
                // Kör mot lokal databas! Ingen låsning m.m.
                Connection iConnection = DriverManager.getConnection(
                        "jdbc:hsqldb:file:db" + File.separator + "JFSDB", "sa", "");

                SSDB.getInstance().startupLocal(iConnection);
            } else {
                // Kör mot en Fribokserver specificerad i serveradressen.
                Connection iConnection = DriverManager.getConnection(
                        "jdbc:hsqldb:hsql://" + iServerAddress + "/JFSDB", "sa", "");

                SSDB.getInstance().startupRemote(iConnection, iServerAddress);
            }

        } catch (SQLException e) {
            SSQueryDialog iDialog = new SSQueryDialog(SSMainFrame.getInstance(),
                    "noserverfoundstartup");

            if (iDialog.getResponce() == JOptionPane.YES_OPTION) {
                if (SSDB.getInstance().getLocking()) {
                    SSCompanyLock.removeLock(SSDB.getInstance().getCurrentCompany());
                    SSYearLock.removeLock(SSDB.getInstance().getCurrentYear());
                }
                SSDBConfig.setServerAddress(null);
                SSDB.getInstance().setCurrentCompany(null);
                SSDB.getInstance().setCurrentYear(null);
                SSFrameManager.getInstance().close();
                SSDB.getInstance().loadLocalDatabase();
                SSCompanyFrame.showFrame(SSMainFrame.getInstance(), 500, 300);
            }
        }
    }

    /**
     * The main method of the program.
     *
     * @param args The arguments to the program.
     */
    public static void main(String[] args) {
        try {
	    String os = System.getProperty("os.name");
	    FontSet fontSet = null;
	    if (os.startsWith("Windows")) {
		fontSet = FontSets.createDefaultFontSet(new Font(
				   "arial unicode MS", Font.PLAIN, 13));
	    } else {
		fontSet = FontSets.createDefaultFontSet(new Font(
				   "arial unicode", Font.PLAIN, 13));
	    }
	    FontPolicy fixedPolicy = FontPolicies.createFixedPolicy(fontSet);
	    Plastic3DLookAndFeel.setFontPolicy(fixedPolicy);
	    //Plastic3DLookAndFeel.setHighContrastFocusColorsEnabled(true);
	    String lnfClassName = Plastic3DLookAndFeel.class.getName();  
	    if (os.startsWith("Mac OS") || os.startsWith("Windows")) {
		lnfClassName = UIManager.getSystemLookAndFeelClassName();
	    } else {
		String xdgCurrentDesktop = System.getenv("XDG_CURRENT_DESKTOP");
		if ("Unity".equalsIgnoreCase(xdgCurrentDesktop)
				|| "XFCE".equalsIgnoreCase(xdgCurrentDesktop)
				|| "GNOME".equalsIgnoreCase(xdgCurrentDesktop)
				|| "X-Cinnamon".equalsIgnoreCase(xdgCurrentDesktop)
				|| "LXDE".equalsIgnoreCase(xdgCurrentDesktop)
				) {
			//lnfClassName = UIManager.getSystemLookAndFeelClassName();
			//lnfClassName = PlasticLookAndFeel.class.getName();
		} else {
			lnfClassName = Plastic3DLookAndFeel.class.getName();
		}
	    }

            UIManager.setLookAndFeel(lnfClassName);
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        iRunning = true;

        // Print information to ease debugging
        System.out.println("Starting up...");
        System.out.println("Title     : " + Version.APP_TITLE);
        System.out.println("Version   : " + Version.APP_VERSION);
        System.out.println("Build     : " + Version.APP_BUILD);
        System.out.println("Directory : " + Path.get(Path.APP_BASE));
        System.out.println("");
        System.out.println("Operating system: " + System.getProperty("os.name"));
        System.out.println("Architecture    : " + System.getProperty("os.arch"));
        System.out.println("Java version    : " + System.getProperty("java.version"));
        System.out.println("");
        System.out.println("Paths:");
        for (Path name : Path.values()) {
            System.out.printf("   %-12s = %s\n", name, Path.get(name));
        }

        String warning = null;

        // Create paths as needed, warning the user on failure
        for (Path name : Path.values()) {
            File dir = Path.get(name);

            if (!dir.exists()) {
                try {
                    if (dir.mkdirs()) {
                        System.out.println("Created " + dir);
                    } else {
                        warning = "unable to create";
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else if (!dir.isDirectory()) {
                warning = "exists but is not a directory";
            }
            if (warning != null) {
                System.out.println(" !! WARNING: " + dir + ' ' + warning);
                warning = null;
            }
        }

        // Create and display the main iMainFrame.
        SSMainFrame iMainFrame = SSMainFrame.getInstance();

        UIManager.put("InternalFrame.icon", SSIcon.getIcon("ICON_FRAME"));
        UIManager.put("InternalFrame.inactiveIcon", SSIcon.getIcon("ICON_FRAME"));
        startupDatabase();

        // Display the main frame.
        iMainFrame.setVisible(true);

        SSDB.getInstance().readOldDatabase();

        // Only display the company iMainFrame if there are no companies defined.
        // I would prefer to only open the select company iMainFrame if there are no companies.
        // But Fredrik and Joakim wants it to displayed every time.
        if ((Boolean) SSConfig.getInstance().get("companyframe.showatstart", true)) {
            iMainFrame.showCompanyFrame();
        }

        SSDB.getInstance().init(true);

        // Perhaps add some type of shut down hook.
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        new Runnable() {
            public void run() {
                SSFrameManager.getInstance().storeAllFrames();

                if (SSDBConfig.getClientkey() != null
                        && SSDBConfig.getClientkey().length() != 0) {
                    SSDB.getInstance().removeClient();
                }

                try {
                    iRunning = false;
                    SSDB.getInstance().shutdown();
                    // Shut down the database.
                    if (SSDB.getInstance().getLocking()
                            && SSDB.getInstance().getSocket() != null) {
                        SSDB.getInstance().getWriter().println("disconnect");
                        SSDB.getInstance().getWriter().flush();
                        SSDB.getInstance().getWriter().close();

                        SSDB.getInstance().getReader().close();
                        SSDB.getInstance().getSocket().close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

}
