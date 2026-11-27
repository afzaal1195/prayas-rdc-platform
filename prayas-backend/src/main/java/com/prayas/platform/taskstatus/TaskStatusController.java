package com.prayas.platform.taskstatus;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.taskstatus.TaskStatusViews.TaskBoard;
import com.prayas.platform.user.AppUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Needs no special permission: the service only returns what the caller's own role allows. */
@RestController
@RequestMapping("/api/v1/task-status")
public class TaskStatusController {

    private final TaskStatusService service;

    public TaskStatusController(TaskStatusService service) {
        this.service = service;
    }

    @GetMapping
    public TaskBoard board(@CurrentUser AppUser viewer) {
        return service.board(viewer);
    }
}
