import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 *
 * @author Yogesh Jagadeesan
 */

public class Communication {
    
    private static Transmitter transmitter;
    private static DatagramSocket socket;  
    private static ArrayList<String> addressesInCall;
    
    private static void startTransmitterAndReceiver(String transmitAddress, String recvAddress, Integer port){
        try{
                transmitter = new Transmitter(transmitAddress, port);
                Receiver receiver = new Receiver();
                transmitter.startTransmitter();
                receiver.initiate(recvAddress, port);   
                addressesInCall.add(recvAddress);
                addressesInCall.add(transmitAddress);
        }
        catch(Exception e){             
        }
    }
        
    
    private static class ListenForCall implements Runnable{

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
            while(true){
                try {
                        socket.receive(incomingPacket);
                        String message = new String(incomingPacket.getData(), incomingPacket.getOffset(), incomingPacket.getLength());
                        String user = "";
                        String[] parts = message.split(" ");
                        if(parts.length > 1){
                            user = parts[1];
                        }
                        if(message.contains("RequestingCall")){
                            System.out.println(user+" is making a call...Answered");                           
                            startTransmitterAndReceiver(incomingPacket.getAddress().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 2224);
                        }
                        else if(message.contains("RequestingConference")){
                            System.out.println(user+" has joined the conference");
                            transmitter.addTarget(incomingPacket.getAddress().getHostAddress().toString(), 2224);
                            addressesInCall.add(incomingPacket.getAddress().getHostAddress().toString());
                            String listOfAddresses = "";
                            
                            //Build list of addresses
                            for(int idx=0; idx<addressesInCall.size(); ++idx){
                                if(idx==0){
                                    listOfAddresses += addressesInCall.get(idx);
                                }
                                else{
                                    listOfAddresses += ":"+addressesInCall.get(idx);
                                }
                            }
                            
                            //Send list to all participants
                            Thread.sleep(5000);
                            for(int idx=0; idx<addressesInCall.size(); ++idx){
                                if(!addressesInCall.get(idx).equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress())){
                                    socket.send(new DatagramPacket(listOfAddresses.getBytes(), listOfAddresses.getBytes().length, InetAddress.getByName(addressesInCall.get(idx)), 23880));
                                }
                            }
                        }
                        else{
                            String[] addresses = message.split(":");
                            for(int idx=0; idx<addresses.length; ++idx){
                                if(!addressesInCall.contains(addresses[idx])){
                                    transmitter.addTarget(addresses[idx], 2224);
                                    addressesInCall.add(addresses[idx]);
                                }
                            }
                        }
                    } catch (Exception ex) {
                }
            }
        }
    }
    
    public static void main(String[] args) throws Exception{
        int choice;
        String ip, message;
        DatagramPacket dp;
        
        socket = new DatagramSocket(23880,InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
        addressesInCall = new ArrayList<String>();
        
        Thread thr = new Thread(new ListenForCall());
        thr.start();
        
        System.out.println("1. Make a call\n2. Join a call");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        choice = Integer.parseInt(br.readLine());       
        
        switch(choice){
            case 1:
                System.out.print("Enter IP:");
                ip = br.readLine();
                message = "RequestingCall"+" "+System.getenv("username");
                dp = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(ip), 23880);
                socket.send(dp);
                startTransmitterAndReceiver(ip, InetAddress.getLocalHost().getHostAddress(), 2224);
                break;
            case 2:
                System.out.print("Enter IP:");
                ip = br.readLine();
                message = "RequestingConference"+" "+System.getenv("username");
                dp = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(ip), 23880);
                socket.send(dp);
                startTransmitterAndReceiver(ip, InetAddress.getLocalHost().getHostAddress(), 2224);
                break;
            default:
                break;
        }
    }
}
