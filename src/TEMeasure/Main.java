package TEMeasure;

import JISA.Control.ConfigStore;
import JISA.GUI.GUI;
import TEMeasure.GUI.MainWindow;

import java.io.IOException;

public class Main extends GUI {

    public static void main(String[] args) throws IOException {

        MainWindow mainWindow = new MainWindow();
        mainWindow.show();

    }
}
