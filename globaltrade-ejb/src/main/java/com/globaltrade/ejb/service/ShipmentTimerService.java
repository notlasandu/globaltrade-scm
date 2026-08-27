package com.globaltrade.ejb.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import java.util.Date;
import java.util.logging.Logger;

@Stateless
public class ShipmentTimerService {
    private static final Logger logger = Logger.getLogger(ShipmentTimerService.class.getName());

    @Resource
    private TimerService timerService;

    public void scheduleShipmentUpdate(Long shipmentId, Date checkDate) {
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(shipmentId);
        timerConfig.setPersistent(true);
        timerService.createSingleActionTimer(checkDate, timerConfig);
        logger.info("Scheduled shipment update for shipment: " + shipmentId + " at " + checkDate);
    }
}
