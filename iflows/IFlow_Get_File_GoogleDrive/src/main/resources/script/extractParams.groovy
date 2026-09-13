import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

def Message processData(Message message) {
    // Read query parameter fileId or header fileId from incoming HTTP request
    def headers = message.getHeaders();
    def properties = message.getProperties();

    // Extract fileId from header or parameter
    def fileId = headers.get("fileId") ?: headers.get("CamelHttpQuery")?.split('&')?.find { it.startsWith("fileId=") }?.split('=')?.getAt(1);
    def fileName = headers.get("fileName") ?: "CV_SAPERP.PDF";

    if (!fileId) {
        throw new IllegalArgumentException("Le paramètre 'fileId' est obligatoire dans la requête HTTP.");
    }

    // Store in Exchange Properties for subsequent steps
    message.setProperty("fileId", fileId);
    message.setProperty("fileName", fileName);

    return message;
}
