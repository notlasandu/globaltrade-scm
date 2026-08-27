package com.globaltrade.client;

import javax.naming.Context;

public interface SimulationActor {
    boolean authenticate(Context jndiContext);
    void execute(Context jndiContext) throws Exception;
}
