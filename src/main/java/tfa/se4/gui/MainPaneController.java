package tfa.se4.gui;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import tfa.se4.Options;

public class MainPaneController {

    @FXML
    private TabPane tabPane;

    public void connectButtonClicked() {

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Setting Files", "*.properties")
        );
        File selected = chooser.showOpenDialog(SE4AdminGUI.getPrimaryStage());
        if (selected == null)
            return;

        // If we've already got this server being monitored. Select the tab and exit.
        for (final Tab t : tabPane.getTabs())
        {
            if (selected.getAbsolutePath().equals(t.getId()))
            {
                tabPane.getSelectionModel().select(t);
                return;
            }
        }

        final Tab newTab = new Tab(selected.getAbsolutePath());
        try
        {
            final Options opts = new Options(selected.getAbsolutePath());
            final MonitoredServerConnection conn = new MonitoredServerConnection(opts);
            final ServerPaneController controller = new ServerPaneController(conn);
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ServerPane.fxml"));
            loader.setController(controller);
            newTab.setContent(loader.load());
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setHeaderText("Unable to initialise connection");
            errorAlert.setContentText(e.getLocalizedMessage());
            errorAlert.showAndWait();
            System.exit(0);
        }

        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
    }

    /**
     * Handler for quit button. Close all the tabs and exit.
     */
    public void quitButtonClicked() {
        //TODO: close all tabs signalling controllers to shutdown threads
        SE4AdminGUI.getPrimaryStage().close();
        System.exit(0);
    }
}