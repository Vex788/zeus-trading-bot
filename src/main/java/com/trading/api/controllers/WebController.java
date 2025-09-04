package com.trading.api.controllers;

import com.trading.core.engine.TradingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for serving HTML pages
 */
@Controller
public class WebController {
    
    @Autowired
    private TradingEngine tradingEngine;
    
    /**
     * Main dashboard page
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("botStatus", tradingEngine.isRunning() ? "RUNNING" : "STOPPED");
        model.addAttribute("botMode", tradingEngine.getCurrentMode().toString());
        model.addAttribute("isPaused", tradingEngine.isPaused());
        return "dashboard";
    }
    
    /**
     * Configuration page
     */
    @GetMapping("/config")
    public String configuration(Model model) {
        model.addAttribute("currentMode", tradingEngine.getCurrentMode().toString());
        return "config";
    }
    
    /**
     * Analytics page
     */
    @GetMapping("/analytics")
    public String analytics(Model model) {
        return "analytics";
    }
    
    /**
     * Trading history page
     */
    @GetMapping("/history")
    public String history(Model model) {
        return "history";
    }
}