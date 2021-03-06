package org.cdbbnny.ftc_utils.autonomous;
//Note: Use Ctrl+Alt+O to organize and remove unused (grayed out) imports.
//Guys, you should read ALL the comments! They contain some useful observations about the software!
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.cdbbnny.ftc_utils.Config;
import org.cdbbnny.ftc_utils.Logger;
import org.cdbbnny.ftc_utils.Persistent;
import org.cdbbnny.ftc_utils.autonomous.tasks.Task;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Base autonomous OpMode. Sub-OpModes that are going to be used for the games must extend this.
 *
 * ## Tip ##
 *   * If you have any sub-methods in your OpModes/Tasks that you expect to wait or run for a long
 *     time (>0.1 second), please declare them as 'throws InterruptedException'. If you're not sure,
 *     just make it throw InterruptedException. This makes it so that if the robot tries to stop
 *     (e.g. because the 30-second autonomous period has ended or the stop button has been pressed),
 *     it stops instead of crashing with a 'The robot is stuck in stop()' message and restarting the
 *     robot. **Important**: If you have a while loop, such as this:
 *
 *       while (condition) {
 *           //Stuff
 *       }
 *
 *     you need to put another condition so that it will stop when the thread is interrupted, like
 *     this:
 *
 *       while (condition && !Thread.interrupted()) {
 *           //Stuff
 *       }
 *
 *     This way you can avoid 'stuck in stop()' errors.
 */
//The @Autonomous annotation is required for all OpModes. This puts it in the list of autonomous
//programs. You should specify a name for each OpMode.
//If you want to disable any OpModes you have, you should include the @Disabled annotation.
//If your OpMode is missing from the list of OpModes, the case is probably that you forgot to mark
//it as @Autonomous.
//This one won't be listed since it's not a functional OpMode, just a base.
// @Autonomous(name = "Autonomous")
public abstract class BaseAutonomous extends LinearOpMode {

    //Queue of tasks to run
    protected final List<Task> tasks = new ArrayList<>();
    //We're making BaseAutonomous a 'singleton' class. This means that there is always only ONE
    //instance in use at a time. This is stored in this static field, which can be retrieved by
    //other code without having to pass an instance to all of the methods that want to use it.
    private static BaseAutonomous instance;

    public Config config;

    /**
     * Get the current instance of BaseAutonomous. This is set when the OpMode is initialized and
     * unset when it is stopped.
     * @return An instance of BaseAutonomous
     */
    public static final BaseAutonomous instance() {
        return instance;
    }
    public static final boolean instantated() {
        return instance != null;
    }


    /**
     * Run the op mode.
     * ## New programmers ##
     *   * Declaring a method as 'final' will make it so that it cannot be overridden by the class
     *     that implements it.
     * @throws InterruptedException If the OpMode is trying to stop (e.g. by the stop button or 30-
     * second timeout)
     */
    @Override
    public final void runOpMode() throws InterruptedException {
        Logger log = null;
        //Catch any InterruptedExceptions or other unchecked exceptions so that we can unset the
        //instance in case of an error.
        Throwable exc = null;
        try {
            //Run initialization operations here
            //Create our latest.log file
            Logger.init(new File(Config.storageDir + "logs/" + new SimpleDateFormat
                    ("yy_MM_dd_HH_mm_ss", Locale.US).format(new Date()) + ".log"));
            log = new Logger("BaseAutonomous");
            //Initialize the configuration file
            config = new Config(Config.configFile);

            //Clear the task list in case the robot was stopped before the list was empty
            //and the OpMode wasn't re-initialized.
            tasks.clear();

            //Clear the persistent objects since this would be a new round in competition
            Persistent.clear();

            //Set the current instance
            instance = this;

            initialize();

            //Must wait for start, otherwise the robot will run as soon as it is initialized, which can
            //be incredibly annoying. We could also simply override start(), but we also want to
            //initialize stuff, so it makes it simpler to use one method.
            waitForStart();
            if (!opModeIsActive()) return;
            Logger.startTimer();
            //Run the tasks
            run();
            //Run any leftover tasks
            runTasks();
            log.i("Finished all tasks");
            while (opModeIsActive()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException | RuntimeException | IOException | Error e) {
            exc = e;
        } finally {
            finish();
            instance = null;
            if (exc != null) {
                //We can't just throw any Throwables; we need to throw either unchecked exceptions
                //(RuntimeException and Error) or InterruptedException, which it is declared to be
                //able to throw.
                if (log != null) log.e(exc);
                if (exc instanceof Error) throw (Error)exc;
                else if (exc instanceof RuntimeException) throw (RuntimeException)exc;
                else if (exc instanceof IOException) log.e(exc);
                else throw (InterruptedException)exc;
            }
            Logger.close();
        }
    }

    /**
     * Executed when the robot is initializing. Implementation is optional; does nothing by default.
     * @throws InterruptedException if the OpMode is trying to stop
     */
    public void initialize() throws InterruptedException {}

    /**
     * Executed when the 'play' button is pressed. Implementation is required. Main tasks should be
     * added to the list here.
     * @throws InterruptedException if the OpMode is trying to stop
     */
    public abstract void run() throws InterruptedException;

    /**
     * Executed after all tasks have completed. Does nothing by default.
     * @throws InterruptedException if the OpMode is trying to stop
     */
    public void finish() throws InterruptedException { }

    /**
     * Use this method to run all of the tasks currently in the list. This is executed after run()
     * so that any tasks left in the queue are run. If you don't want this behavior, you should
     * clear the task list at the end of the method. This method will remove all of the tasks from
     * the list. Run this before any decisions about the state of the robot between tasks are made,
     * since nothing actually happens when the tasks are added to the list.
     * @throws InterruptedException if the OpMode is trying to stop
     */
    public final void runTasks() throws InterruptedException {
        while (!tasks.isEmpty()) {
            //Possibly add telemetry showing which task is running?
            Task t = tasks.remove(0);
            t.runTask();
        }
    }
}
