import java.io.*;
import java.net.InetAddress;
import javax.media.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import java.util.Vector;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;

/**
 *
 * @author Yogesh Jagadeesan
 */
public class Transmitter {

    private Processor processor;
    private static DataSource outputDataSource = null;
    private Integer stateLock = new Integer(0);
    private boolean failed = false;
    private RTPManager rtpManager;
    
    public Transmitter(String partipantIP, int participantPort) throws Exception{
        Vector<CaptureDeviceInfo> deviceList = CaptureDeviceManager.getDeviceList(null);
        CaptureDeviceInfo actualDevice = null;
        for(int idx=0; idx<deviceList.size(); ++idx){
            if(deviceList.get(idx).getName().equalsIgnoreCase("JavaSound audio capture")){
                actualDevice = deviceList.get(idx);
            }
        }
        if(actualDevice != null){
            outputDataSource = createProcessorAndDataSource(actualDevice);
            rtpManager = createRTPManager(partipantIP, participantPort);
        }
    }
    
    public void addTarget(String ipAddress, int port) throws Exception{
        rtpManager.addTarget(new SessionAddress(InetAddress.getByName(ipAddress), port));
    }
    
    private RTPManager createRTPManager(String partipantIP, int participantPort) throws Exception{
        RTPManager rtpManager = RTPManager.newInstance();
        SessionAddress localAddress = new SessionAddress(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), 2226);
        rtpManager.initialize(localAddress);
        rtpManager.addTarget(new SessionAddress(InetAddress.getByName(partipantIP),participantPort));
        SendStream sendStream = rtpManager.createSendStream(outputDataSource, 0);
        sendStream.start();
        return rtpManager;
    }
    
    private DataSource createProcessorAndDataSource(CaptureDeviceInfo info) throws Exception{
        MediaLocator locator = info.getLocator();
        DataSource dataSource = javax.media.Manager.createDataSource(locator);
        processor = javax.media.Manager.createProcessor(dataSource);
        waitForProcessorState(processor, Processor.Configured);
        processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW_RTP));
        waitForProcessorState(processor, Controller.Realized);
        DataSource output = processor.getDataOutput();
        return output;
    }
    
    Integer getStateLock() {
	return stateLock;
    }

    void setFailed() {
	failed = true;
    }
    
    private synchronized boolean waitForProcessorState(Processor processor, int state) {
	processor.addControllerListener(new StateListener());
	failed = false;

	if (state == Processor.Configured) {
	    processor.configure();
	} else if (state == Processor.Realized) {
	    processor.realize();
	}
	
	while (processor.getState() < state && !failed) {
	    synchronized (getStateLock()) {
		try {
		    getStateLock().wait();
		} catch (Exception exception) {
		    return false;
		}
	    }
	}

	if (failed)
	    return false;
	else
	    return true;
    }
    
    private class StateListener implements ControllerListener {

        @Override
	public void controllerUpdate(ControllerEvent controllerEvent) {

	    if (controllerEvent instanceof ControllerClosedEvent)
		setFailed();
            
	    if (controllerEvent instanceof ControllerEvent) {
		synchronized (getStateLock()) {
		    getStateLock().notifyAll();
		}
	    }
	}
    }
    
    public void startTransmitter() throws IOException{
        processor.start();
    }
    
    public void stopTransmitter() throws IOException{
        synchronized(this){
            processor.stop();
            processor.close();
            rtpManager.removeTargets("Session ended");
            rtpManager.dispose();
        }
    }
}
