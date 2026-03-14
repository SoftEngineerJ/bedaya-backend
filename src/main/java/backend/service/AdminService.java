package backend.service;

import backend.entity.Contact;
import backend.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private ContactRepository contactRepository;

    public Page<Contact> getAllContacts(Pageable pageable) {
        return contactRepository.findAll(pageable);
    }

    public Contact getContactById(Long id) {
        return contactRepository.findById(id).orElse(null);
    }

    public List<Contact> getContactsByStatus(Contact.ContactStatus status) {
        return contactRepository.findByStatus(status);
    }

    public List<Contact> getContactsByType(Contact.ContactType type) {
        return contactRepository.findByType(type);
    }

    public List<Contact> getContactsByTypeAndStatus(Contact.ContactType type, Contact.ContactStatus status) {
        return contactRepository.findByTypeAndStatus(type, status);
    }

    public Contact updateContactStatus(Long id, Contact.ContactStatus status) {
        Contact contact = contactRepository.findById(id).orElse(null);
        if (contact != null) {
            contact.setStatus(status);
            return contactRepository.save(contact);
        }
        return null;
    }

    public void deleteContact(Long id) {
        contactRepository.deleteById(id);
    }

    public long getContactCount() {
        return contactRepository.count();
    }

    public long getContactCountByStatus(Contact.ContactStatus status) {
        return contactRepository.countByStatus(status);
    }

    public long getContactCountByType(Contact.ContactType type) {
        return contactRepository.countByType(type);
    }

    public long getContactCountByTypeAndStatus(Contact.ContactType type, Contact.ContactStatus status) {
        return contactRepository.countByTypeAndStatus(type, status);
    }
}
