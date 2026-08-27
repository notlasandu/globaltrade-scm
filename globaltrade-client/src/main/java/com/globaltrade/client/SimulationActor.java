package com.globaltrade.client;

import javax.naming.Context;

public interface SimulationActor {
    void execute(Context jndiContext) throws Exception;
}
