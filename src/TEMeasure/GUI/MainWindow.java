package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Gridable;
import JISA.GUI.Tabs;

import java.io.IOException;

public class MainWindow extends Tabs {

    private ConfigStore      configStore      = new ConfigStore("TEMeasure");
    private ConnectionTab    connectionTab    = new ConnectionTab(configStore);
    private SMUConfigTab     smuConfigTab     = new SMUConfigTab(connectionTab);
    private TCConfigTab      tcConfigTab      = new TCConfigTab(connectionTab);
    private GatedTEMTab      gatedTEMTab      = new GatedTEMTab(smuConfigTab.heaterSMU, smuConfigTab.hotGateSMU, smuConfigTab.coldGateSMU, smuConfigTab.tvSMU, tcConfigTab.stage);
    private RTCalibrationTab rtCalibrationTab = new RTCalibrationTab(smuConfigTab.heaterSMU, smuConfigTab.rtSMU, tcConfigTab.stage);

    public MainWindow() throws IOException {
        super("TEMeasure");

        setMaximised(true);
        setExitOnClose(true);
        addTab(connectionTab);
        addTab(smuConfigTab);
        addTab(tcConfigTab);
        addTab(gatedTEMTab);
        addTab(rtCalibrationTab);
    }
}
