package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static FareCalculatorService fareCalculatorService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        final Ticket ticket = ticketDAO.getTicket("ABCDEF");
        final ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertEquals(1, ticket.getId());
        assertFalse(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExit() throws InterruptedException {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Ticket ticket = new Ticket();
        final int parkingSpotNumber = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        final ParkingSpot parkingSpot = new ParkingSpot(parkingSpotNumber, ParkingType.CAR, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(Date.from(LocalDateTime.now().minusHours(1).atZone(ZoneId.systemDefault()).toInstant()));
        ticketDAO.saveTicket(ticket);
        Thread.sleep(500);
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF");
        // check that the fare generated and out time are populated correctly in the database
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(),0.01);
        assertFalse(ticket.getParkingSpot().isAvailable());
    }

    @Test
    public void testParkingLotExitRecurringUser() throws InterruptedException {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Ticket oldTicket = new Ticket();
        final int parkingSpotNumber = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        final ParkingSpot parkingSpot = new ParkingSpot(parkingSpotNumber, ParkingType.CAR, false);
        oldTicket.setParkingSpot(parkingSpot);
        oldTicket.setVehicleRegNumber("ABCDEF");
        oldTicket.setInTime(Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
        oldTicket.setOutTime(Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
        oldTicket.setPrice(0);
        ticketDAO.saveTicket(oldTicket);
        oldTicket = ticketDAO.getTicket("ABCDEF");

        parkingService.processIncomingVehicle();
        Thread.sleep(1000);
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ticket.setOutTime(Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant()));
        fareCalculatorService.calculateFare(ticket,true);
        ticketDAO.updateTicket(ticket);
        assertEquals(0,oldTicket.getPrice());
        assertEquals(0.95 * Fare.CAR_RATE_PER_HOUR,ticket.getPrice(),0.001);
    }
}
