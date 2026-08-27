package com.globaltrade.ejb.service;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.logging.Logger;

@Singleton
@Startup
public class InventoryTimerService {
    private static final Logger logger = Logger.getLogger(InventoryTimerService.class.getName());

    @Schedule(hour = "*", minute = "*/15", persistent = false)
    public void monitorInventoryLevels() {
        logger.info("Monitoring inventory levels every 15 minutes...");
    }
}
