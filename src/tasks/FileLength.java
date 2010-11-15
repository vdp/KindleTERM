import java.io.File;

/*
 * Created on 15/04/2005
 *
 */

/**
 * @author Karl
 * 
 */
public class FileLength {
    public static void main(String[] argv) {
        if (argv.length != 1) {
            System.err.println("Usage: filename");
            System.exit(1);
        }
        
        File file = new File(argv[0]);
        System.out.println(file.length());
    }
}
