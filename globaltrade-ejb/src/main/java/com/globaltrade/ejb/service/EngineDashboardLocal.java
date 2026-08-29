package com.globaltrade.ejb.service;

import jakarta.ejb.Local;

@Local
public interface EngineDashboardLocal {
    EngineDashboardDTO getDashboardData();
}
