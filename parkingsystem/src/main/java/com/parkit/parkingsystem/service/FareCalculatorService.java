package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        //TODO: Some tests are failing here. Need to check if this logic is correct
        double durationInMinute;
        durationInMinute = (outHour - inHour) / 60 / 1000;
        double duration = durationInMinute / 60;
        if (durationInMinute <= 30) {
            ticket.setPrice(0);
        } else {
            if (discount) {
                switch (ticket.getParkingSpot().getParkingType()) {
                    case CAR: {
                        ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR * 0.95);
                        break;
                    }
                    case BIKE: {
                        ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR * 0.95);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown Parking Type");
                }
            } else {
                switch (ticket.getParkingSpot().getParkingType()) {
                    case CAR: {
                        ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                        break;
                    }
                    case BIKE: {
                        ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown Parking Type");
                }
            }

        }
    }

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}