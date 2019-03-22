package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Tabs;

import java.io.IOException;

public class MainWindow extends Tabs {

    private ConfigStore      configStore      = new ConfigStore("TEMeasure");
    private ConnectionTab    connectionTab    = new ConnectionTab(configStore);
    private SMUConfigTab     smuConfigTab     = new SMUConfigTab(connectionTab, configStore);
    private TCConfigTab      tcConfigTab      = new TCConfigTab(connectionTab, configStore);
    private GatedTEMTab      gatedTEMTab      = new GatedTEMTab(smuConfigTab.heaterSMU, smuConfigTab.hotGateSMU, smuConfigTab.coldGateSMU, smuConfigTab.tvSMU, tcConfigTab.stage);
    private RTCalibrationTab rtCalibrationTab = new RTCalibrationTab(smuConfigTab.heaterSMU, smuConfigTab.rtSMU, tcConfigTab.stage);
    private TempTab          tempTab          = new TempTab(tcConfigTab.stage, tcConfigTab.shield, tcConfigTab.fStage, tcConfigTab.sStage);

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
}
