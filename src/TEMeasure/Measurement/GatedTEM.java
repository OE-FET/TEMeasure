package TEMeasure.Measurement;

import JISA.Devices.DeviceException;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Experiment.ResultList;
import JISA.Experiment.ResultStream;
import JISA.Experiment.ResultTable;
import JISA.Util;

import java.io.IOException;

public class GatedTEM {

    private boolean running   = false;
    private boolean stopped   = false;
    private Thread  runThread = Thread.currentThread();

    private SMU thermoVoltage;
    private SMU hotGate;
    private SMU coldGate;
    private SMU heater;
    private TC  stage;

    // Parameters, with default values
    private double gateStart   = -40;         // -40 Volts
    private double gateStop    = 0;           //   0 Volts
    private int    gateSteps   = 9;           //   9 Steps
    private double heaterStart = 0;           //   0 Volts
    private double heaterStop  = 5;           //   5 Volts
    private int    heaterSteps = 6;           //   6 Steps
    private int    gateDelay   = 20000;       //  20 seconds
    private int    heaterDelay = 10000;       //  10 seconds
    private double intTime     = 10.0 / 50.0; //  10 power-line cycles

    private int         currentStep = 0;
    private int         numSteps    = gateSteps * heaterSteps;
    private ResultTable results;

    // Names and units for columns in our results
    public static final String[] COLUMNS = {"No.", "Sample Temperature", "Gate Voltage", "Gate Current", "Heater Voltage", "Heater Current", "Heater Power", "Thermo-Voltage", "Gate Set", "Gate Config", "Thermo-Current"};
    public static final String[] UNITS   = {"~", "K", "V", "A", "V", "A", "W", "V", "V", "~", "A"};

    // Constants to define what each column in our results is meant to be
    public static final int COL_NUMBER             = 0;  // Measurement Number
    public static final int COL_SAMPLE_TEMPERATURE = 1;  // Sample Temperature
    public static final int COL_GATE_VOLTAGE       = 2;  // Gate Voltage
    public static final int COL_GATE_CURRENT       = 3;  // Gate Leakage Current
    public static final int COL_HEATER_VOLTAGE     = 4;  // Heater Voltage
    public static final int COL_HEATER_CURRENT     = 5;  // Heater Current
    public static final int COL_HEATER_POWER       = 6;  // Heater Power
    public static final int COL_THERMO_VOLTAGE     = 7;  // Thermo-Voltage
    public static final int COL_GATE_SET_VOLTAGE   = 8;  // Gate Voltage Set-Point
    public static final int COL_GATE_CONFIG        = 9;  // Gate Configuration (0=hot-gate, 1=cold-gate)
    public static final int COL_THERMO_CURRENT     = 10; // Current between hot and cold contacts

    public GatedTEM(SMU thermoVoltageSMU, SMU hotGateSMU, SMU coldGateSMU, SMU heaterSMU, TC stageController) {
        thermoVoltage = thermoVoltageSMU;
        hotGate = hotGateSMU;
        coldGate = coldGateSMU;
        heater = heaterSMU;
        stage = stageController;
    }

    /**
     * Creates a new results table for measurements, stored in memory.
     *
     * @return New ResultTable
     */
    public ResultTable newResults() {
        results = new ResultList(COLUMNS);
        results.setUnits(UNITS);
        return results;
    }

    /**
     * Creates a new results table for measurements that streams directly to a file.
     *
     * @param path File path to stream to
     *
     * @return New ResultTable
     *
     * @throws IOException Upon error opening specified file for writing
     */
    public ResultTable newResults(String path) throws IOException {
        results = new ResultStream(path, COLUMNS);
        results.setUnits(UNITS);
        return results;
    }

    /**
     * Results the current results table being used by the measurement.
     *
     * @return Current ResultTable
     */
    public ResultTable getResults() {
        return results;
    }

    /**
     * Configures the steps to take in gate voltage.
     *
     * @param start Start voltage, in Volts
     * @param stop  End voltage, in Volts
     * @param steps Number of steps
     *
     * @return Self-reference, for chaining
     */
    public GatedTEM configureGate(double start, double stop, int steps) {
        gateStart = start;
        gateStop = stop;
        gateSteps = steps;
        return this;
    }

    /**
     * Configures the steps to take in heater voltage.
     *
     * @param start Start voltage, in Volts
     * @param stop  End voltage, in Volts
     * @param steps Number of steps
     *
     * @return Self-reference, for chaining
     */
    public GatedTEM configureHeater(double start, double stop, int steps) {
        heaterStart = start;
        heaterStop = stop;
        heaterSteps = steps;
        return this;
    }

    /**
     * Configures the timing parameters of the measurement.
     *
     * @param gateHold        Time to hold for after changing the gate voltage, in seconds (1 ms resolution)
     * @param heaterHold      Time to hold for after changing heater voltage, in seconds (1 ms resolution)
     * @param integrationTime Integration time for the thermo-voltage measurement, in seconds
     *
     * @return Self-reference, for chaining
     */
    public GatedTEM configureTiming(double gateHold, double heaterHold, double integrationTime) {
        gateDelay = (int) (gateHold * 1000);        // Convert to milliseconds
        heaterDelay = (int) (heaterHold * 1000);    // Convert to milliseconds
        intTime = integrationTime;
        return this;
    }

    /**
     * Perform the measurement, using the configured parameters.
     *
     * @throws IOException     Upon communications error with an instrument
     * @throws DeviceException Upon incompatibility error with an instrument
     */
    public void performMeasurement() throws IOException, DeviceException {

        // Reset the stopped flag
        stopped = false;

        // We are now running
        running = true;

        // Hold on to which thread is currently running this measurement so we can interrupt it
        runThread = Thread.currentThread();

        try {

            // This variable is used to count which measurement number we are currently on
            currentStep = 0;

            // Total number of measurements that will be taken
            numSteps = gateSteps * heaterSteps;

            // Make sure outputs are disabled to begin with
            thermoVoltage.turnOff();
            hotGate.turnOff();
            coldGate.turnOff();
            heater.turnOff();

            thermoVoltage.useFourProbe(false);
            hotGate.useFourProbe(false);
            coldGate.useFourProbe(false);
            heater.useFourProbe(false);

            // Set integration time of thermo-voltage smu
            thermoVoltage.setIntegrationTime(intTime);
            thermoVoltage.useAutoRanges();
            thermoVoltage.setCurrent(0.0);
            thermoVoltage.setVoltageLimit(200.0);

            // Configure gate SMUs
            hotGate.useAutoRanges();
            hotGate.setVoltage(gateStart);
            hotGate.setOffMode(SMU.OffMode.HIGH_IMPEDANCE);
            coldGate.useAutoRanges();
            coldGate.setVoltage(gateStart);
            coldGate.setOffMode(SMU.OffMode.HIGH_IMPEDANCE);

            // Configure heater SMU
            heater.useAutoRanges();
            heater.setVoltage(heaterStart);

            // Create arrays of voltage values to use for gate and heater voltages
            double[] gates   = Util.makeLinearArray(gateStart, gateStop, gateSteps);
            double[] heaters = Util.makeLinearArray(heaterStart, heaterStop, heaterSteps);

            // This number indicates whether we're using hot-gate (0) or cold-gate (1) in our data
            double config = 0;
            double factor = 1.0;

            // Loop over each configuration
            mainLoop:
            for (SMU gate : new SMU[]{hotGate, coldGate}) {

                // Turn on this gate and the thermo-voltage SMU
                thermoVoltage.turnOn();
                gate.turnOn();

                // If the running flag has been flipped back to false, then stop running
                if (!running) {
                    break mainLoop;
                }

                // Loop over each gate value we want to use
                for (double G : gates) {

                    // Set the gate voltage and wait our gate hold time
                    gate.setVoltage(factor * G);
                    Util.sleep(gateDelay);

                    if (!running) {
                        break mainLoop;
                    }

                    // Initial values
                    heater.setVoltage(heaterStart);
                    heater.turnOn();

                    for (double H : heaters) {

                        // Set the heater and wait our heater hold time
                        heater.setVoltage(H);
                        Util.sleep(heaterDelay);

                        if (!running) {
                            break mainLoop;
                        }

                        // Get the heater current and voltage to calculate power
                        double heaterVoltage = heater.getVoltage();
                        double heaterCurrent = heater.getCurrent();
                        double heaterPower   = heaterVoltage * heaterCurrent;

                        // Add data-point to our results
                        results.addData(
                                (double) currentStep,        // Measurement number
                                stage.getTemperature(),      // Sample temperature
                                gate.getVoltage() * factor,  // Gate voltage
                                gate.getCurrent() * factor,  // Gate leakage current
                                heaterVoltage,               // Heater voltage
                                heaterCurrent,               // Heater current
                                heaterPower,                 // Heater power
                                thermoVoltage.getVoltage(),  // Thermo-voltage
                                G,                           // Gate set-point
                                config,                      // Hot-Gate (0) or Cold-Gate (1) ?
                                thermoVoltage.getCurrent()
                        );

                        currentStep++;

                    }

                    // Turn the heater off and wait our heater hold time
                    heater.turnOff();
                    Util.sleep(heaterDelay);

                    if (!running) {
                        break mainLoop;
                    }

                }

                // Reverse gate voltages for next iteration of gate loop
                for (int i = 0; i < gates.length / 2; i++) {
                    double temp = gates[i];
                    gates[i] = gates[gates.length - i - 1];
                    gates[gates.length - i - 1] = temp;
                }

                // Next iteration will be cold-gate config
                config = 1;
                factor = -1;

                // Turn off this gate before using the next
                gate.turnOff();
            }

        } finally { // This will always run even if the code inside the try{...} throws an exception

            running = false;

            // Make sure we try to turn everything off
            try {
                heater.turnOff();
                hotGate.turnOff();
                coldGate.turnOff();
                thermoVoltage.turnOff();
            } catch (Exception ignored) {
            }

        }

    }

    /**
     * Returns the current percentage completion of the on-going measurement.
     *
     * @return Perentage completion
     */
    public double getPercentageComplete() {
        return 100D * ((double) currentStep) / ((double) numSteps);
    }

    /**
     * Returns whether this measurement is currently running.
     *
     * @return Running?
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns whether this measurement was stopped before completion on its last run.
     *
     * @return Stopped early?
     */
    public boolean wasStopped() {
        return stopped;
    }

    /**
     * Stops the currently running measurement.
     */
    public void stop() {
        if (isRunning()) {
            running = false;
            stopped = true;
            runThread.interrupt();
        }
    }

}
