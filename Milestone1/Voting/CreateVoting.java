import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CreateVoting
{

    // socket for connection to SISServer
    static Socket universal;
    private static int port = 53217;
    // message writer
    static MsgEncoder encoder;
    // message reader
    static MsgDecoder decoder;
    // scope of this component
    private static final String SCOPE = "SIS.Scope1";
	// name of this component
    private static final String NAME = "Voting";
    // messages types that can be handled by this component
    private static final List<String> TYPES = new ArrayList<String>(
        Arrays.asList(new String[] { "Setting", "Confirm" }));
    // shared by all kinds of records that can be generated by this component
    private static KeyValueList record = new KeyValueList();
    // shared by all kinds of alerts that can be generated by this component
    private static KeyValueList alert = new KeyValueList();

    /*
     * Main program
     */
    public static void main(String[] args)
    {
        while (true)
        {
            try
            {

                // try to establish a connection to SISServer
                universal = connect();

                // bind the message reader to inputstream of the socket
                decoder = new MsgDecoder(universal.getInputStream());
                // bind the message writer to outputstream of the socket
                encoder = new MsgEncoder(universal.getOutputStream());

                /*
                 * construct a Connect message to establish the connection
                 */
                KeyValueList conn = new KeyValueList();
                conn.putPair("Scope", SCOPE);
                conn.putPair("MessageType", "Connect");
				conn.putPair("Role", "Basic");
                conn.putPair("Name", NAME);
                encoder.sendMsg(conn);

                // KeyValueList for inward messages, see KeyValueList for
                // details
                KeyValueList kvList;

                while (true)
                {
                    // attempt to read and decode a message, see MsgDecoder for
                    // details
                    kvList = decoder.getMsg();

                    // process that message
                    ProcessMsg(kvList);
                }

            }
            catch (Exception e)
            {
                // if anything goes wrong, try to re-establish the connection
                e.printStackTrace();
                try
                {
                    // wait for 1 second to retry
                    Thread.sleep(1000);
                }
                catch (InterruptedException e2)
                {
                }
                System.out.println("Try to reconnect");
                try
                {
                    universal = connect();
                }
                catch (IOException e1)
                {
                }
            }
        }
    }

    /*
     * used for connect(reconnect) to SISServer
     */
    static Socket connect() throws IOException
    {
        Socket socket = new Socket("127.0.0.1", port);
        return socket;
    }
    private static void ProcessMsg(KeyValueList kvList) throws Exception
    {
		KeyValueList message = new KeyValueList();
        String scope = kvList.getValue("Scope");
        if (!SCOPE.startsWith(scope))
        {
            return;
        }

        String messageType = kvList.getValue("MessageType");
        if (!TYPES.contains(messageType))
        {
            return;
        }

        String sender = kvList.getValue("Sender");

        String receiver = kvList.getValue("Receiver");

        String purpose = kvList.getValue("Purpose");

        switch (messageType)
        {
        case "Alert":
        	boolean state = test(kvList.getValue("first"),kvList.getValue("second"),kvList.getValue("last"));
    		message.putPair("MessageType","Confirm");
    		message.putPair("Scope","SISServer.Scope1");
    		message.putPair("Description","Test Result");
    		message.putPair("Sender","Voting");
    		message.putPair("Receiver","SISServer");
    		if (state)
    			message.putPair("Message Received", "success");
    		else
    			message.putPair("Message Received", "failure");
    		break;
        	
        case "Confirm":
            System.out.println("Connect to SISServer successful.");
            break;
        case "Setting":
            if (receiver.equals(NAME))
            {
                System.out.println("Message from " + sender);
                System.out.println("Message type: " + messageType);
                System.out.println("Message Purpose: " + purpose);
                switch (purpose)
                {

                case "Activate":
                	message.putPair("MessageType","26");
                	message.putPair("Description","Acknowledgment (Server acknowledges that voting is now connected to server)");
                	message.putPair("AckMsgId","23");
                	message.putPair("YesNo","Yes");
                	message.putPair("Name","Voting");
                    
                    System.out.println("Voting Software Activated");
                    break;

                case "Kill":
                    message.putPair("MessageType","26");
                	message.putPair("Description","Acknowledgment (Server acknowledges request to kill component)");
                	message.putPair("AckMsgId","22");
                	message.putPair("YesNo","Yes");
                	message.putPair("Name","Voting");
                    System.exit(1);
                    break;

                case "Deactivate":
                    message.putPair("MessageType","26");
                	message.putPair("Description","Acknowledgment (Server acknowledges that voting is now deactivated");
                	message.putPair("AckMsgId","25");
                	message.putPair("YesNo","Yes");
                	message.putPair("Name","Voting");
                    System.out.println("VotingSoftware Deactivated");
                    System.exit(0);
                    break;
                }
            }
            break;
            default:
            	message.putPair("MessageType","ERROR");
            	message.putPair("Description","Unable to process message");
            	System.out.println("There was an error with your message");
            	break;
        }
        encoder.sendMsg(message);
    }
    
    //Testing method
    private static boolean test(String first, String second, String last){
    	String result = new String(first + " " + second + last);
    	System.out.println(result);
    	if (result.equals("Hello World!")) return true;
    	else return false;
    }
}