import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;

def Message processData(Message message) {
    def properties = message.getProperties();
    def headers = message.getHeaders();
    def body = message.getBody(String);

    def fileName = properties.get("fileName") ?: "DATA_CLIENT_SAPERP.csv";
    def httpStatusCode = headers.get("CamelHttpResponseCode") ?: 200;

    def driveFileId = "N/A";
    def uploadStatus = "Succès";

    try {
        if (body && body.trim().startsWith("{")) {
            def jsonSlurper = new JsonSlurper();
            def jsonResult = jsonSlurper.parseText(body);
            if (jsonResult.id) {
                driveFileId = jsonResult.id;
            }
            if (jsonResult.name) {
                fileName = jsonResult.name;
            }
        }
    } catch (Exception e) {
        // Log or handle JSON parsing error
    }

    message.setProperty("driveFileId", driveFileId);
    message.setProperty("uploadStatus", uploadStatus);

    def emailBody = """Bonjour,

Le fichier ${fileName} a été correctement uploadé dans Google Drive via SAP CPI.

Statut : ${uploadStatus}
Nom du fichier : ${fileName}
ID Google Drive : ${driveFileId}

Cordialement,
Team CPI""";

    message.setBody(emailBody);
    message.setHeader("Subject", "Upload du fichier réussi");
    message.setHeader("Content-Type", "text/plain; charset=UTF-8");

    return message;
}
