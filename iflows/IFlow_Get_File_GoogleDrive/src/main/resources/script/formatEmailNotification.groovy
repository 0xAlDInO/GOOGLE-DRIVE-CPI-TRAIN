import com.sap.gateway.ip.core.customdev.util.Message;

def Message processData(Message message) {
    def properties = message.getProperties();
    def fileName = properties.get("fileName") ?: "CV_SAPERP.PDF";
    def fileId = properties.get("fileId") ?: "N/A";

    // Save file payload attachment / body if needed
    // Prepare HTML / Plain text body for notification email
    def emailBody = """Bonjour,

Le fichier ${fileName} (ID: ${fileId}) a été récupéré avec succès depuis Google Drive par SAP CPI.

Cordialement,
Team CPI""";

    message.setBody(emailBody);
    message.setHeader("Subject", "Fichier récupéré depuis Google Drive");
    message.setHeader("Content-Type", "text/plain; charset=UTF-8");

    return message;
}
