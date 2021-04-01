import java.util.zip.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import java.lang.Integer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//refer for help: https://www.roseindia.net/java/beginners/ChecksumByteArray.shtml
/**
 * Class for error-checking data packages allowing 
 * for verification of package integrity.
 */
public class Check  {
    private byte[] buffer;
    private long value;
    private ByteArrayInputStream bais;
    private CheckedInputStream cis;
    DatagramPacket packet;
    /**
     * Creates an object used to check integrity of a 
     * DatagramPacket's data integrity.
     * Extracks data information from a packet and 
     * creates a checksum for that packet's data.
     * @param p Packet to be checked
     */
    public Check (DatagramPacket p){
        packet = p;
        buffer = packet.getData();
        bais = new ByteArrayInputStream(buffer);
        cis = new CheckedInputStream(bais, new Adler32());
    }

    /**
     * Creates a checksum object using a buffer.
     * @param b Input buffer.
     */
    public Check (byte[] b){
        buffer = b;
        bais = new ByteArrayInputStream(buffer);
        cis = new CheckedInputStream(bais, new Adler32());
    }
    /**
     * Creates an object allowing for a Checksum number to be 
     * generated using contained data.
     * @param s String data to be checked
     */
    public Check (String s){ 
        buffer = s.getBytes();
        bais = new ByteArrayInputStream(buffer);
        cis = new CheckedInputStream(bais, new Adler32());

    }



    /**
     * Calculates a checksum value on calling Check object.
     * @return A long value of this object's Checksum.
     */
    public long Checksum(){
        byte[] readBuffer = new byte[buffer.length];
        try{
            while (cis.read(readBuffer) >= 0){
                value = cis.getChecksum().getValue();
            }
        }
        catch(Exception e){
            System.out.println("Exception caught: "+ e);
        }
        return value;
    }

    /**
     * Sees if calling Checksum is equal to passed Checksum
     * based on their Checksum value.
     * Use the String class' equals method to check
     * value pulled from a DatagramPacket's message.
     * @param c a Check object to be compared.
     * @return True if equal, false if not.
     */
    public boolean equals(Check c){
        boolean result;
        if(this.value == c.getValue()){
            result = true;
        }
        else{
            result = false;
        }
        return result;
    }

    public long getValue(){
        return value;
    }

    /**
     * For comparing one Checksum against another Checksum.
     * 
     * @param s
     * @return
     */
    public boolean equals(String s1, String s2){

    }

    /**
     * Main method to test that passed objects/parameters 
     * yeild correct answers where applicable.
     * Tested with DatagramPackets and Strings.
     * @param args
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException{
        if(args.length == 3){
            System.out.println("Checking on DatagramPackets\nSyntax: <host> <data1> <data2>\n"+
            "<checksum1> <checksum2>\n");
            InetAddress address = InetAddress.getByName(args[0]);
            DatagramPacket packet1 = new DatagramPacket(args[1].getBytes(), args[1].getBytes().length, address, 4446);
            DatagramPacket packet2 = new DatagramPacket(args[2].getBytes(), args[2].getBytes().length, address, 4447);
            
            Check check1 = new Check(packet1);
            Check check2 = new Check(packet2);
            long checksum1 = check1.Checksum();
            long checksum2 = check2.Checksum();
            System.out.println("Checksum1: "+checksum1+"\nChecksum2: "+checksum2+"\n");
            if(checksum1 == checksum2){
                System.out.println("It's a match!");
            }
            else{
                System.out.println("No luck here, buddy.");
            }
        }
        else if (args.length == 2){
            System.out.println("Checking on String data\nSyntax: <string data1> <string data2>\n"+
            "<checksum1> <checksum2>\n");
            Check check1 = new Check(args[0]);
            Check check2 = new Check(args[1]);

            long checksum1 = check1.Checksum();
            long checksum2 = check2.Checksum();
            System.out.println("Checksum1: "+checksum1+"\nChecksum2: "+checksum2+"\n");

            if(checksum1 == checksum2){
                System.out.println("It's a match!");
            }
            else{
                System.out.println("No luck here, buddy.");
            }
        }

    }

}

