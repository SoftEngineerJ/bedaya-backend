package backend.service;

import backend.entity.Contact;
import backend.model.ContactRequest;
import backend.model.BookingRequest;
import backend.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ContactRepository contactRepository;

    public void sendContactEmail(ContactRequest contactRequest) {
        // Save to database
        Contact contact = new Contact();
        contact.setType(Contact.ContactType.CONTACT);
        contact.setName(contactRequest.getName());
        contact.setEmail(contactRequest.getEmail());
        contact.setPhone(contactRequest.getPhone());
        contact.setCountry(contactRequest.getCountry());
        contact.setService(contactRequest.getService());
        contact.setMessage(contactRequest.getMessage());
        contact.setCreatedAt(LocalDateTime.now());
        contact.setStatus(Contact.ContactStatus.NEW);

        contactRepository.save(contact);

        // Send email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("mazroo.develop@gmail.com");
        message.setSubject("Neue Kontaktanfrage von: " + contactRequest.getName());
        message.setText(buildEmailText(contactRequest));

        mailSender.send(message);
    }

    public void sendBookingEmail(BookingRequest bookingRequest) {
        // Save to database (reuse Contact entity for now)
        Contact contact = new Contact();
        contact.setType(Contact.ContactType.BOOKING);
        contact.setName(bookingRequest.getFirstName() + " " + bookingRequest.getLastName());
        contact.setEmail(bookingRequest.getEmail());
        contact.setPhone(bookingRequest.getPhone());
        contact.setCountry(bookingRequest.getCountry());
        contact.setService(bookingRequest.getDesiredService());
        contact.setMessage(bookingRequest.getMessage() + "\n\nStudy Level: " + bookingRequest.getStudyLevel());
        contact.setCreatedAt(LocalDateTime.now());
        contact.setStatus(Contact.ContactStatus.NEW);

        contactRepository.save(contact);

        // Send confirmation email to customer
        SimpleMailMessage customerMessage = new SimpleMailMessage();
        customerMessage.setTo(bookingRequest.getEmail());
        customerMessage.setSubject("تأكيد استلام طلبك - بداية");
        customerMessage.setText(buildBookingConfirmationText(bookingRequest));
        mailSender.send(customerMessage);

        // Send notification email to admin
        SimpleMailMessage adminMessage = new SimpleMailMessage();
        adminMessage.setTo("mazroo.develop@gmail.com");
        adminMessage.setSubject("طلب جديد: " + bookingRequest.getDesiredService() + " - "
                + bookingRequest.getFirstName() + " " + bookingRequest.getLastName());
        adminMessage.setText(buildBookingAdminText(bookingRequest));
        mailSender.send(adminMessage);
    }

    private String buildBookingConfirmationText(BookingRequest bookingRequest) {
        return String.format(
                "شكراً لتواصلك مع بداية!\n\n" +
                        "تم استلام طلبك بنجاح:\n\n" +
                        "الاسم: %s %s\n" +
                        "البريد: %s\n" +
                        "الهاتف: %s\n" +
                        "الخدمة: %s\n" +
                        "المستوى: %s\n\n" +
                        "الخطوات التالية:\n" +
                        "- مراجعة الطلب خلال 2-4 ساعات\n" +
                        "- التواصل معك خلال 24 ساعة\n" +
                        "- تحديد موعد الاستشارة الأولى\n\n" +
                        "info@studentenhilfe.de",
                bookingRequest.getFirstName(),
                bookingRequest.getLastName(),
                bookingRequest.getEmail(),
                bookingRequest.getPhone(),
                bookingRequest.getDesiredService(),
                bookingRequest.getStudyLevel());
    }

    private String buildBookingAdminText(BookingRequest bookingRequest) {
        return String.format(
                "🚨 طلب جديد!\n\n" +
                        "معلومات العميل:\n" +
                        "الاسم: %s %s\n" +
                        "البريد: %s\n" +
                        "الهاتف: %s\n" +
                        "البلد: %s\n" +
                        "الخدمة: %s\n" +
                        "المستوى: %s\n\n" +
                        "الرسالة:\n%s\n\n" +
                        "إجراءات فورية:\n" +
                        "1. مراجعة الطلب فوراً\n" +
                        "2. التواصل مع العميل خلال 24 ساعة\n" +
                        "3. تحديد موعد استشارة\n" +
                        "4. إعداد عرض سعر مخصص",
                bookingRequest.getFirstName(),
                bookingRequest.getLastName(),
                bookingRequest.getEmail(),
                bookingRequest.getPhone(),
                bookingRequest.getCountry(),
                bookingRequest.getDesiredService(),
                bookingRequest.getStudyLevel(),
                bookingRequest.getMessage());
    }

    private String buildEmailText(ContactRequest contactRequest) {
        return String.format(
                "Neue Kontaktanfrage erhalten:\n\n" +
                        "Name: %s\n" +
                        "Email: %s\n" +
                        "Telefon: %s\n" +
                        "Land: %s\n" +
                        "Service: %s\n\n" +
                        "Nachricht:\n%s",
                contactRequest.getName(),
                contactRequest.getEmail(),
                contactRequest.getPhone(),
                contactRequest.getCountry(),
                contactRequest.getService(),
                contactRequest.getMessage());
    }
}