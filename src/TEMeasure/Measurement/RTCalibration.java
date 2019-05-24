package TEMeasure.Measurement;

import JISA.Devices.DeviceException;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Enums.Source;
import JISA.Experiment.Measurement;
import JISA.Experiment.ResultTable;
import JISA.Util;

import java.io.IOException;

public class RTCalibration extends Measurement {

    public static final String[] COLUMNS               = {"No.", "Sweep No.", "Sample Temperature", "Heater Voltage", "Heater Current", "Heater Power", "RT Voltage", "RT Current", "RT Resistance"};
    public static final String[] UNITS                 = {"~", "~", "K", "V", "A", "W", "V", "A", "Ohms"};
    public static final int      COL_NUMBER            = 0;
    public static final int      COL_SWEEP             = 1;
    public static final int      COL_STAGE_TEMPERATURE = 2;
    public static final int      COL_HEATER_VOLTAGE    = 3;
    public static final int      COL_HEATER_CURRENT    = 4;
    public static final int      COL_HEATER_POWER      = 5;
    public static final int      COL_RT_VOLTAGE        = 6;
    public static final int      COL_RT_CURRENT        = 7;
    public static final int      COL_RT_RESISTANCE     = 8;
    private             SMU      heater;
    private             SMU      rt;
    private             TC       stageTC;
    private             int      sweeps;
    private             double   heaterStart;
    private             double   heaterStop;
    private             int      heaterSteps;
    private             double   rtStart;
    private             double   rtStop;
    private             int      rtSteps;
    private             double   intTime;
    private             int      delTime;
    private             int      heaterDelay;

    public RTCalibration(SMU heaterSMU, SMU rtSMU, TC stageTC) {
        heater = heaterSMU;
        rt = rtSMU;
        this.stageTC = stageTC;
    }

    @Override
    public void run() throws Exception {

        ResultTable results = getResults();

        // Create arrays of voltages and currents that we will use
        double[] heaters  = Util.makeLinearArray(heaterStart, heaterStop, heaterSteps);
        double[] currents = Util.makeLinearArray(rtStart, rtStop, rtSteps);

        // Make sure everything's off first
        heater.turnOff();
        rt.turnOff();

        // Configure heater SMU
        heater.setSource(Source.VOLTAGE);
        heater.useAutoRanges();
        heater.useFourProbe(false);

        // Configure RT SMU
        rt.setSource(Source.CURRENT);
        rt.useAutoRanges();
        rt.useFourProbe(true);
        rt.setIntegrationTime(intTime);

        int currentStep = 0;

        for (int sweep = 0; sweep < sweeps; sweep++) {

            // Initial value
            heater.setVoltage(heaterStart);
            heater.turnOn();

            for (double H : heaters) {

                // Set heater voltage and wait heater hold time
                heater.setVoltage(H);
                sleep(heaterDelay);

                // Initial value for rt
                rt.setCurrent(rtStart);
                rt.turnOn();
                for (double I : currents) {

                    // Set current and wait for current hold time
                    rt.setCurrent(I);
                    sleep(delTime);

                    // Calculate heater power
                    double heaterVoltage = heater.getVoltage();
                    double heaterCurrent = heater.getCurrent();
                    double heaterPower   = heaterVoltage * heaterCurrent;

                    // Calculate RT resistance (assuming 0 y-intercept of V vs I)
                    double rtVoltage    = rt.getVoltage();
                    double rtCurrent    = rt.getCurrent();
                    double rtResistance = rtVoltage / rtCurrent;

                    // Add data point to results
                    results.addData(
                            (double) currentStep,      // Measurement number
                            (double) sweep,            // Sweep number
                            stageTC.getTemperature(),  // Sample temperature
                            heaterVoltage,             // Heater voltage
                            heaterCurrent,             // Heater current
                            heaterPower,               // Heater power
                            rtVoltage,                 // RT voltage
                            rtCurrent,                 // RT current
                            rtResistance               // RT resistance
                    );

                    // Increment measurement number by 1
                    currentStep++;

                }

                // Turn off current through RT
                rt.turnOff();

            }

            // Turn off heater and wait for heater hold time
            heater.turnOff();
            sleep(heaterDelay);

        }

    }

    @Override
    public void onInterrupt() throws Exception {

    }

    @Override
    public void onFinish() throws Exception {
        heater.turnOff();
        rt.turnOff();
    }

    @Override
    public String[] getColumns() {
        return COLUMNS;
    }

    @Override
    public String[] getUnits() {
        return UNITS;
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

}
