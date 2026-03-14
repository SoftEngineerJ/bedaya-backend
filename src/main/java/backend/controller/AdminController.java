package backend.controller;

import backend.entity.Contact;
import backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/contacts")
    public ResponseEntity<Page<Contact>> getAllContacts(Pageable pageable) {
        Page<Contact> contacts = adminService.getAllContacts(pageable);
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/contacts/{id}")
    public ResponseEntity<Contact> getContact(@PathVariable Long id) {
        Contact contact = adminService.getContactById(id);
        if (contact != null) {
            return ResponseEntity.ok(contact);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/contacts/status/{status}")
    public ResponseEntity<List<Contact>> getContactsByStatus(@PathVariable Contact.ContactStatus status) {
        List<Contact> contacts = adminService.getContactsByStatus(status);
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/contacts/type/{type}")
    public ResponseEntity<List<Contact>> getContactsByType(@PathVariable Contact.ContactType type) {
        List<Contact> contacts = adminService.getContactsByType(type);
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/contacts/type/{type}/status/{status}")
    public ResponseEntity<List<Contact>> getContactsByTypeAndStatus(
            @PathVariable Contact.ContactType type,
            @PathVariable Contact.ContactStatus status) {
        List<Contact> contacts = adminService.getContactsByTypeAndStatus(type, status);
        return ResponseEntity.ok(contacts);
    }

    @PutMapping("/contacts/{id}/status")
    public ResponseEntity<Contact> updateContactStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Contact.ContactStatus status = Contact.ContactStatus.valueOf(request.get("status"));
        Contact updatedContact = adminService.updateContactStatus(id, status);

        if (updatedContact != null) {
            return ResponseEntity.ok(updatedContact);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        adminService.deleteContact(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = Map.of(
                "totalContacts", adminService.getContactCount(),
                "newContacts", adminService.getContactCountByStatus(Contact.ContactStatus.NEW),
                "inProgressContacts", adminService.getContactCountByStatus(Contact.ContactStatus.IN_PROGRESS),
                "completedContacts", adminService.getContactCountByStatus(Contact.ContactStatus.COMPLETED),
                "totalContactRequests", adminService.getContactCountByType(Contact.ContactType.CONTACT),
                "totalBookingRequests", adminService.getContactCountByType(Contact.ContactType.BOOKING),
                "newContactRequests",
                adminService.getContactCountByTypeAndStatus(Contact.ContactType.CONTACT, Contact.ContactStatus.NEW),
                "newBookingRequests",
                adminService.getContactCountByTypeAndStatus(Contact.ContactType.BOOKING, Contact.ContactStatus.NEW));
        return ResponseEntity.ok(stats);
    }
}
