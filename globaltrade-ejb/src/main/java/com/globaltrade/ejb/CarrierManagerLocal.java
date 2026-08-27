package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CarrierManagerLocal {
    List<Order> getManifest();
    void updateTransitStatus(Long orderId, String eventCode);
}
