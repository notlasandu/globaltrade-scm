package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import jakarta.ejb.Remote;
import java.util.List;

@Remote
public interface CarrierManagerRemote {
    List<String> getManifest();
    void updateTransitStatus(String trackingNumber, String eventCode);
    String issueTrackingNumber(String prefix);
}
