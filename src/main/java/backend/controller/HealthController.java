package backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Backend is running and connected to PostgreSQL!");
    }
    
    @GetMapping("/status")
    public ResponseEntity<Object> status() {
        return ResponseEntity.ok(new StatusResponse("OK", "Backend is healthy"));
    }
    
    public static class StatusResponse {
        private String status;
        private String message;
        
        public StatusResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }
}
