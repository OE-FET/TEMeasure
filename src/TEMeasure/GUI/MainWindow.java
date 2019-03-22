package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Tabs;

import java.io.IOException;

public class MainWindow extends Tabs {

    ConfigStore      configStore      = new ConfigStore("TEMeasure");
    ConnectionTab    connectionTab    = new ConnectionTab(this);
    SMUConfigTab     smuConfigTab     = new SMUConfigTab(this);
    TCConfigTab      tcConfigTab      = new TCConfigTab(this);
    GatedTEMTab      gatedTEMTab      = new GatedTEMTab(this);
    RTCalibrationTab rtCalibrationTab = new RTCalibrationTab(this);
    TempTab          tempTab          = new TempTab(this);

    public MainWindow() throws IOException {
        super("TEMeasure");

        setMaximised(true);
        setExitOnClose(true);
        add(connectionTab);
        add(smuConfigTab);
        add(tcConfigTab);
        add(tempTab);
        add(gatedTEMTab);
        add(rtCalibrationTab);
    }

    public boolean isRunning() {
        return gatedTEMTab.isRunning() || rtCalibrationTab.isRunning();
    }

}
