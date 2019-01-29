package TEMeasure.GUI;

import JISA.GUI.Gridable;
import JISA.GUI.Tabs;

public class MainWindow extends Tabs {

    private ConnectionTab    connectionTab    = new ConnectionTab();
    private SMUConfigTab     smuConfigTab     = new SMUConfigTab(connectionTab);
    private TCConfigTab      tcConfigTab      = new TCConfigTab(connectionTab);
    private GatedTEMTab      gatedTEMTab      = new GatedTEMTab(smuConfigTab.heaterSMU, smuConfigTab.gateSMU, smuConfigTab.tvSMU, tcConfigTab.stage);
    private RTCalibrationTab rtCalibrationTab = new RTCalibrationTab(smuConfigTab.heaterSMU, smuConfigTab.rtSMU, tcConfigTab.stage);

    public MainWindow() {
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
