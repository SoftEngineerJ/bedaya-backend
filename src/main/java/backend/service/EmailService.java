package backend.service;

import backend.entity.Contact;
import backend.model.ContactRequest;
import backend.model.BookingRequest;
import backend.repository.ContactRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.io.IOException;

@Service
public class EmailService {

        @Autowired
        private ContactRepository contactRepository;

        private final String sendGridApiKey = System.getenv("SENDGRID_API_KEY");
        private final String fromEmail = System.getenv().getOrDefault("MAIL_FROM", "no-reply@bedaya.local");
        private final String adminEmail = System.getenv().getOrDefault("MAIL_ADMIN_TO", "mazroo.develop@gmail.com");

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

                // Send email via SendGrid
                String subject = "Neue Kontaktanfrage von: " + contactRequest.getName();
                String text = buildEmailText(contactRequest);
                sendEmail(adminEmail, subject, text);
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
                sendEmail(
                                bookingRequest.getEmail(),
                                "تأكيد استلام طلبك - بداية",
                                buildBookingConfirmationText(bookingRequest));

                // Send notification email to admin
                sendEmail(
                                adminEmail,
                                "طلب جديد: " + bookingRequest.getDesiredService() + " - "
                                                + bookingRequest.getFirstName() + " " + bookingRequest.getLastName(),
                                buildBookingAdminText(bookingRequest));
        }

        private void sendEmail(String to, String subject, String text) {
                if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
                        throw new IllegalStateException("SENDGRID_API_KEY is not set");
                }

                Email from = new Email(fromEmail);
                Email recipient = new Email(to);
                Content content = new Content("text/plain", text);
                Mail mail = new Mail(from, subject, recipient, content);

                Personalization personalization = new Personalization();
                personalization.addTo(recipient);
                mail.addPersonalization(personalization);

                SendGrid sg = new SendGrid(sendGridApiKey);
                Request request = new Request();
                try {
                        request.setMethod(Method.POST);
                        request.setEndpoint("mail/send");
                        request.setBody(mail.build());
                        Response response = sg.api(request);
                        int statusCode = response.getStatusCode();
                        if (statusCode < 200 || statusCode >= 300) {
                                throw new IllegalStateException("SendGrid failed with status " + statusCode + ": "
                                                + response.getBody());
                        }
                } catch (IOException e) {
                        throw new IllegalStateException("SendGrid request failed: " + e.getMessage(), e);
                }
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