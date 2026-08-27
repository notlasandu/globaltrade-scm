package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import jakarta.ejb.Remote;
import java.util.List;

@Remote
public interface CarrierManagerRemote {
    List<Order> getManifest();
    void updateTransitStatus(Long orderId, String eventCode);
}
