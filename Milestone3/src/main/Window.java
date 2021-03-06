/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author adamhayes
 */

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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
public class Window {
    public boolean begun;
    public boolean connected;
    static Socket universal;
        // scope of this component
    private static final String SCOPE = "SIS.Scope1";
	// name of this component
    private static final String NAME = "GUI";
    
    public static int port = 53217;
    // messages types that can be handled by this component
    private static final List<String> TYPES = new ArrayList<String>(
        Arrays.asList(new String[] { "Alert", "Emergency", "Confirm", "Setting" }));

  //  private static UploaderReading reading = new UploaderReading();
    // message writer
    static MsgEncoder encoder;
    // message reader
    static MsgDecoder decoder;
    //GUI Components
    private JFrame frame;
    private JPanel panel;
    //Panels for action buttons
    private JPanel activate;
    private JPanel deactivate;
    private JPanel showTally;
    private JPanel admin;
    private JPanel kill;
    private JPanel quit;
    // buttons for the panels
    private JButton actBut;
    private JButton deactBut;
    private JButton showBut;
    private JButton killBut;
    private JButton adminBut;
    public JTextField field;
    private String password;
    
    public Window() {
        begun = false;
        connected = false;
        GridLayout layout = new GridLayout(3,2,100, 50);
        activeListen actList = new activeListen();
        deactiveListen deactList = new deactiveListen();
        showListen showList = new showListen();
        killListen killList = new killListen();
        passwordListen adminList = new passwordListen();
        
        panel = new JPanel();
        panel.setLayout(layout);        
        activate = new JPanel();
        activate.setLayout(new BoxLayout(activate,BoxLayout.PAGE_AXIS));
        deactivate = new JPanel();
        deactivate.setLayout(new BoxLayout(deactivate,BoxLayout.PAGE_AXIS));
        showTally = new JPanel();
        showTally.setLayout(new BoxLayout(showTally,BoxLayout.PAGE_AXIS));
        admin = new JPanel();
        admin.setLayout(new BoxLayout(admin,BoxLayout.PAGE_AXIS));
        kill = new JPanel();
        kill.setLayout(new BoxLayout(kill,BoxLayout.PAGE_AXIS));
        
        actBut = new JButton("Start Accepting Votes");
        deactBut = new JButton("Stop Accepting Votes");
        showBut = new JButton("Display Results");
        killBut = new JButton("Kill the program");
        adminBut = new JButton("Enter Admin Password");
        
        actBut.addActionListener(actList);
        deactBut.addActionListener(deactList);
        showBut.addActionListener(showList);
        killBut.addActionListener(killList);
        adminBut.addActionListener(adminList);
        
        actBut.setHorizontalAlignment(SwingConstants.CENTER);
        deactBut.setHorizontalAlignment(SwingConstants.CENTER);
        showBut.setHorizontalAlignment(SwingConstants.CENTER);
        killBut.setHorizontalAlignment(SwingConstants.CENTER);
        adminBut.setHorizontalAlignment(SwingConstants.CENTER);
        
        activate.add(actBut);
        deactivate.add(deactBut);
        showTally.add(showBut);
        kill.add(killBut);
        admin.add(adminBut);
        
        panel.add(activate);
        panel.add(deactivate);
        panel.add(showTally);
        panel.add(kill);
        panel.add(admin);
        
        frame = new JFrame("1631 Voting Software");
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        newPassword();
        sendConnect();
        
        
    }
    
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
				while(!registerComponent());
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
    
    static boolean registerComponent(){
		System.out.println("attempting to register");
		try{
        	KeyValueList reg = new KeyValueList();
        	reg.putPair("MessageType","Register");
        	reg.putPair("MsgId","21");
        	reg.putPair("Scope",SCOPE);
        	reg.putPair("Role","Basic");
        	reg.putPair("Description","Create GUI Component");
        	reg.putPair("SecurityLevel","3");
        	reg.putPair("Component Description","GUI allows admin to create voting table and activate and deactivate voting");
			reg.putPair("KnowledgeBase","TallyTable,CreateVoting");
			reg.putPair("InputMsgID 1", "26");
			reg.putPair("OutputMsgID 1", "703");
			reg.putPair("OutputMsgID 2", "24");
			reg.putPair("OutputMsgID 3", "25");
			reg.putPair("OutputMsgID 4", "22");
			reg.putPair("OutputMsgID 5", "702");
			encoder.sendMsg(reg);
			return true;
		} catch(Exception e){
			return false;
		}
    }   
    static Socket connect() throws IOException
    {
        Socket socket = new Socket("127.0.0.1", port);
        return socket;
    }
    public void sendConnect(){
        
        while(!connected){
            try{
            

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
                connected = true;
                decoder.getMsg();
                
                // KeyValueList for inward messages, see KeyValueList for
                // details
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
                connected = false;
            }
        }
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
        
        String ackMsgId = kvList.getValue("AckMsgId");
        
        String msgId = kvList.getValue("MsgId");
        
        String yesNo;
        
        String passwordAccept;

        switch (messageType)
        {
        case "Setting":
			switch(ackMsgId){
				//Tried to create tally table
				case "703":
					yesNo = kvList.getValue("YesNo");
					passwordAccept = kvList.getValue("Password");
					if (yesNo.equals("Yes"))
						JOptionPane.showInputDialog("Tally Table created");
					else if (passwordAccept.equals("Yes")){
						JOptionPane.showInputDialog("Password incorrect");
					}
					else{
						JOptionPane.showInputDialog("Tally Table already exists");
					}
					break;
				//Tried to activate voting
				case "24":
					yesNo = kvList.getValue("YesNo");
					if (yesNo.equals("Yes")){
						JOptionPane.showInputDialog("Voting software activated");
					}
					else{
						JOptionPane.showInputDialog("Password incorrect");
					}
					break;
				//Tried to deactivate voting
				case "25":
					yesNo = kvList.getValue("YesNo");
					if (yesNo.equals("Yes")){
						JOptionPane.showInputDialog("Voting software deactivated");
					}
					else{
						JOptionPane.showInputDialog("Password incorrect");
					}
					break;
			}
            break;
       //Tried to kill voting
        case "Alert":
        	yesNo = kvList.getValue("YesNo");
			if (yesNo.equals("Yes")){
				JOptionPane.showInputDialog("Voting software killed");
			}
			else{
				JOptionPane.showInputDialog("Password incorrect");
			}
			break;
        case "Confirm":
			break;
        case "Connect":
        	break;
        case "Emergency":
        	break;
        //Voting results
        case "Reading":
        	String results = kvList.getValue("RankedReport");
        	yesNo = kvList.getValue("YesNo");
			if (yesNo.equals("Yes")){
				JOptionPane.showInputDialog(results);
			}
			else{
				JOptionPane.showInputDialog("Password incorrect");
			}
        	break;
        
       
        default:
			JOptionPane.showInputDialog("There was an error reading the message.");
			break;
        }
    }
    
     private static void genMessage(KeyValueList message){
		message.putPair("Sender",NAME);
        message.putPair("Scope",SCOPE);
        message.putPair("Receiver", "VotingSoftware");            
    }
    class activeListen implements ActionListener{
        //send the activate and init messages
        public void actionPerformed(ActionEvent e) {
            try{
            KeyValueList actMes = new KeyValueList();
            sendConnect();
            
            String candTemp = JOptionPane.showInputDialog("Enter List of "
                        + "Candidate IDs separated by commas");
            actMes.putPair("CandidateList",candTemp);
            
   
           
           genMessage(actMes);
           actMes.putPair("MessageType","Setting");
           actMes.putPair("Passcode",password);
           actMes.putPair("MsgId","24");
           encoder.sendMsg(actMes);
        
           KeyValueList resp = decoder.getMsg();
           System.out.println(resp.toString());
           System.out.println(resp.getValue("Scope"));
           if(resp.getValue("YesNo").equals("No")){
         
               JOptionPane.showMessageDialog(frame,"Bad Admin Password: Could not activate");
               newPassword();
               return;
               
           } else {
         
               actBut.setEnabled(false);
           }
           actMes.removePair("MsgId");
           actMes.putPair("MsgId","703");
        
           encoder.sendMsg(actMes);
         
           activated();
            JOptionPane.showMessageDialog(frame,"Voting Has Begun");
           
            } catch (Exception d){
                JOptionPane.showMessageDialog(frame,"Error "
                        + "communicating with Voting Software","Communication Error",
                        JOptionPane.ERROR_MESSAGE);
                d.printStackTrace();
                
            }
               
            
        }
    }
    public void newPassword(){
        actBut.setEnabled(false);
        killBut.setEnabled(false);
        showBut.setEnabled(false);
        deactBut.setEnabled(false);
        adminBut.setEnabled(true);
    }
    public void activated(){
        showBut.setEnabled(true);
		deactBut.setEnabled(true);
        actBut.setEnabled(false);
    }
	public void deactivated(){
		deactBut.setEnabled(false);
	}
    class passwordListen implements ActionListener{
        public void actionPerformed(ActionEvent e){
            password = JOptionPane.showInputDialog("Enter Admin Password");
            actBut.setEnabled(true);
            killBut.setEnabled(true);
            deactBut.setEnabled(false);
            adminBut.setEnabled(false);
        }
    }
    class deactiveListen implements ActionListener{
        //stop accepting new votes
        public void actionPerformed(ActionEvent e){
            try{
            sendConnect();
            KeyValueList deactMes = new KeyValueList();
            genMessage(deactMes);
            deactMes.putPair("MessageType","Setting");
            deactMes.putPair("MsgId", "25");
            deactMes.putPair("Passcode",password);
            encoder.sendMsg(deactMes);
            KeyValueList resp = decoder.getMsg();
            if(resp.getValue("YesNo").equals("No")){
                JOptionPane.showMessageDialog(frame,"Bad Admin Password: Unable to deactivate");
                newPassword();
                return;
            }
			deactivated();
			JOptionPane.showMessageDialog(frame,"Voting Complete");
            } catch (Exception d){
                JOptionPane.showMessageDialog(frame,"Error "
                        + "communicating with Voting Software","Communication Error",
                        JOptionPane.ERROR_MESSAGE);
				d.printStackTrace();
            }
			
        }
    }
    class showListen implements ActionListener{
        //send the show winners message
        public void actionPerformed(ActionEvent e){
            try{
            sendConnect();
            KeyValueList showMes = new KeyValueList();
            genMessage(showMes);
            showMes.putPair("MessageType","Setting");
            showMes.putPair("MsgId","702");
            showMes.putPair("Passcode",password);
            showMes.putPair("N","1");
            encoder.sendMsg(showMes);
			
            KeyValueList resp = decoder.getMsg();
            if(resp.getValue("YesNo").equals("No")){
                JOptionPane.showMessageDialog(frame, resp.getValue("Category") + ": Could not display rankings");
                if(resp.getValue("Category").equals("Bad Admin Password")){
                    newPassword();
                }
                return;
            } else {
				
				String result = resp.getValue("RankedReport");
				String display_string = "";
				String[] parts = result.split(";");
				for(int i = 0; i < parts.length; i++){
					display_string = display_string.concat(parts[i]);
					if(i < parts.length - 1){
						display_string = display_string.concat(" | ");
					}
				}
				display_string = display_string.replace(",",": ");
                JOptionPane.showMessageDialog(frame, display_string);
            }
            } catch (Exception d){
                JOptionPane.showMessageDialog(frame,"Error "
                        + "communicating with Voting Software. Please retry.","Communication Error",
                        JOptionPane.ERROR_MESSAGE);
				 d.printStackTrace();
            }
        }
    }
    class killListen implements ActionListener{
        public void actionPerformed(ActionEvent e){
            try{
            sendConnect();
            KeyValueList killMes = new KeyValueList();
            genMessage(killMes);
            killMes.putPair("MessageType","Emergency");
            killMes.putPair("MsgId","22");
            killMes.putPair("Passcode",password);
            encoder.sendMsg(killMes);
            KeyValueList resp = decoder.getMsg();
            if(resp.getValue("YesNo").equals("No")){
                JOptionPane.showMessageDialog(frame,"Bad Admin Password: Couldn't Kill");
                newPassword();
            } else {
                JOptionPane.showMessageDialog(frame,"Ending HayesHannaVoting Software");
                
                System.exit(0);
            }
            
            } catch (Exception d){
                JOptionPane.showMessageDialog(frame,"Error "
                        + "communicating with Voting Software","Communication Error",
                        JOptionPane.ERROR_MESSAGE);
				d.printStackTrace();
            }
        }
    }
}
