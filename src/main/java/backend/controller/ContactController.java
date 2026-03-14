package backend.controller;

import backend.model.ContactRequest;
import backend.model.BookingRequest;
import backend.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ContactController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/contact")
    public ResponseEntity<String> submitContact(@Valid @RequestBody ContactRequest contactRequest) {
        try {
            emailService.sendContactEmail(contactRequest);
            return ResponseEntity.ok("Contact form submitted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error submitting contact form: " + e.getMessage());
        }
    }

    @PostMapping("/booking")
    public ResponseEntity<String> submitBooking(@Valid @RequestBody BookingRequest bookingRequest) {
        try {
            emailService.sendBookingEmail(bookingRequest);
            return ResponseEntity.ok("Booking submitted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error submitting booking: " + e.getMessage());
        }
    }
}
