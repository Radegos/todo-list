package cz.muni.fi.pv168.project;

import cz.muni.fi.pv168.project.ui.MainWindow;
import org.apache.derby.jdbc.EmbeddedDataSource;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.swing.UIManager;

/**
 * The entry point of the application.
 **/
public class Main {

    private Main() {
        throw new AssertionError("This class is not intended for instantiation.");
    }

    public static void main(String[] args) {
        initNimbusLookAndFeel();
        var dataSource = createDataSource();
        EventQueue.invokeLater(() -> new MainWindow(dataSource).show());
    }

    private static DataSource createDataSource() {
        String dbPath = System.getProperty("user.home") + "/pv168/db/todo_list";
        EmbeddedDataSource dataSource = new EmbeddedDataSource();
        dataSource.setDatabaseName(dbPath);
        dataSource.setCreateDatabase("create");
        return dataSource;
    }

    private static void initNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Nimbus layout initialization failed", ex);
        }
    }
}
