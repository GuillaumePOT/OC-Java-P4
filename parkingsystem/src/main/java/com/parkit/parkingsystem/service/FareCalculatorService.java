package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean isDiscount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }
        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();
        double durationInMinute;
        durationInMinute =(double)(outHour - inHour) / 60 / 1000;
        double duration = durationInMinute / 60;
        if (durationInMinute <= 30) {
            ticket.setPrice(0);
        } else {
            final double discount = isDiscount ? 0.95 : 1;
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR:
                    ticket.setPrice(Fare.CAR_RATE_PER_HOUR * duration  * discount);
                    break;
                case BIKE:
                    ticket.setPrice(Fare.BIKE_RATE_PER_HOUR * duration * discount);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown Parking Type");
            }
        }
    }

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}