package com.letitbee.diamondvaluationsystem.controller;

import com.letitbee.diamondvaluationsystem.enums.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("api/v1/field-diamonds")
public class FieldDiamondController {

    @GetMapping("/claritys")
    public Map<String, String> getClarities() {
        Map<String, String> clarities = new HashMap<>();
        for (Clarity clarity : Clarity.values()) {
            clarities.put(clarity.name(), clarity.getDisplayName());
        }
        return clarities;
    }

    @GetMapping("/colors")
    public Map<String, String> getColors() {
        Map<String, String> colors = new HashMap<>();
        for (Color color : Color.values()) {
            colors.put(color.name(), color.getDisplayName());
        }
        return colors;
    }

    @GetMapping("/cuts")
    public Map<String, String> getCuts() {
        Map<String, String> cuts = new HashMap<>();
        for (Cut cut : Cut.values()) {
            cuts.put(cut.name(), cut.getDisplayName());
        }
        return cuts;
    }

    @GetMapping("/diamond-origins")
    public Map<String, String> getDiamondOrigins() {
        Map<String, String> diamondOrigins = new HashMap<>();
        for (DiamondOrigin diamondOrigin : DiamondOrigin.values()) {
            diamondOrigins.put(diamondOrigin.name(), diamondOrigin.getDisplayName());
        }
        return diamondOrigins;
    }

    @GetMapping("/shapes")
    public Map<String, String> getShapes() {
        Map<String, String> shapes = new HashMap<>();
        for (Shape shape : Shape.values()) {
            shapes.put(shape.name(), shape.getDisplayName());
        }
        return shapes;
    }

    @GetMapping("/polishs")
    public Map<String, String> getPolishs() {
        Map<String, String> polishs = new HashMap<>();
        for (Polish polish : Polish.values()) {
            polishs.put(polish.name(), polish.getDisplayName());
        }
        return polishs;
    }

    @GetMapping("/symmetries")
    public Map<String, String> getSymmetries() {
        Map<String, String> symmetries = new HashMap<>();
        for (Symmetry symmetry : Symmetry.values()) {
            symmetries.put(symmetry.name(), symmetry.getDisplayName());
        }
        return symmetries;
    }

    @GetMapping("/fluorescences")
    public Set<Fluorescence> getFluorescences() {
        Set<Fluorescence> fluorescences = new HashSet<>();
        for (Fluorescence fluorescence : Fluorescence.values()) {
            fluorescences.add(fluorescence);
        }
        return fluorescences;
    }

}