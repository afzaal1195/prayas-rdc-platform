package com.prayas.platform.assignment;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.user.AppUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The signed-in person's own shifts. No special permission is needed: it only
 * ever returns the caller's own assignments, whoever the caller is.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MyAssignmentsController {

    private final AssignmentService service;

    public MyAssignmentsController(AssignmentService service) {
        this.service = service;
    }

    @GetMapping("/assignments")
    public List<MyAssignmentView> mine(@CurrentUser AppUser user) {
        return service.myAssignments(user.getId());
    }
}
