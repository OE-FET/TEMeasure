package TEMeasure.Measurement;

import JISA.Devices.DeviceException;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Experiment.ResultList;
import JISA.Experiment.ResultStream;
import JISA.Experiment.ResultTable;
import JISA.GUI.SMUConfig;
import JISA.Util;

import java.io.IOException;

public class RTCalibration {

    private boolean running = false;

    private SMU         heater;
    private SMU         rt;
    private TC stageTC;

    private int sweeps;

    private double heaterStart;
    private double heaterStop;
    private int    heaterSteps;

    private double rtStart;
    private double rtStop;
    private int    rtSteps;

    private double intTime;
    private int    delTime;
    private int    heaterDelay;

    private int currentStep = 0;
    private int numSteps    = sweeps * heaterSteps * rtSteps;

    private ResultTable results;

    public static final String[] COLUMNS = {"No.", "Sweep No.", "Stage Temperature", "Heater Voltage", "Heater Current", "Heater Power", "RT Voltage", "RT Current", "RT Resistance"};
    public static final String[] UNITS   = {"~", "~", "K", "V", "A", "W", "V", "A", "Ohms"};

    public static final int COL_NUMBER            = 0;
    public static final int COL_SWEEP             = 1;
    public static final int COL_STAGE_TEMPERATURE = 2;
    public static final int COL_HEATER_VOLTAGE    = 3;
    public static final int COL_HEATER_CURRENT    = 4;
    public static final int COL_HEATER_POWER      = 5;
    public static final int COL_RT_VOLTAGE        = 6;
    public static final int COL_RT_CURRENT        = 7;
    public static final int COL_RT_RESISTANCE     = 8;

    public RTCalibration(SMU heaterSMU, SMU rtSMU, TC stageTC) {
        heater = heaterSMU;
        rt = rtSMU;
        stageTC = stageTC;
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

    public RTCalibration configureSweeps(int numSweeps) {
        this.sweeps = numSweeps;
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
    public RTCalibration configureHeater(double start, double stop, int steps) {
        heaterStart = start;
        heaterStop = stop;
        heaterSteps = steps;
        return this;
    }

    /**
     * Configures the steps to take in RT-Current for four-point-probe resistance measurements.
     *
     * @param start Start current, in Amps
     * @param stop  Stop current, in Amps
     * @param steps Number of steps
     *
     * @return Self-reference, for chaining
     */
    public RTCalibration configureRT(double start, double stop, int steps) {
        rtStart = start;
        rtStop = stop;
        rtSteps = steps;
        return this;
    }

    /**
     * Configures the timing parameters of the measurement.
     *
     * @param heaterHold      Time to hold for after changing heater voltage, in seconds (ms resolution)
     * @param delayTime       Time to hold for before taking resistance measurements, in seconds (ms resolution)
     * @param integrationTime Integration time for resistance measurements, in seconds
     *
     * @return Self-reference, for chaining
     */
    public RTCalibration configureTiming(double heaterHold, double delayTime, double integrationTime) {
        heaterDelay = (int) (heaterHold * 1000);
        delTime = (int) (delayTime * 1000);
        intTime = integrationTime;
        return this;
    }

    public void performMeasurement() throws IOException, DeviceException {

        try {

            running = true;
            currentStep = 0;
            numSteps = sweeps * heaterSteps * rtSteps;

            double[] heaters  = Util.makeLinearArray(heaterStart, heaterStop, heaterSteps);
            double[] currents = Util.makeLinearArray(rtStart, rtStop, rtSteps);

            heater.turnOff();
            rt.turnOff();

            heater.setSource(SMU.Source.VOLTAGE);
            heater.useAutoRanges();
            heater.useFourProbe(false);

            rt.setSource(SMU.Source.CURRENT);
            rt.useAutoRanges();
            rt.useFourProbe(true);
            rt.setIntegrationTime(intTime);

            mainLoop:
            for (int sweep = 0; sweep < sweeps; sweep++) {

                heater.setVoltage(heaterStart);
                heater.turnOn();

                for (double H : heaters) {

                    heater.setVoltage(H);
                    Util.sleep(heaterDelay);

                    rt.setCurrent(rtStart);
                    rt.turnOn();
                    for (double I : currents) {

                        rt.setCurrent(I);
                        Util.sleep(delTime);

                        double heaterVoltage = heater.getVoltage();
                        double heaterCurrent = heater.getCurrent();
                        double heaterPower   = heaterVoltage * heaterCurrent;

                        double rtVoltage    = rt.getVoltage();
                        double rtCurrent    = rt.getCurrent();
                        double rtResistance = rtCurrent / rtVoltage;

                        results.addData(
                                (double) currentStep,
                                (double) sweep,
                                stageTC.getTemperature(),
                                heaterVoltage,
                                heaterCurrent,
                                heaterPower,
                                rtVoltage,
                                rtCurrent,
                                rtResistance
                        );


                        currentStep++;

                        if (!running) {
                            break mainLoop;
                        }

                    }
                    rt.turnOff();

                }

                heater.turnOff();
                Util.sleep(heaterDelay);

            }

        } finally {

            running = false;
            try {
                heater.turnOff();
                rt.turnOff();
            } catch (Exception ignored) {
            }

        }


    }

    public double getPercentageComplete() {
        return 100D * ((double) currentStep) / ((double) numSteps);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }

}
