package com.terminal.controller;

import com.terminal.model.Route;
import com.terminal.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/routes")
@CrossOrigin("*")
public class RouteController {

    @Autowired
    private RouteRepository routeRepository;

    @PostMapping
    public Object createRoute(@RequestBody Route route) {
        if (route.getOrigin() == null || route.getOrigin().isBlank())
            return "El origen es obligatorio";
        if (route.getDestination() == null || route.getDestination().isBlank())
            return "El destino es obligatorio";
        if (route.getSchedule() == null || route.getSchedule().isBlank())
            return "El horario es obligatorio";
        if (route.getPrice() == null || route.getPrice() <= 0)
            return "El precio debe ser mayor que cero";
        if (route.getCapacity() == null || route.getCapacity() <= 0)
            return "La capacidad debe ser mayor que cero";

        route.setAvailableSeats(route.getCapacity());
        return routeRepository.save(route);
    }

    @PutMapping("/{id}")
    public Object updateRoute(@PathVariable Long id, @RequestBody Route routeData) {
        Optional<Route> optional = routeRepository.findById(id);
        if (optional.isEmpty())
            return ResponseEntity.notFound().build();

        if (routeData.getOrigin() == null || routeData.getOrigin().isBlank())
            return "El origen es obligatorio";
        if (routeData.getDestination() == null || routeData.getDestination().isBlank())
            return "El destino es obligatorio";
        if (routeData.getSchedule() == null || routeData.getSchedule().isBlank())
            return "El horario es obligatorio";
        if (routeData.getPrice() == null || routeData.getPrice() <= 0)
            return "El precio debe ser mayor que cero";
        if (routeData.getCapacity() == null || routeData.getCapacity() <= 0)
            return "La capacidad debe ser mayor que cero";

        Route route = optional.get();
        route.setOrigin(routeData.getOrigin());
        route.setDestination(routeData.getDestination());
        route.setSchedule(routeData.getSchedule());
        route.setPrice(routeData.getPrice());
        route.setCapacity(routeData.getCapacity());

        return routeRepository.save(route);
    }

    @DeleteMapping("/{id}")
    public Object deleteRoute(@PathVariable Long id) {
        if (!routeRepository.existsById(id))
            return ResponseEntity.notFound().build();
        routeRepository.deleteById(id);
        return ResponseEntity.ok().body("Ruta eliminada correctamente");
    }

    @GetMapping
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }
}