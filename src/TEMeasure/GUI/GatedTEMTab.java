package TEMeasure.GUI;

import JISA.Control.Field;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Experiment.ResultTable;
import JISA.GUI.*;
import TEMeasure.Measurement.GatedTEM;
import javafx.scene.paint.Color;

import java.util.LinkedList;

@SuppressWarnings("Duplicates")
public class GatedTEMTab extends Grid {

    private final MainWindow mainWindow;
    private final Fields     gateParams   = new Fields("Gate");
    private final Fields     heaterParams = new Fields("Heater");
    private final Fields     otherParams  = new Fields("Other");

    private final Field<Double>  gateStart;
    private final Field<Double>  gateStop;
    private final Field<Integer> gateSteps;
    private final Field<Double>  gateTime;

    private final Field<Double>  heaterStart;
    private final Field<Double>  heaterStop;
    private final Field<Integer> heaterSteps;
    private final Field<Double>  heaterTime;

    private final Field<Double> intTime;
    private final Field<String> outputFile;

    private final Plot  heaterPlot  = new Plot("Heater Power", "Measurement No.", "Heater Power [W]");
    private final Plot  gatePlot    = new Plot("Gate Voltage", "Measurement No.", "Gate Voltage [V]");
    private final Plot  thermalPlot = new Plot("Thermo-Voltage", "Measurement No.", "Thermo-Voltage [V]");
    private final Plot  tpPlot      = new Plot("TV vs Power", "Heater Power [W]", "Thermo-Voltage [V]");
    private final Table table       = new Table("Table of Results");

    private GatedTEM measurement = null;

    public GatedTEMTab(MainWindow mainWindow) {

        super("Gated TE Measurement");
        this.mainWindow = mainWindow;

        setNumColumns(1);
        setGrowth(true, false);

        // Set-up gate parameters panel
        gateStart = gateParams.addDoubleField("Start Gate [V]");
        gateStop  = gateParams.addDoubleField("Stop Gate [V]");
        gateSteps = gateParams.addIntegerField("No. Steps");
        gateParams.addSeparator();
        gateTime  = gateParams.addDoubleField("Hold Time [s]");

        // Set-up heater parameters panel
        heaterStart = heaterParams.addDoubleField("Start Heater [V]");
        heaterStop  = heaterParams.addDoubleField("Stop Heater [V]");
        heaterSteps = heaterParams.addIntegerField("No. Steps");
        heaterParams.addSeparator();
        heaterTime  = heaterParams.addDoubleField("Hold Time [s]");

        // Set-up other parameters panel
        intTime    = otherParams.addDoubleField("Integration Time [s]");
        outputFile = otherParams.addFileSave("Output File");

        Grid topGrid    = new Grid("", gateParams, heaterParams, otherParams);
        Grid bottomGrid = new Grid("", heaterPlot, gatePlot, thermalPlot, tpPlot);
        bottomGrid.setNumColumns(2);
        ;

        add(topGrid);
        add(bottomGrid);
        add(new Grid("", table));

        heaterPlot.showLegend(false);
        gatePlot.showLegend(true);
        thermalPlot.showLegend(false);

        addToolbarButton("Start", this::run);
        addToolbarButton("Stop", this::stop);

        fillDefaults();

    }

    private void fillDefaults() {

        heaterStart.set(0.0);
        heaterStop.set(5.0);
        heaterSteps.set(6);
        heaterTime.set(10.0);

        gateStart.set(-40.0);
        gateStop.set(0.0);
        gateSteps.set(9);
        gateTime.set(20.0);

        intTime.set(10.0 / 50.0);

    }

    private void disableInputs(boolean disable) {

        gateParams.setFieldsDisabled(disable);
        heaterParams.setFieldsDisabled(disable);
        otherParams.setFieldsDisabled(disable);

    }

    /**
     * Checks if all instruments are present and, if so, runs the measurement
     */
    private void run() {

        // Make sure nothing else is running
        if (mainWindow.isRunning()) {
            GUI.errorAlert("Error", "Already Running", "Another measurement is already running!");
            return;
        }

        ResultTable results = null;

        try {

            // Disabled all the text-boxes etc
            disableInputs(true);

            // Get the instruments that have been configured on the config tabs
            SMU                thermoVoltage   = mainWindow.smuConfigTab.tvSMU.getSMU();
            SMU                hotGateVoltage  = mainWindow.smuConfigTab.hotGateSMU.getSMU();
            SMU                coldGateVoltage = mainWindow.smuConfigTab.coldGateSMU.getSMU();
            SMU                heaterVoltage   = mainWindow.smuConfigTab.heaterSMU.getSMU();
            TC                 stageTemp       = mainWindow.tcConfigTab.stage.getTController();
            LinkedList<String> errors          = new LinkedList<>();

            // Check that everything is present and configured
            if (thermoVoltage == null) {
                errors.add("Thermo-Voltage SMU is not configured.");
            }

            if (hotGateVoltage == null) {
                errors.add("Hot-Gate SMU is not configured.");
            }

            if (coldGateVoltage == null) {
                errors.add("Cold-Gate SMU is not configured.");
            }

            if (heaterVoltage == null) {
                errors.add("Heater SMU is not configured.");
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

            // Create a new measurement object using our instruments
            measurement = new GatedTEM(thermoVoltage, hotGateVoltage, coldGateVoltage, heaterVoltage, stageTemp);

            // Configure experiment parameters using values in fields
            measurement.configureGate(gateStart.get(), gateStop.get(), gateSteps.get())
                       .configureHeater(heaterStart.get(), heaterStop.get(), heaterSteps.get())
                       .configureTiming(gateTime.get(), heaterTime.get(), intTime.get());

            // Stream results directly to file
            results = measurement.newResults(outputFile.get());

            configurePlots(results);

            // Do the actual measurement now that everything's ready
            measurement.performMeasurement();

            // Check whether it finished because "stop" was pressed or it completing fully
            if (measurement.wasStopped()) {
                GUI.warningAlert("Stopped", "Measurement Stopped", "The measurement was stopped before completion.");
            } else {
                GUI.infoAlert("Complete", "Measurement Completed", "The measurement completed without error.");
            }

        } catch (Exception e) {

            // If something went wrong, output to terminal and show error alert.
            e.printStackTrace();
            GUI.errorAlert("Error", "Exception Encountered", e.getMessage());

        } finally {

            // If we actually got some results, then finalise the table (closes the file)
            if (results != null) {
                results.finalise();
            }

            // Re-enable all the text boxes
            disableInputs(false);
        }

    }

    /**
     * Configures the plots on the tab to display the live results of a new measurement
     *
     * @param results The results object of the new measurement
     */
    private void configurePlots(ResultTable results) {

        heaterPlot.clear();
        heaterPlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_HEATER_POWER, "Heater", Color.TEAL);

        gatePlot.clear();
        gatePlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_GATE_VOLTAGE, r -> r.get(GatedTEM.COL_GATE_CONFIG) == 0.0, "Hot-Gate", Color.ORANGERED);
        gatePlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_GATE_VOLTAGE, r -> r.get(GatedTEM.COL_GATE_CONFIG) == 1.0, "Cold-Gate", Color.CORNFLOWERBLUE);


        thermalPlot.clear();
        thermalPlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_THERMO_VOLTAGE, "Thermo-Voltage", Color.PURPLE);

        tpPlot.clear();
        tpPlot.watchListSplit(results, GatedTEM.COL_HEATER_POWER, GatedTEM.COL_THERMO_VOLTAGE, GatedTEM.COL_GATE_SET_VOLTAGE, r -> r.get(GatedTEM.COL_GATE_CONFIG) == 0.0);
        tpPlot.watchListSplit(results, GatedTEM.COL_HEATER_POWER, GatedTEM.COL_THERMO_VOLTAGE, GatedTEM.COL_GATE_SET_VOLTAGE, r -> r.get(GatedTEM.COL_GATE_CONFIG) == 1.0);

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
