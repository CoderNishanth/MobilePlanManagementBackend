package com.examly.springapp.controller;

import com.examly.springapp.model.Plan;
import com.examly.springapp.model.User;
import com.examly.springapp.service.PlanService;
import com.examly.springapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserService userService;

    // Get all plans (public access)
    @GetMapping
    public ResponseEntity<List<Plan>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }



    // Create new plan (ADMIN or PLAN_MANAGER)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PLAN_MANAGER')")
    public ResponseEntity<?> createPlan(@RequestAttribute("userEmail") String email,
                                        @RequestBody Plan plan) {
        User manager = userService.findByEmail(email).orElseThrow();
        plan.setPlanManager(manager);  // associate plan manager
        Plan savedPlan = planService.createPlan(plan);
        return ResponseEntity.ok(savedPlan);
    }
    
    // Create new plan with explicit endpoint (ADMIN or PLAN_MANAGER)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','PLAN_MANAGER')")
    public ResponseEntity<?> createPlanExplicit(@RequestAttribute("userEmail") String email,
                                               @RequestBody Plan plan) {
        User manager = userService.findByEmail(email).orElseThrow();
        plan.setPlanManager(manager);  // associate plan manager
        Plan savedPlan = planService.createPlan(plan);
        return ResponseEntity.ok(savedPlan);
    }

    // Update existing plan (ADMIN or PLAN_MANAGER)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLAN_MANAGER')")
    public ResponseEntity<?> updatePlan(@PathVariable Long id,
                                        @RequestAttribute("userEmail") String email,
                                        @RequestBody Plan plan) {
        User manager = userService.findByEmail(email).orElseThrow();
        plan.setId(id);  // Ensure we're updating the correct plan
        plan.setPlanManager(manager);  // associate plan manager
        Plan updatedPlan = planService.updatePlan(plan);
        return ResponseEntity.ok(updatedPlan);
    }

    // Delete plan (ADMIN or PLAN_MANAGER)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLAN_MANAGER')")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.ok().body("Plan deleted successfully");
    }

    // Get plan statistics (ADMIN or PLAN_MANAGER)
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','PLAN_MANAGER')")
    public ResponseEntity<?> getPlanStatistics() {
        try {
            Map<String, Object> stats = planService.getPlanStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch plan statistics: " + e.getMessage()));
        }
    }

    // Get plan performance metrics (ADMIN or PLAN_MANAGER)
    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('ADMIN','PLAN_MANAGER')")
    public ResponseEntity<?> getPlanPerformance() {
        try {
            List<Map<String, Object>> performance = planService.getPlanPerformance();
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch plan performance: " + e.getMessage()));
        }
    }
}
