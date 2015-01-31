import java.net.InetAddress;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.media.RealizeCompleteEvent;
import javax.media.control.BufferControl;
import javax.media.protocol.DataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.SessionEvent;

/**
 *
 * @author Yogesh Jagadeesan
 */
public class Receiver implements ReceiveStreamListener, SessionListener, ControllerListener{
    
    private RTPManager rtpManager=null;
    private Object syncLock = new Object();
   
    public void initiate(String destinationIP, int port) throws Exception{
        SessionAddress sessionAddress = new SessionAddress(InetAddress.getByName(destinationIP), port);
        
        //Add listeners
        rtpManager = (RTPManager) RTPManager.newInstance();
        rtpManager.addSessionListener(this);
        rtpManager.addReceiveStreamListener(this);
        rtpManager.initialize(sessionAddress);
        
        //Set buffer size
        BufferControl bufferControl = (BufferControl)rtpManager.getControl("javax.media.control.BufferControl");
        bufferControl.setBufferLength(350);
        rtpManager.addTarget(sessionAddress);
        
        //wait for data to arrive
        synchronized(syncLock){
            syncLock.wait();
        }
    }
    
    @Override
    public void update(ReceiveStreamEvent rse){
        RTPManager manager = (RTPManager)rse.getSource();
        Participant participant = rse.getParticipant();
        String currentUser = System.getenv("username")+"@"+System.getenv("computername");
        Player player=null;
        
        if((rse instanceof NewReceiveStreamEvent)){
            ReceiveStream receiveStream = ((NewReceiveStreamEvent)rse).getReceiveStream();
            DataSource dataSource = receiveStream.getDataSource();
            try {
                    //create and store player.
                    player = javax.media.Manager.createPlayer(dataSource);
                    player.addControllerListener(this);
                    player.realize();
                
                    synchronized(syncLock){
                        syncLock.notifyAll();
                    }
                } catch (Exception ex) {
            }
        }
        else if (rse instanceof ByeEvent) {
            if(player!=null){
                player.close();
            }           
	}
    }

    @Override
    public void update(SessionEvent evt) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void controllerUpdate(ControllerEvent controllerEvent) {
        Player player = (Player) controllerEvent.getSource();
        
        //when realized, start player
        if(controllerEvent instanceof RealizeCompleteEvent){
            player.start();
        }
    }
}
