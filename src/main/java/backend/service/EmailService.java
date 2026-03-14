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
        private final String publicLogoUrl = System.getenv().getOrDefault("MAIL_LOGO_URL", "");

        private static final String BRAND_DARK = "#0f172a";
        private static final String BRAND_TEXT = "#0f172a";
        private static final String BRAND_MUTED = "#64748b";
        private static final String BRAND_BG = "#f8fafc";
        private static final String BRAND_CARD = "#ffffff";

        private static final String LOGO_CID = "bedaya-logo";

        private static final String LOGO_BASE64_PNG = "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";

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
                String html = buildAdminContactHtml(contactRequest);
                sendEmail(adminEmail, subject, text, html);

                // Send confirmation email to user
                sendEmail(
                                contactRequest.getEmail(),
                                "تم استلام رسالتك بنجاح - بداية",
                                buildContactConfirmationText(contactRequest),
                                buildContactConfirmationHtml(contactRequest));
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
                                buildBookingConfirmationText(bookingRequest),
                                buildBookingConfirmationHtml(bookingRequest));

                // Send notification email to admin
                sendEmail(
                                adminEmail,
                                "طلب جديد: " + bookingRequest.getDesiredService() + " - "
                                                + bookingRequest.getFirstName() + " " + bookingRequest.getLastName(),
                                buildBookingAdminText(bookingRequest),
                                buildBookingAdminHtml(bookingRequest));
        }

        private void sendEmail(String to, String subject, String text, String html) {
                if (brevoApiKey == null || brevoApiKey.isBlank()) {
                        throw new IllegalStateException("BREVO_API_KEY is not set");
                }

                boolean includeInlineLogo = publicLogoUrl == null || publicLogoUrl.isBlank();
                String payload = buildBrevoPayload(to, fromEmail, subject, text, html, includeInlineLogo);

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

        private String buildBrevoPayload(String toEmail, String fromEmail, String subject, String text, String html,
                        boolean includeInlineLogo) {
                String safeHtml = html == null ? "" : html;
                String attachments = "";
                if (includeInlineLogo) {
                        attachments = ",\"attachment\":[{" +
                                        "\"content\":\"" + LOGO_BASE64_PNG + "\"," +
                                        "\"name\":\"bedaya-logo.png\"," +
                                        "\"contentType\":\"image/png\"," +
                                        "\"contentId\":\"" + LOGO_CID + "\"" +
                                        "}]";
                }

                return "{" +
                                "\"sender\":{\"email\":\"" + jsonEscape(fromEmail) + "\"}," +
                                "\"to\":[{\"email\":\"" + jsonEscape(toEmail) + "\"}]," +
                                "\"subject\":\"" + jsonEscape(subject) + "\"," +
                                "\"textContent\":\"" + jsonEscape(text) + "\"," +
                                "\"htmlContent\":\"" + jsonEscape(safeHtml) + "\"" +
                                attachments +
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

        private String buildBookingConfirmationHtml(BookingRequest bookingRequest) {
                String title = "تم استلام طلبك بنجاح";
                String body = "<p style=\"margin:0 0 12px 0;\">شكراً لتواصلك مع <b>بداية</b>.</p>" +
                                "<p style=\"margin:0 0 16px 0;\">تم استلام طلبك بنجاح، وسنقوم بمراجعته والتواصل معك خلال 24 ساعة.</p>"
                                +
                                detailsTable(new String[][] {
                                                { "الاسم", safe(bookingRequest.getFirstName() + " "
                                                                + bookingRequest.getLastName()) },
                                                { "البريد الإلكتروني", safe(bookingRequest.getEmail()) },
                                                { "الهاتف", safe(bookingRequest.getPhone()) },
                                                { "الخدمة", safe(bookingRequest.getDesiredService()) },
                                                { "المستوى", safe(bookingRequest.getStudyLevel()) }
                                }) +
                                (bookingRequest.getMessage() == null || bookingRequest.getMessage().isBlank()
                                                ? ""
                                                : section("رسالتك", "<p style=\"margin:0; white-space:pre-wrap;\">"
                                                                + safe(bookingRequest.getMessage()) + "</p>"));
                return wrapEmailHtml(title, body);
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

        private String buildContactConfirmationHtml(ContactRequest contactRequest) {
                String title = "تم استلام رسالتك بنجاح";
                String body = "<p style=\"margin:0 0 12px 0;\">شكراً لتواصلك مع <b>بداية</b>.</p>" +
                                "<p style=\"margin:0 0 16px 0;\">لقد استلمنا رسالتك وسنتواصل معك في أقرب وقت ممكن.</p>"
                                +
                                detailsTable(new String[][] {
                                                { "الاسم", safe(contactRequest.getName()) },
                                                { "البريد الإلكتروني", safe(contactRequest.getEmail()) },
                                                { "الهاتف", safeNullable(contactRequest.getPhone()) },
                                                { "الخدمة", safeNullable(contactRequest.getService()) }
                                }) +
                                section("رسالتك", "<p style=\"margin:0; white-space:pre-wrap;\">"
                                                + safe(contactRequest.getMessage()) + "</p>");
                return wrapEmailHtml(title, body);
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

        private String buildBookingAdminHtml(BookingRequest bookingRequest) {
                String title = "طلب جديد";
                String body = "<p style=\"margin:0 0 12px 0;\">تم استلام <b>طلب جديد</b>.</p>" +
                                detailsTable(new String[][] {
                                                { "الاسم", safe(bookingRequest.getFirstName() + " "
                                                                + bookingRequest.getLastName()) },
                                                { "البريد الإلكتروني", safe(bookingRequest.getEmail()) },
                                                { "الهاتف", safe(bookingRequest.getPhone()) },
                                                { "البلد", safe(bookingRequest.getCountry()) },
                                                { "الخدمة", safe(bookingRequest.getDesiredService()) },
                                                { "المستوى", safe(bookingRequest.getStudyLevel()) }
                                }) +
                                section("الرسالة", "<p style=\"margin:0; white-space:pre-wrap;\">"
                                                + safe(bookingRequest.getMessage()) + "</p>");
                return wrapEmailHtml(title, body);
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

        private String buildAdminContactHtml(ContactRequest contactRequest) {
                String title = "Kontaktanfrage";
                String body = "<p style=\"margin:0 0 12px 0;\">Neue Kontaktanfrage erhalten.</p>" +
                                detailsTable(new String[][] {
                                                { "Name", safe(contactRequest.getName()) },
                                                { "Email", safe(contactRequest.getEmail()) },
                                                { "Telefon", safeNullable(contactRequest.getPhone()) },
                                                { "Land", safeNullable(contactRequest.getCountry()) },
                                                { "Service", safeNullable(contactRequest.getService()) }
                                }) +
                                section("Nachricht",
                                                "<p style=\"margin:0; white-space:pre-wrap;\">"
                                                                + safe(contactRequest.getMessage())
                                                                + "</p>");
                return wrapEmailHtml(title, body);
        }

        private String detailsTable(String[][] rows) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                                "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:separate; border-spacing:0; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;\">");
                for (int i = 0; i < rows.length; i++) {
                        String label = rows[i][0];
                        String value = rows[i][1];
                        if (value == null || value.isBlank()) {
                                continue;
                        }
                        sb.append("<tr>");
                        sb.append(
                                        "<td style=\"padding:10px 12px; background:#f8fafc; color:" + BRAND_MUTED
                                                        + "; font-size:12px; width:34%; border-bottom:1px solid #e2e8f0;\">")
                                        .append(safe(label))
                                        .append("</td>");
                        sb.append(
                                        "<td style=\"padding:10px 12px; background:#ffffff; color:" + BRAND_TEXT
                                                        + "; font-size:13px; border-bottom:1px solid #e2e8f0;\">")
                                        .append(safe(value))
                                        .append("</td>");
                        sb.append("</tr>");
                }
                sb.append("</table>");
                return sb.toString();
        }

        private String wrapEmailHtml(String title, String innerHtml) {
                String logoSrc = "cid:" + LOGO_CID;
                if (publicLogoUrl != null && !publicLogoUrl.isBlank()) {
                        logoSrc = safe(publicLogoUrl);
                }
                return "<!doctype html>" +
                                "<html lang=\"ar\" dir=\"rtl\">" +
                                "<head>" +
                                "<meta charset=\"utf-8\">" +
                                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                                "</head>" +
                                "<body style=\"margin:0; padding:0; background:" + BRAND_BG + "; color:" + BRAND_TEXT
                                + "; font-family:Tahoma, Arial, sans-serif;\">" +
                                "<div style=\"max-width:640px; margin:0 auto; padding:24px;\">" +
                                "<div style=\"background:" + BRAND_CARD
                                + "; border:1px solid #e2e8f0; border-radius:14px; overflow:hidden;\">" +
                                "<div style=\"padding:20px 24px; background:linear-gradient(135deg, " + BRAND_DARK
                                + " 0%, #1e3a8a 100%);\">" +
                                "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
                                +
                                "<tr>" +
                                "<td style=\"vertical-align:middle;\">" +
                                "<img src=\"" + logoSrc
                                + "\" alt=\"Bedaya\" width=\"56\" height=\"56\" style=\"display:block; width:56px; height:56px;\">"
                                +
                                "</td>" +
                                "<td style=\"vertical-align:middle; text-align:right;\">" +
                                "<div style=\"color:#ffffff; font-size:18px; font-weight:700;\">بداية</div>" +
                                "<div style=\"color:rgba(255,255,255,0.85); font-size:12px; margin-top:2px;\">"
                                + safe(title) + "</div>" +
                                "</td>" +
                                "</tr>" +
                                "</table>" +
                                "</div>" +
                                "<div style=\"padding:22px 24px;\">" +
                                "<div style=\"font-size:20px; font-weight:700; margin:0 0 12px 0; color:" + BRAND_DARK
                                + ";\">" + safe(title) + "</div>" +
                                innerHtml +
                                "</div>" +
                                "<div style=\"padding:16px 24px; border-top:1px solid #e2e8f0; background:#f1f5f9;\">"
                                +
                                "<div style=\"color:" + BRAND_MUTED + "; font-size:12px; line-height:1.6;\">" +
                                "فريق بداية — شريكك في التعليم<br>" +
                                "<span style=\"direction:ltr; unicode-bidi:bidi-override;\">info@studentenhilfe.de</span>"
                                +
                                "</div>" +
                                "</div>" +
                                "</div>" +
                                "</div>" +
                                "</body>" +
                                "</html>";
        }

        private String section(String title, String contentHtml) {
                return "<div style=\"margin-top:16px;\">" +
                                "<div style=\"font-size:14px; font-weight:700; color:" + BRAND_DARK
                                + "; margin:0 0 8px 0;\">" + safe(title) + "</div>" +
                                "<div style=\"border:1px solid #e2e8f0; border-radius:12px; padding:12px; background:#ffffff;\">"
                                +
                                contentHtml +
                                "</div>" +
                                "</div>";
        }

        private String safeNullable(String v) {
                return v == null ? "" : v;
        }

        private String safe(String v) {
                if (v == null) {
                        return "";
                }
                return v
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }
}