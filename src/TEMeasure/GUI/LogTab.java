package TEMeasure.GUI;


import JISA.Control.RTask;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Devices.TMeter;
import JISA.Experiment.Col;
import JISA.Experiment.ResultStream;
import JISA.Experiment.ResultTable;
import JISA.GUI.*;
import JISA.Util;
import javafx.scene.paint.Color;


public class LogTab extends Grid {

    private MainWindow mainWindow;

    private Plot        samplePlot   = new Plot("Sample Stage", "Time [mins]", "Temperature [K]");
    private Plot        radPlot      = new Plot("Radiation Shield", "Time [mins]", "Temperature [K]");
    private Plot        armPlot      = new Plot("Arm Sensor", "Time [mins]", "Temperature [K]");
    private Plot        refPlot      = new Plot("Reference", "Time [mins]", "Temperature [K]");
    private Plot        fStagePlot   = new Plot("First Stage", "Time [mins]", "Temperature [K]");
    private Plot        sStagePlot   = new Plot("Second Stage", "Time [mins]", "Temperature [K]");
    private Plot        heatPlot     = new Plot("Heaters", "Time [mins]", "Output [%]");
    private Plot        heatVolt     = new Plot("Heater Voltage", "Time [mins]", "Voltage [V]");
    private Plot        heatCurr     = new Plot("Heater Current", "Time [mins]", "Current [A]");
    private Plot        hotGateVolt  = new Plot("Hot-Gate Voltage", "Time [mins]", "Voltage [V]");
    private Plot        hotGateCurr  = new Plot("Hot-Gate Current", "Time [mins]", "Current [A]");
    private Plot        coldGateVolt = new Plot("Cold-Gate Voltage", "Time [mins]", "Voltage [V]");
    private Plot        coldGateCurr = new Plot("Cold-Gate Current", "Time [mins]", "Current [A]");
    private Plot        tvVolt       = new Plot("Thermal Voltage", "Time [mins]", "Voltage [V]");
    private Plot        tvCurr       = new Plot("Thermal Current", "Time [mins]", "Current [A]");
    private Plot        rtVolt       = new Plot("RT Voltage", "Time [mins]", "Voltage [V]");
    private Plot        rtCurr       = new Plot("RT Current", "Time [mins]", "Current [A]");
    private RTask       logger       = null;
    private ResultTable log          = null;


    public LogTab(MainWindow mainWindow) {

        super("Instrument Log", 3);
        this.mainWindow = mainWindow;

        addAll(
                samplePlot,
                radPlot,
                armPlot,
                refPlot,
                fStagePlot,
                sStagePlot,
                heatPlot,
                heatVolt,
                heatCurr,
                hotGateVolt,
                hotGateCurr,
                coldGateVolt,
                coldGateCurr,
                tvVolt,
                tvCurr,
                rtVolt,
                rtCurr
        );

        addToolbarButton("Start Log", () -> {

            if (logger != null && logger.isRunning()) {
                return;
            }

            String file = GUI.saveFileSelect();

            if (file != null) {
                try {
                    startLog(file);
                } catch (Exception e) {
                    GUI.errorAlert("Error", "Error", e.getMessage(), 600);
                }
            }

        });

        addToolbarButton("Stop Log", this::stopLog);

    }

    public void startLog(String outputFile) throws Exception {

        stopLog();

        log = new ResultStream(
                outputFile,
                new Col("Time", "min"),
                new Col("Sample T", "K"),
                new Col("Rad T", "K"),
                new Col("Arm T", "K"),
                new Col("Ref T", "K"),
                new Col("F Stage T", "K"),
                new Col("S Stage T", "K"),
                new Col("Sample SP", "K"),
                new Col("Rad SP", "K"),
                new Col("F Stage SP", "K"),
                new Col("S Stage SP", "K"),
                new Col("Heater Voltage", "V"),
                new Col("Heater Current", "A"),
                new Col("Hot-Gate Voltage", "V"),
                new Col("Hot-Gate Current", "A"),
                new Col("Cold-Gate Voltage", "V"),
                new Col("Cold-Gate Current", "A"),
                new Col("Thermal Voltage", "V"),
                new Col("Thermal Current", "A"),
                new Col("RT Voltage", "V"),
                new Col("RT Current", "A")
        );

        TC sampleTC = mainWindow.tcConfigTab.stage.getTController();
        TC radTC    = mainWindow.tcConfigTab.shield.getTController();
        TC fStageTC = mainWindow.tcConfigTab.fStage.getTController();
        TC sStageTC = mainWindow.tcConfigTab.sStage.getTController();

        TMeter sample = mainWindow.tcConfigTab.sampleSense.getTMeter();
        TMeter rad    = mainWindow.tcConfigTab.radSense.getTMeter();
        TMeter arm    = mainWindow.tcConfigTab.armSense.getTMeter();
        TMeter ref    = mainWindow.tcConfigTab.refSense.getTMeter();
        TMeter fStage = mainWindow.tcConfigTab.fStageSense.getTMeter();
        TMeter sStage = mainWindow.tcConfigTab.sStageSense.getTMeter();

        SMU heater   = mainWindow.smuConfigTab.heaterSMU.getSMU();
        SMU hotGate  = mainWindow.smuConfigTab.hotGateSMU.getSMU();
        SMU coldGate = mainWindow.smuConfigTab.coldGateSMU.getSMU();
        SMU tvSMU    = mainWindow.smuConfigTab.tvSMU.getSMU();
        SMU rtSMU    = mainWindow.smuConfigTab.rtSMU.getSMU();

        if (Util.areAnyNull(sampleTC, radTC, fStageTC, sStageTC, sample, rad, arm, ref, fStage, sStage, heater, hotGate, coldGate, tvSMU, rtSMU)) {
            throw new Exception("Instruments are not fully configured.");
        }

        samplePlot.clear();
        radPlot.clear();
        armPlot.clear();
        refPlot.clear();
        fStagePlot.clear();
        sStagePlot.clear();
        heatPlot.clear();
        heatVolt.clear();
        heatCurr.clear();
        hotGateVolt.clear();
        hotGateCurr.clear();
        coldGateVolt.clear();
        coldGateCurr.clear();
        tvVolt.clear();
        tvCurr.clear();
        rtVolt.clear();
        rtCurr.clear();

        Series sampleTS      = samplePlot.watchList(log, 0, 1, "Sensor", Color.BLUE);
        Series sampleSPS     = samplePlot.watchList(log, 0, 7, "Set-Point", Color.CORNFLOWERBLUE);
        Series radTS         = radPlot.watchList(log, 0, 2, "Sensor", Color.BLUE);
        Series radSPS        = radPlot.watchList(log, 0, 8, "Set-Point", Color.CORNFLOWERBLUE);
        Series armTS         = armPlot.watchList(log, 0, 3, "Sensor", Color.BLUE);
        Series refTS         = refPlot.watchList(log, 0, 4, "Sensor", Color.BLUE);
        Series fStageTS      = fStagePlot.watchList(log, 0, 5, "Sensor", Color.BLUE);
        Series fStageSPS     = fStagePlot.watchList(log, 0, 9, "Set-Point", Color.CORNFLOWERBLUE);
        Series sStageTS      = sStagePlot.watchList(log, 0, 6, "Sensor", Color.BLUE);
        Series sStageSPS     = sStagePlot.watchList(log, 0, 10, "Set-Point", Color.CORNFLOWERBLUE);
        Series heatVoltS     = heatVolt.watchList(log, 0, 11);
        Series heatCurrS     = heatCurr.watchList(log, 0, 12);
        Series hotGateVoltS  = hotGateVolt.watchList(log, 0, 11);
        Series hotGateCurrS  = hotGateCurr.watchList(log, 0, 12);
        Series coldGateVoltS = coldGateVolt.watchList(log, 0, 11);
        Series coldGateCurrS = coldGateCurr.watchList(log, 0, 12);
        Series tvVoltS       = tvVolt.watchList(log, 0, 11);
        Series tvCurrS       = tvCurr.watchList(log, 0, 12);
        Series rtVoltS       = rtVolt.watchList(log, 0, 11);
        Series rtCurrS       = rtCurr.watchList(log, 0, 12);

        sampleTS.showMarkers(false);
        sampleSPS.showMarkers(false);
        radTS.showMarkers(false);
        radSPS.showMarkers(false);
        armTS.showMarkers(false);
        refTS.showMarkers(false);
        fStageTS.showMarkers(false);
        fStageSPS.showMarkers(false);
        sStageTS.showMarkers(false);
        sStageSPS.showMarkers(false);
        heatVoltS.showMarkers(false);
        heatCurrS.showMarkers(false);
        hotGateVoltS.showMarkers(false);
        hotGateCurrS.showMarkers(false);
        coldGateVoltS.showMarkers(false);
        coldGateCurrS.showMarkers(false);
        tvVoltS.showMarkers(false);
        tvCurrS.showMarkers(false);
        rtVoltS.showMarkers(false);
        rtCurrS.showMarkers(false);

        // Repeating task, repeats every 2000 ms (2 seconds).
        logger = new RTask(2000, (t) -> {

            log.addData(
                    t.getSecFromStart() / 60.0,
                    sample.getTemperature(),
                    rad.getTemperature(),
                    arm.getTemperature(),
                    ref.getTemperature(),
                    fStage.getTemperature(),
                    sStage.getTemperature(),
                    sampleTC.getTargetTemperature(),
                    radTC.getTargetTemperature(),
                    fStageTC.getTargetTemperature(),
                    sStageTC.getTargetTemperature(),
                    heater.getVoltage(),
                    heater.getCurrent(),
                    hotGate.getVoltage(),
                    hotGate.getCurrent(),
                    coldGate.getVoltage(),
                    coldGate.getCurrent(),
                    tvSMU.getVoltage(),
                    tvSMU.getCurrent(),
                    rtSMU.getVoltage(),
                    rtSMU.getCurrent()
            );

        });

        logger.start();

    }

    public void stopLog() {
        if (logger != null) {
            logger.stop();
            log.finalise();
        }
    }

}
