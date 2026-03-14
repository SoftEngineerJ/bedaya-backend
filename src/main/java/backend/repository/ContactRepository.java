package backend.repository;

import backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByStatus(Contact.ContactStatus status);

    List<Contact> findByType(Contact.ContactType type);

    List<Contact> findByTypeAndStatus(Contact.ContactType type, Contact.ContactStatus status);

    long countByStatus(Contact.ContactStatus status);

    long countByType(Contact.ContactType type);

    long countByTypeAndStatus(Contact.ContactType type, Contact.ContactStatus status);
}
