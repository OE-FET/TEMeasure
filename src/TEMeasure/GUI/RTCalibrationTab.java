package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.Control.Field;
import JISA.Control.SRunnable;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Experiment.ResultTable;
import JISA.GUI.*;
import TEMeasure.Measurement.RTCalibration;
import javafx.scene.paint.Color;

import java.util.LinkedList;

public class RTCalibrationTab extends Grid {

    private final SMUConfig  heaterSMU;
    private final SMUConfig  rtSMU;
    private final TCConfig   stageTC;
    private final MainWindow mainWindow;
    private final Fields     heaterParams = new Fields("Heater");
    private final Fields     rtParams     = new Fields("RT");
    private final Fields     otherParams  = new Fields("Other");

    private final Field<Double>  rtStart;
    private final Field<Double>  rtStop;
    private final Field<Integer> rtSteps;
    private final Field<Double>  rtTime;

    private final Field<Double>  heaterStart;
    private final Field<Double>  heaterStop;
    private final Field<Integer> heaterSteps;
    private final Field<Double>  heaterTime;

    private final Field<Integer> nSweeps;
    private final Field<Double>  intTime;
    private final Field<String>  outputFile;

    private final Plot          heaterVPlot = new Plot("Heater Voltage", "Measurement No.", "Heater Voltage [V]");
    private final Plot          heaterPPlot = new Plot("Heater Power", "Measurement No.", "Heater Power [W]");
    private final Plot          rtPlot      = new Plot("RT Resistance", "Measurement No.", "Resistance [Ohms]");
    private final Table         table       = new Table("Table of Results");
    private final Field<Double> restTime;

    private RTCalibration measurement = null;

    public RTCalibrationTab(MainWindow mainWindow) {

        super("RT Calibration");
        heaterSMU = mainWindow.smuConfigTab.heaterSMU;
        rtSMU     = mainWindow.smuConfigTab.rtSMU;
        stageTC   = mainWindow.tcConfigTab.stage;

        this.mainWindow = mainWindow;

        setNumColumns(1);
        setGrowth(true, false);

        // Set-up heater parameters panel
        heaterStart = heaterParams.addDoubleField("Start Heater [V]", 0.0);
        heaterStop  = heaterParams.addDoubleField("Stop Heater [V]", 5.0);
        heaterSteps = heaterParams.addIntegerField("No. Steps", 11);
        heaterParams.addSeparator();
        heaterTime = heaterParams.addDoubleField("Hold Time [s]", 30.0);
        restTime   = heaterParams.addDoubleField("Resting Time [s]", 300);

        // Set-up heater parameters panel
        rtStart = rtParams.addDoubleField("Start Current [A]", 100e-6);
        rtStop  = rtParams.addDoubleField("Stop Current [A]", 100e-6);
        rtSteps = rtParams.addIntegerField("No. Steps", 5);
        rtParams.addSeparator();
        rtTime = rtParams.addDoubleField("Hold Time [s]", 100e-3);

        // Set-up other parameters panel
        nSweeps    = otherParams.addIntegerField("No. Sweeps", 2);
        intTime    = otherParams.addDoubleField("Integration Time [s]", 200e-3);
        outputFile = otherParams.addFileSave("Output File", "");

        // Link to config file - loads last used values (and will save values on exit)
        heaterParams.loadFromConfig("rt-heater-params", mainWindow.configStore);
        heaterParams.loadFromConfig("rt-rt-params", mainWindow.configStore);
        heaterParams.loadFromConfig("rt-other-params", mainWindow.configStore);

        Grid topGrid = new Grid(heaterParams, rtParams, otherParams, heaterVPlot, heaterPPlot, rtPlot);

        add(topGrid);
        add(new Grid(table));

        heaterVPlot.showLegend(false);
        heaterPPlot.showLegend(false);
        rtPlot.showLegend(false);

        addToolbarButton("Start", this::run);
        addToolbarButton("Stop", this::stop);

        fillDefaults();

    }

    private void disableInputs(boolean disable) {
        heaterParams.setFieldsDisabled(disable);
        rtParams.setFieldsDisabled(disable);
        otherParams.setFieldsDisabled(disable);
    }

    private void fillDefaults() {

        heaterStart.set(0.0);
        heaterStop.set(5.0);
        heaterSteps.set(6);
        heaterTime.set(10.0);

        rtStart.set(100.0e-6);
        rtStop.set(100.0e-6);
        rtSteps.set(1);
        rtTime.set(0.1);

        intTime.set(10.0 / 50.0);
        nSweeps.set(3);

    }

    private void run() {

        if (mainWindow.isRunning()) {
            GUI.errorAlert("Error", "Already Running", "Another measurement is already running!");
            return;
        }

        try {

            disableInputs(true);

            SMU                heaterVoltage = heaterSMU.get();
            SMU                rtMeasure     = rtSMU.get();
            TC                 stageTemp     = stageTC.get();
            LinkedList<String> errors        = new LinkedList<>();

            if (heaterVoltage == null) {
                errors.add("Heater SMU is not configured.");
            }

            if (rtMeasure == null) {
                errors.add("RT SMU is not configured.");
            }

            if (stageTemp == null) {
                errors.add("Sample T-Controller is not configured.");
            }

            if (outputFile.get().trim().equals("")) {
                errors.add("No output file specified.");
            }

            if (!errors.isEmpty()) {
                GUI.errorAlert("Error", "Error Starting Measurement", String.join("\n\n", errors), 600);
                return;
            }

            measurement = new RTCalibration(heaterVoltage, rtMeasure, stageTemp);

            measurement.configureRT(rtStart.get(), rtStop.get(), rtSteps.get())
                       .configureHeater(heaterStart.get(), heaterStop.get(), heaterSteps.get())
                       .configureTiming(heaterTime.get(), rtTime.get(), restTime.get(), intTime.get())
                       .configureSweeps(nSweeps.get());

            ResultTable results = measurement.newResults(outputFile.get());

            configurePlots(results);

            measurement.performMeasurement();

            if (measurement.wasStopped()) {
                GUI.warningAlert("Stopped", "Measurement Stopped", "The measurement was stopped before completion.");
            } else {
                GUI.infoAlert("Complete", "Measurement Completed", "The measurement completed without error.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            GUI.errorAlert("Error", "Exception Encountered", e.getMessage(), 600);
        } finally {
            disableInputs(false);
        }

    }

    private void configurePlots(ResultTable results) {

        heaterVPlot.clear();

        heaterVPlot.createSeries()
                   .watch(results, RTCalibration.COL_NUMBER, RTCalibration.COL_HEATER_VOLTAGE)
                   .setName("Voltage")
                   .setColour(Colour.TEAL);

        heaterPPlot.clear();

        heaterPPlot.createSeries()
                   .watch(results, RTCalibration.COL_NUMBER, RTCalibration.COL_HEATER_POWER)
                   .setName("Power")
                   .setColour(Colour.ORANGE);

        rtPlot.clear();

        rtPlot.createSeries()
              .watch(results, RTCalibration.COL_NUMBER, RTCalibration.COL_RT_RESISTANCE)
              .setName("Resistance")
              .setColour(Colour.CORNFLOWERBLUE);

        table.clear();
        table.watchList(results);

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
