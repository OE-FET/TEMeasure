package TEMeasure.GUI;

import JISA.Control.Field;
import JISA.Devices.DeviceException;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Experiment.ResultTable;
import JISA.GUI.*;
import TEMeasure.Measurement.GatedTEM;
import TEMeasure.Measurement.RTCalibration;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.LinkedList;

public class RTCalibrationTab extends Grid {

    private final SMUConfig heaterSMU;
    private final SMUConfig rtSMU;
    private final TCConfig  stageTC;
    private final Fields    heaterParams = new Fields("Heater");
    private final Fields    rtParams     = new Fields("RT");
    private final Fields    otherParams  = new Fields("Other");

    private final Field<Double>  rtStart;
    private final Field<Double>  rtStop;
    private final Field<Integer> rtSteps;
    private final Field<Double>  rtTime;

    private final Field<Double>  heaterStart;
    private final Field<Double>  heaterStop;
    private final Field<Integer> heaterSteps;
    private final Field<Double>  heaterTime;

    private final Field<Integer> nSweeps;
    private final Field<Double> intTime;
    private final Field<String> outputFile;

    private final Plot heaterVPlot = new Plot("Heater Voltage", "Measurement No.", "Heater Voltage [V]");
    private final Plot heaterPPlot = new Plot("Heater Power", "Measurement No.", "Heater Power [W]");
    private final Plot rtPlot      = new Plot("RT Resistance", "Measurement No.", "Resistance [Ohms]");

    private RTCalibration measurement = null;

    public RTCalibrationTab(SMUConfig heaterSMU, SMUConfig rtSMU, TCConfig stageTC) {

        super("RT Calibration");
        this.heaterSMU = heaterSMU;
        this.rtSMU = rtSMU;
        this.stageTC = stageTC;

        setNumColumns(1);
        setGrowth(true, false);

        // Set-up heater parameters panel
        heaterStart = heaterParams.addDoubleField("Start Heater [V]");
        heaterStop = heaterParams.addDoubleField("Stop Heater [V]");
        heaterSteps = heaterParams.addIntegerField("No. Steps");
        heaterParams.addSeparator();
        heaterTime = heaterParams.addDoubleField("Hold Time [s]");
        // Set-up heater parameters panel
        rtStart = rtParams.addDoubleField("Start Current [uA]");
        rtStop = rtParams.addDoubleField("Stop Current [uA]");
        rtSteps = rtParams.addIntegerField("No. Steps");
        rtParams.addSeparator();
        rtTime = rtParams.addDoubleField("Hold Time [s]");

        // Set-up other parameters panel
        nSweeps = otherParams.addIntegerField("No. Sweeps");
        intTime = otherParams.addDoubleField("Integration Time [s]");
        outputFile = otherParams.addFileSave("Output File");

        Grid topGrid = new Grid("", heaterParams, rtParams, otherParams);
        Grid bottomGrid = new Grid("", heaterVPlot, heaterPPlot, rtPlot);

        add(topGrid);
        add(bottomGrid);

        heaterVPlot.showLegend(false);
        heaterPPlot.showLegend(false);
        rtPlot.showLegend(false);

        addToolbarButton("Start", this::run);
        addToolbarButton("Stop", this::stop);

        fillDefaults();

    }

    private void disableInputs(boolean disable) {

        rtStart.setDisabled(disable);
        rtStop.setDisabled(disable);
        rtSteps.setDisabled(disable);
        rtTime.setDisabled(disable);
        heaterStart.setDisabled(disable);
        heaterStop.setDisabled(disable);
        heaterSteps.setDisabled(disable);
        heaterTime.setDisabled(disable);
        intTime.setDisabled(disable);
        outputFile.setDisabled(disable);
        nSweeps.setDisabled(disable);

    }

    private void fillDefaults() {

        heaterStart.set(0.0);
        heaterStop.set(5.0);
        heaterSteps.set(6);
        heaterTime.set(10.0);

        rtStart.set(100.0);
        rtStop.set(100.0);
        rtSteps.set(1);
        rtTime.set(0.1);

        intTime.set(10.0 / 50.0);
        nSweeps.set(3);

    }

    public void run() {

        try {

            disableInputs(true);

            SMU                heaterVoltage = heaterSMU.getSMU();
            SMU                rtMeasure     = rtSMU.getSMU();
            TC                 stageTemp     = stageTC.getTController();
            LinkedList<String> errors        = new LinkedList<>();

            if (heaterVoltage == null) {
                errors.add("Heater SMU is not configured.");
            }

            if (rtMeasure == null) {
                errors.add("RT SMU is not configured.");
            }

            if (stageTemp == null) {
                errors.add("Stage T-Controller is not configured.");
            }

            if (outputFile.get().trim().equals("")) {
                errors.add("No output file specified.");
            }

            if (!errors.isEmpty()) {
                GUI.errorAlert("Error", "Error Starting Measurement", String.join("\n\n", errors), 600);
                return;
            }

            measurement = new RTCalibration(heaterVoltage, rtMeasure, stageTemp);

            measurement.configureRT(rtStart.get() * 1e-6, rtStop.get() * 1e-6, rtSteps.get())
                       .configureHeater(heaterStart.get(), heaterStop.get(), heaterSteps.get())
                       .configureTiming(heaterTime.get(), rtTime.get(), intTime.get())
                       .configureSweeps(nSweeps.get());

            ResultTable results = measurement.newResults(outputFile.get());

            heaterVPlot.clear();
            heaterVPlot.watchList(results, RTCalibration.COL_NUMBER, RTCalibration.COL_HEATER_VOLTAGE, "Voltage", Color.TEAL);

            heaterPPlot.clear();
            heaterPPlot.watchList(results, RTCalibration.COL_NUMBER, RTCalibration.COL_HEATER_POWER, "Power", Color.ORANGE);

            rtPlot.clear();
            rtPlot.watchList(results, RTCalibration.COL_NUMBER, RTCalibration.COL_RT_RESISTANCE, "Resistance", Color.CORNFLOWERBLUE);

            measurement.performMeasurement();

            GUI.infoAlert("Complete", "Measurement Complete", "The measurement completed without error.");

        } catch (Exception e) {
            e.printStackTrace();
            GUI.errorAlert("Error", "Exception Encountered", e.getMessage(), 600);
        } finally {
            disableInputs(false);
        }

    }

    public void stop() {
        if (measurement != null) {
            measurement.stop();
        }
    }

    public boolean isRunning() {
        return measurement != null && measurement.isRunning();
    }
}
