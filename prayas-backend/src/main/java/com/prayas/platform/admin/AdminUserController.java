package com.prayas.platform.admin;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lead / Faculty In-charge only -- see AccessService.canAdmin. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("@access.canAdmin()")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public List<AdminUserView> list() {
        return service.list();
    }

    @GetMapping("/domains")
    public List<DomainView> domains() {
        return service.listDomains();
    }

    @PostMapping("/users")
    public AdminUserView create(@Valid @RequestBody AdminUserRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser AppUser actor) {
        service.delete(id, actor);
    }

    @PutMapping("/users/{id}")
    public AdminUserView update(@PathVariable Long id,
                                @Valid @RequestBody AdminUserRequest request,
                                @CurrentUser AppUser actor) {
        return service.update(id, request, actor);
    }
}
