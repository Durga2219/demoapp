package com.ashu.ride_sharing.security.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ashu.ride_sharing.models.User;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.verification.base-url}")
    private String verificationBaseURL;

    @Value("${app.email.from}")
    private String fromEmail;

    private static String appName = "Ride Share";

    public void sendVerificationEmail(User user, String token) {
        try {
            // SimpleMailMessage message = getSimpleMailMessage(user, token);
            // mailSender.send(message);
            // log.info("Verification email sent successfully to {}", user.getEmail());
            sendProfessionalHtmlEmail(user, token);
        log.info("Verification email sent successfully to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to {} : {}", user.getEmail(), e.getMessage());
        }
    }

    // private SimpleMailMessage getSimpleMailMessage(User user, String token) {
    //     SimpleMailMessage message = new SimpleMailMessage();
    //     message.setFrom(fromEmail);
    //     message.setTo(user.getEmail());
    //     message.setSubject("Verify your Email Address");

    //     String verificationUrl = verificationBaseURL + "?token=" + token;
    //     message.setText("Dear " + user.getUsername()
    //             + token +
    //             "\n\nPlease click the link below to verify your email address:\n"
    //             + verificationUrl + "\n\nThis link will expire in 24 hours. \n\n Thank you,\nEcom App");

    //     return message;
    // }

    private void sendProfessionalHtmlEmail(User user, String token) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        String verificationUrl = verificationBaseURL + "?token=" + token;
        String content = buildProfessionalEmailTemplate(user.getFirstName(), verificationUrl);

        helper.setFrom(new InternetAddress(fromEmail, appName));
        helper.setTo(user.getEmail());
        helper.setSubject("Verify Your Email Address - " + appName);
        helper.setText(content, true);
        
        mailSender.send(mimeMessage);
    }

    private String buildProfessionalEmailTemplate(String username, String verificationUrl) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Email Verification</title>");
        html.append("<style>");
        html.append("@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');");
        html.append("body { font-family: 'Inter', 'Segoe UI', system-ui, sans-serif; line-height: 1.6; color: #374151; margin: 0; padding: 0; background-color: #f9fafb; }");
        html.append(".email-wrapper { max-width: 600px; margin: 0 auto; background: #ffffff; }");
        html.append(".header { background: linear-gradient(135deg, #e47e35ff 0%, #7dac2cff 100%); padding: 40px 30px; text-align: center; color: white; }");
        html.append(".logo { font-size: 28px; font-weight: 600; margin-bottom: 10px; }");
        html.append(".tagline { font-weight: 300; opacity: 0.9; font-size: 16px; }");
        html.append(".content { padding: 40px; }");
        html.append(".greeting { font-size: 20px; color: #111827; margin-bottom: 10px; font-weight: 500; }");
        html.append(".message { color: #6b7280; margin-bottom: 25px; font-size: 15px; }");
        html.append(".button-container { text-align: center; margin: 35px 0; }");
        html.append(".verify-button { display: inline-block; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 500; box-shadow: 0 4px 6px rgba(16, 185, 129, 0.2); transition: all 0.3s ease; }");
        html.append(".verify-button:hover { transform: translateY(-1px); box-shadow: 0 6px 12px rgba(16, 185, 129, 0.3); }");
        html.append(".link-container { background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 6px; padding: 15px; margin: 25px 0; }");
        html.append(".link-text { word-break: break-all; font-size: 14px; color: #495057; font-family: 'Courier New', monospace; }");
        html.append(".security-note { background: #fffbeb; border: 1px solid #fef3c7; border-radius: 6px; padding: 16px; margin: 25px 0; font-size: 14px; }");
        html.append(".security-icon { color: #d97706; font-weight: 600; }");
        html.append(".footer { background: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef; }");
        html.append(".support-info { color: #6b7280; font-size: 14px; margin-bottom: 15px; }");
        html.append(".copyright { font-size: 12px; color: #9ca3af; margin-top: 20px; }");
        html.append(".divider { height: 1px; background: #e5e7eb; margin: 25px 0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='email-wrapper'>");
        html.append("<div class='header'>");
        html.append("<div class='logo'>").append(appName).append("</div>");
        html.append("<div class='tagline'>Secure Ride Sharing</div>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        html.append("<div class='greeting'>Hello ").append(username).append(",</div>");
        
        html.append("<div class='message'>");
        html.append("<p>Welcome to ").append(appName).append("! To complete your account registration and start using our ride-sharing services, please verify your email address.</p>");
        html.append("<p>Verifying your email helps us ensure the security of your account and enables important notifications about your rides.</p>");
        html.append("</div>");
        
        html.append("<div class='button-container'>");
        html.append("<a href='").append(verificationUrl).append("' class='verify-button'>Verify Email Address</a>");
        html.append("</div>");
        
        html.append("<div class='security-note'>");
        html.append("<span class='security-icon'>⚠️</span> For your security, this verification link will expire in <strong>24 hours</strong>.");
        html.append("</div>");
        
        html.append("<div class='message'>");
        html.append("<p><strong>Trouble with the button?</strong> Copy and paste the URL below into your web browser:</p>");
        html.append("</div>");
        
        html.append("<div class='link-container'>");
        html.append("<code class='link-text'>").append(verificationUrl).append("</code>");
        html.append("</div>");
        
        html.append("<div class='divider'></div>");
        
        html.append("<div class='message'>");
        html.append("<p><strong>Didn't create an account?</strong> If you didn't request this email, please ignore it. Your email address may have been entered by mistake.</p>");
        html.append("</div>");
        
        html.append("</div>");
        
        html.append("<div class='footer'>");
        html.append("<div class='support-info'>");
        html.append("Need help? Contact our support team at <a href='mailto:support@example.com' style='color: #10b981;'>support@example.com</a>");
        html.append("<br>We're here to assist you Monday through Friday, 9:00 AM to 6:00 PM");
        html.append("</div>");
        html.append("<div class='copyright'>");
        html.append("© 2024 ").append(appName).append(". All rights reserved.<br>");
        html.append("This email was sent to you as part of your account registration process.");
        html.append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}
