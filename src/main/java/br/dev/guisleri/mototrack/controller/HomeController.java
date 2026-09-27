package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.HomeResponseDTO;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.HomeService;
import br.dev.guisleri.mototrack.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class HomeController {

    private final HomeService homeService;
    private final UserService userService;

    public HomeController(
            HomeService homeService,
            UserService userService
    ) {
        this.homeService = homeService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<HomeResponseDTO> getHome(
            Authentication authentication
    ) {
        User currentUser =
                userService.findUserByEmail(authentication.getName());

        HomeResponseDTO homeResponse =
                homeService.getHome(currentUser.getId());

        return ResponseEntity.ok(homeResponse);
    }
}