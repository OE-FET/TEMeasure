package temeasure.gui;

import jisa.control.ConfigStore;
import jisa.gui.Tabs;

import java.io.IOException;

public class MainWindow extends Tabs {

    final ConfigStore      configStore      = new ConfigStore("TEMeasure");
    final ConnectionTab    connectionTab    = new ConnectionTab(this);
    final SMUConfigTab     smuConfigTab     = new SMUConfigTab(this);
    final TCConfigTab      tcConfigTab      = new TCConfigTab(this);
    final GatedTEMTab      gatedTEMTab      = new GatedTEMTab(this);
    final RTCalibrationTab rtCalibrationTab = new RTCalibrationTab(this);
    final TempTab          tempTab          = new TempTab(this);

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
