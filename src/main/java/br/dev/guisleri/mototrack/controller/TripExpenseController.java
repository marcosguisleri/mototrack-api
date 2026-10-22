package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.TripExpenseRequestDTO;
import br.dev.guisleri.mototrack.dto.TripExpenseResponseDTO;
import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.TripExpenseService;
import br.dev.guisleri.mototrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trips/{tripId}/expenses")
public class TripExpenseController {

    private final TripExpenseService tripExpenseService;
    private final UserService userService;

    public TripExpenseController(
            TripExpenseService tripExpenseService,
            UserService userService
    ) {
        this.tripExpenseService = tripExpenseService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<TripExpenseResponseDTO> registerExpense(
            @PathVariable("tripId") long tripId,
            @Valid @RequestBody TripExpenseRequestDTO requestDTO,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        TripExpense expense = tripExpenseService.registerExpenseForOwner(
                tripId,
                currentUser.getId(),
                requestDTO.category(),
                requestDTO.amount(),
                requestDTO.description()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TripExpenseResponseDTO.from(expense));
    }

    @GetMapping
    public ResponseEntity<List<TripExpenseResponseDTO>> findExpenses(
            @PathVariable("tripId") long tripId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        List<TripExpenseResponseDTO> expenses = tripExpenseService
                .findExpensesByTripIdForOwner(tripId, currentUser.getId())
                .stream()
                .map(TripExpenseResponseDTO::from)
                .toList();

        return ResponseEntity.ok(expenses);
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<TripExpenseResponseDTO> updateExpense(
            @PathVariable("tripId") long tripId,
            @PathVariable("expenseId") long expenseId,
            @Valid @RequestBody TripExpenseRequestDTO requestDTO,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        TripExpense expense = tripExpenseService.updateExpenseForOwner(
                tripId,
                expenseId,
                currentUser.getId(),
                requestDTO.category(),
                requestDTO.amount(),
                requestDTO.description()
        );

        return ResponseEntity.ok(TripExpenseResponseDTO.from(expense));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable("tripId") long tripId,
            @PathVariable("expenseId") long expenseId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        tripExpenseService.deleteExpenseForOwner(
                tripId,
                expenseId,
                currentUser.getId()
        );

        return ResponseEntity.noContent().build();
    }

}
