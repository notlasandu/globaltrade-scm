package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CarrierManagerLocal {
    List<String> getManifest();
    void updateTransitStatus(String trackingNumber, String eventCode);
    String issueTrackingNumber(String prefix);
}
