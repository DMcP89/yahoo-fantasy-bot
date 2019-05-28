package worker;

import utils.CronInterpreter;
import utils.JobRunner;
import utils.ServicesHandler;
import utils.Yahoo;

public class Bot {
    public static void start() {
        // Keep trying to get the latest token
        Yahoo.authenticate();
//
//        // Check services for validity
        ServicesHandler.startupCheck();

        // Interpret Cron strings
        CronInterpreter.interpret();

        // Run jobs
        JobRunner.runJobs();
    }
}
