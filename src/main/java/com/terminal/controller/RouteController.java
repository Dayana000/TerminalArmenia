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
    public Route createRoute(@RequestBody Route route) {
        return routeRepository.save(route);
    }

    @GetMapping
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }
}