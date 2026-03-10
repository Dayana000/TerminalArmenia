package com.terminal.controller;

import com.terminal.model.Route;
import com.terminal.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routes")
@CrossOrigin("*")
public class RouteController {

    @Autowired
    private RouteRepository routeRepository;

    @PostMapping
    public Object createRoute(@RequestBody Route route) {

        if (route.getOrigin() == null || route.getOrigin().isBlank()) {
            return "El origen es obligatorio";
        }

        if (route.getDestination() == null || route.getDestination().isBlank()) {
            return "El destino es obligatorio";
        }

        if (route.getSchedule() == null || route.getSchedule().isBlank()) {
            return "El horario es obligatorio";
        }

        if (route.getPrice() <= 0) {
            return "El precio debe ser mayor que cero";
        }

        if (route.getCapacity() <= 0) {
            return "La capacidad debe ser mayor que cero";
        }

        route.setAvailableSeats(route.getCapacity());

        return routeRepository.save(route);
    }

    @GetMapping
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }
}