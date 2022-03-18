package com.graphhopper.routing.util;

import com.graphhopper.util.PMap;



public class BusFlagEncoder extends CarFlagEncoder
{
    public BusFlagEncoder()
    {
        this(5, 5, 1);
    }

    public BusFlagEncoder(PMap properties)
    {
        this(
                (int) properties.getLong("speed_bits", 5),
                properties.getDouble("speed_factor", 5),
                properties.getBool("turn_costs", false) ? 1 : 0
        );
        //this.properties = properties;
        //this.setBlockFords(properties.getBool("block_fords", true));
        blockFords(properties.getBool("block_fords", true));
    }


    public BusFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts)
    {
        super(speedBits, speedFactor, maxTurnCosts);

        restrictions.remove("motorcar");
        restrictions.add("psv");
        restrictions.add("bus");

        restrictedValues.remove("no");
        restrictedValues.remove("private");

        blockByDefaultBarriers.remove("bus_trap");
        blockByDefaultBarriers.remove("sump_buster");


    }

    @Override
    public String toString()
    {
        return "bus";
    }
}