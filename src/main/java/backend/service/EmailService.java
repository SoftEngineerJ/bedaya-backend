package backend.service;

import backend.entity.Contact;
import backend.model.ContactRequest;
import backend.model.BookingRequest;
import backend.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

        @Autowired
        private ContactRepository contactRepository;

        private final String brevoApiKey = System.getenv("BREVO_API_KEY");
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

                // Send email via Brevo
                String subject = "Neue Kontaktanfrage von: " + contactRequest.getName();
                String text = buildEmailText(contactRequest);
                sendEmail(adminEmail, subject, text);

                // Send confirmation email to user
                sendEmail(
                                contactRequest.getEmail(),
                                "تم استلام رسالتك بنجاح - بداية",
                                buildContactConfirmationText(contactRequest));
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
                if (brevoApiKey == null || brevoApiKey.isBlank()) {
                        throw new IllegalStateException("BREVO_API_KEY is not set");
                }

                String payload = buildBrevoPayload(to, fromEmail, subject, text);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                                .header("accept", "application/json")
                                .header("content-type", "application/json")
                                .header("api-key", brevoApiKey)
                                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                                .build();

                try {
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        int statusCode = response.statusCode();
                        if (statusCode < 200 || statusCode >= 300) {
                                throw new IllegalStateException(
                                                "Brevo failed with status " + statusCode + ": " + response.body());
                        }
                } catch (Exception e) {
                        throw new IllegalStateException("Brevo request failed: " + e.getMessage(), e);
                }
        }

        private String buildBrevoPayload(String toEmail, String fromEmail, String subject, String text) {
                return "{" +
                                "\"sender\":{\"email\":\"" + jsonEscape(fromEmail) + "\"}," +
                                "\"to\":[{\"email\":\"" + jsonEscape(toEmail) + "\"}]," +
                                "\"subject\":\"" + jsonEscape(subject) + "\"," +
                                "\"textContent\":\"" + jsonEscape(text) + "\"" +
                                "}";
        }

        private String jsonEscape(String value) {
                if (value == null) {
                        return "";
                }
                return value
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\r", "\\r")
                                .replace("\n", "\\n")
                                .replace("\t", "\\t");
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

        private String buildContactConfirmationText(ContactRequest contactRequest) {
                return String.format(
                                "شكراً لتواصلك مع بداية!\n\n" +
                                                "لقد استلمنا رسالتك بنجاح وسنتواصل معك في أقرب وقت ممكن.\n\n" +
                                                "بياناتك:\n" +
                                                "الاسم: %s\n" +
                                                "البريد الإلكتروني: %s\n" +
                                                "%s" +
                                                "%s" +
                                                "الرسالة:\n%s\n\n" +
                                                "فريق بداية",
                                contactRequest.getName(),
                                contactRequest.getEmail(),
                                contactRequest.getPhone() == null || contactRequest.getPhone().isBlank()
                                                ? ""
                                                : "الهاتف: " + contactRequest.getPhone() + "\n",
                                contactRequest.getService() == null || contactRequest.getService().isBlank()
                                                ? ""
                                                : "الخدمة: " + contactRequest.getService() + "\n",
                                contactRequest.getMessage());
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