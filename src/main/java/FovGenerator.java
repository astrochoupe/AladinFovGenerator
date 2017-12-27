import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generate *.vot files for Aladin software (aladin.u-strasbg.fr).
 *
 * *.vot files describe the field of view (fov) of a camera behind a telescope.
 * You can load these files in Aladin to show the fov on the sky map.
 *
 * The purpose of this class is to simplify the creation of *.vot files when
 * you have multiple cameras and telescopes (cartesian product).
 *
 * The error management is minimalistic.
 */
public class FovGenerator {

    public static void main(String[] args) throws IOException {
        String template = readFileInClasspath("footprint.xml", StandardCharsets.UTF_8);

        try(Reader isCameras = new InputStreamReader(FovGenerator.class.getClassLoader().getResourceAsStream("cameras.csv"), StandardCharsets.UTF_8);
            Reader isOptics = new InputStreamReader(FovGenerator.class.getClassLoader().getResourceAsStream("optics.csv"), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> cameras = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(isCameras);

            for (CSVRecord camera : cameras) {
                String cameraName = camera.get(0);
                String sizePhot = camera.get(1);
                String nbPhotX = camera.get(2);
                String nbPhotY = camera.get(3);

                float photositeSize = Float.valueOf(sizePhot);
                int nbPhotositesWidth = Integer.valueOf(nbPhotX);
                int nbPhotositesHeight = Integer.valueOf(nbPhotY);

                Iterable<CSVRecord> optics = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(isOptics);

                for (CSVRecord optic : optics) {
                    String opticName = optic.get(0);
                    String correctorName = optic.get(1);
                    String focaleLength = optic.get(2);

                    int focale = Integer.valueOf(focaleLength);

                    int fieldWidthArcsec = Math.round(photositeSize * nbPhotositesWidth * 206 / focale);
                    int fieldHeightArcsec = Math.round(photositeSize * nbPhotositesHeight * 206 / focale);

                    double halfFieldWidthArcsec = Math.rint(fieldWidthArcsec / 2);
                    double halfFieldHeightArcsec = Math.rint(fieldHeightArcsec / 2);

                    String outputFilename = composeFilename(cameraName, opticName, correctorName);
                    String extension = ".vot";

                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("{ID}", outputFilename);
                    replacements.put("{TelescopeName}", opticName);
                    replacements.put("{InstrumentName}", cameraName);
                    replacements.put("{HalfFieldWidthArcsec}", String.valueOf(halfFieldWidthArcsec));
                    replacements.put("{HalfFieldHeightArcsec}", String.valueOf(halfFieldHeightArcsec));

                    String content = replace(template, replacements);
                    writeVotFile(outputFilename + extension, content);

                    System.out.println("Ecriture du fichier '" + outputFilename + extension + "'");

                    break;
                }
                break;
            }
        }
    }

    /**
     * Give the filename from the camera, optic and corrector name.
     *
     * @param cameraName Camera name
     * @param opticName Optic name
     * @param correctorName Corrector/reductor name
     * @return Filename of the vot file
     */
    private static String composeFilename(String cameraName, String opticName, String correctorName) {
        cameraName = cleanStringForFilename(cameraName);
        opticName = cleanStringForFilename(opticName);
        correctorName = cleanStringForFilename(correctorName);

        String filename = cameraName + "-" + opticName;
        if(!correctorName.isEmpty()) {
            filename += "-" + correctorName;
        }
        return filename;
    }

    /**
     * Replace some problematic characters in a filename (slash, antislash, space...).
     *
     * @param s String to clean
     * @return A cleaned string
     */
    private static String cleanStringForFilename(String s) {
        return s.replace("/","").replace("\\","").replace(" ","-");
    }

    /**
     * Give the content of a file in the filesystem.
     *
     * @param path Path to a file in the filesystem.
     * @param encoding Charset encoding.
     * @return The content of the file.
     * @throws IOException
     */
    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    /**
     * Give the content of a file in the classpath.
     *
     * @param path Path to a file in the classpath.
     * @param encoding Charset encoding.
     * @return The content of the file.
     * @throws IOException
     */
    private static String readFileInClasspath(String path, Charset encoding) throws IOException {
        URL templateUrl = FovGenerator.class.getClassLoader().getResource(path);
        String templatePath = templateUrl.getPath();
        String templatePathCleaned = templatePath.substring(1);
        return readFile(templatePathCleaned, encoding);
    }

    /**
     * Replace some substrings in a string.
     * Replace only the first occurence.
     *
     * @param inputString The string where we will search
     * @param replacements A map where the key is the old value and the value the new value
     * @return A new String with the old values replaced by the new ones.
     */
    private static String replace(String inputString, Map<String, String> replacements) {
        String outputString = inputString;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            outputString = outputString.replace(entry.getKey(), entry.getValue());
        }
        return outputString;
    }

    /**
     * Write a vot file in the execution directory.
     *
     * @param filename Output filename.
     * @param content Content of the file.
     * @throws FileNotFoundException
     */
    private static void writeVotFile(String filename, String content) throws FileNotFoundException {
        // TODO write in UTF8
        try( PrintWriter out = new PrintWriter(filename) ) {
            out.print(content);
        }
    }
}
