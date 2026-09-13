import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonOutput;

def Message processData(Message message) {
    def headers = message.getHeaders();
    def properties = message.getProperties();

    // Read fileName from Header or default to DATA_CLIENT_SAPERP.csv
    def fileName = headers.get("fileName") ?: headers.get("CamelFileName") ?: "DATA_CLIENT_SAPERP.csv";
    def mimeType = headers.get("Content-Type") ?: "text/csv";
    def folderId = headers.get("folderId") ?: ""; // Optional target folder ID

    def fileContent = message.getBody(byte[]);

    // Store metadata in properties
    message.setProperty("fileName", fileName);
    message.setProperty("mimeType", mimeType);
    message.setProperty("folderId", folderId);

    // Build multipart/related body for Google Drive API v3
    // Boundary string
    def boundary = "-------314159265358979323846";

    // Metadata JSON
    def metadataMap = [
        name: fileName,
        mimeType: mimeType
    ];
    if (folderId) {
        metadataMap.parents = [folderId];
    }

    def metadataJson = JsonOutput.toJson(metadataMap);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // Part 1: Metadata
    baos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
    baos.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes("UTF-8"));
    baos.write((metadataJson + "\r\n").getBytes("UTF-8"));

    // Part 2: File Content
    baos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
    baos.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes("UTF-8"));
    baos.write(fileContent);
    baos.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));

    message.setBody(baos.toByteArray());
    message.setHeader("Content-Type", "multipart/related; boundary=" + boundary);

    return message;
}
