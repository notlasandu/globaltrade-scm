package com.globaltrade.ejb;

import com.globaltrade.ejb.exception.CarrierSystemOutageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CarrierTrackingSimulatorBeanTest {

    private final CarrierTrackingSimulatorBean simulator = new CarrierTrackingSimulatorBean();

    @ParameterizedTest
    @ValueSource(longs = {5L, 10L, 15L})
    public void checkShipmentStatus_should_throwCarrierSystemOutageException_when_orderIdIsMultipleOfFive(Long orderId) {
        Assertions.assertThrows(CarrierSystemOutageException.class, () -> {
            simulator.checkShipmentStatus(orderId);
        });
    }

    @ParameterizedTest
    @ValueSource(longs = {3L, 9L, 21L})
    public void checkShipmentStatus_should_returnDelayedAtCustoms_when_orderIdIsMultipleOfThree(Long orderId) {
        String result = simulator.checkShipmentStatus(orderId);
        Assertions.assertEquals("DELAYED_AT_CUSTOMS", result);
    }
}
