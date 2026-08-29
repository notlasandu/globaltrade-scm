package com.globaltrade.ejb.service;

import jakarta.ejb.Remote;

@Remote
public interface EngineDashboardRemote {
    EngineDashboardDTO getDashboardData();
}
