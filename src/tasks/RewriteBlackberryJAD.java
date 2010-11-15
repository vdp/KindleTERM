import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
 * Created on 15/04/2005
 *
 */

/**
 * @author Karl
 *
 */
public class RewriteBlackberryJAD {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        File jadIn = new File(args[0]);
        File jadOut = new File(args[1]);
        String sha1 = sha1(new File(args[2]));
        String sha2 = sha1(new File(args[3]));
        String cod1 = args[4];
        String cod2 = args[5];
        long length1 = Long.parseLong(args[6]);
        long length2 = Long.parseLong(args[7]);
        
        BufferedReader in = new BufferedReader(new FileReader(jadIn));
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(jadOut)));
        
        String line = in.readLine();
        while (line != null) {
            if (line.startsWith("RIM-COD-URL") || line.startsWith("RIM-COD-SHA1") || line.startsWith("RIM-COD-Size")) {
                /* Ignore these lines */
            }
            else {
                out.println(line);
            }
            line = in.readLine();
        }
        
        out.println("RIM-COD-URL-1: " + cod1);
        out.println("RIM-COD-URL-2: " + cod2);
        out.println("RIM-COD-Size-1: " + length1);
        out.println("RIM-COD-Size-2: " + length2);
        out.println("RIM-COD-SHA1-1: " + sha1);
        out.println("RIM-COD-SHA1-2: " + sha2);
        
        in.close();
        out.close();
    }
 
    private static String sha1(File file) throws IOException, NoSuchAlgorithmException {
    	byte[] data = new byte[(int) file.length()];
    	DataInputStream in = new DataInputStream(new FileInputStream(file));
    	in.readFully(data);
    	
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(data);

		StringBuffer hashString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString((int) (hash[i] & 0xff));
			if (hex.length() < 2) {
				hashString.append('0');
			}
			hashString.append(hex);
			hashString.append(" ");
		}

		return hashString.toString().trim();
    }
}
